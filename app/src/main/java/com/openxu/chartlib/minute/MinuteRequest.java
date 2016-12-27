package com.openxu.chartlib.minute;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.minute.bean.MinuteParame;
import com.openxu.chartlib.minute.bean.MinuteResult;
import com.openxu.chartlib.minute.bean.MinutesBean;
import com.openxu.chartlib.minute.bean.PankouData;
import com.openxu.chartlib.testdata.TestData;
import com.openxu.chartlib.utils.CommonUtil;
import com.openxu.chartlib.utils.DateUtil;
import com.openxu.chartlib.utils.GlFontUtil;
import com.openxu.chartlib.utils.JSONUtil;
import com.openxu.chartlib.utils.LogUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Request;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : MinuteRequest
 * version : 1.0
 * class describe：分时和盘口请求处理类
 */
public class MinuteRequest {

    private static final String TAG="MinuteRequest";

    private String symbol;
    private float preprice;
    private float permaxmin;      //最高价和最低价的差值
    private MinuteManager minuteManager;
    private MinuteParame parame;
    private boolean isZhishu=false;

    private List<MinutesBean> datas;
    private MinuteDeserializer minuteDeserializer;

    //单个线程的线程池
    private ScheduledExecutorService singleThreadPool;


    private volatile boolean isDestory=false;

    private boolean isRequesting=false;

    public MinuteRequest(MinuteManager minuteManager, String symbol, boolean isZhishu, float preprice){
        this.symbol = symbol;
        this.minuteManager = minuteManager;
        this.preprice = preprice;
        this.isZhishu=isZhishu;
        this.parame= new MinuteParame();
    }

    /**
     * 每隔30s请求一次数据
     */
    public void request(){
        if(singleThreadPool!=null&&!singleThreadPool.isShutdown())
            return;
        isDestory=false;
        if(singleThreadPool==null)singleThreadPool=Executors.newSingleThreadScheduledExecutor();
        singleThreadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "每30s更新分时图");
                getMinuteData(symbol);
                if(!isZhishu)
                    getPankoudata(symbol);
            }
        },0,30*1000, TimeUnit.MILLISECONDS);
    }
    public void destoryRequest(){
        isDestory=true;
        if(singleThreadPool==null)
            return;
        singleThreadPool.shutdownNow();
        singleThreadPool=null;
    }

    /**
     * 获取分时数据
     * @param symbol
     */
    private void getMinuteData(String symbol) {

        HashMap<String, String> p = new HashMap<String,String>();
        p.put("symbol", symbol);
        Log.v(TAG, "获取分时数据："+Constants.MinuteRequetUrl+"?symbol="+symbol);
        OkHttpUtils.post()
                .url(Constants.MinuteRequetUrl)
                .tag(TAG)
                //TODO 参数
//                .params(p)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onBefore(Request request) {
                        isRequesting=true;
                    }
                    @Override
                    public void onError(Call call, Exception e) {
                        if(isDestory)return;
                        minuteManager.setData(datas,parame,false);
                    }
                    @Override
                    public void onResponse(String response) {
                        //TODO 如果使用真实接口，请注释下面一段代码
                        response = TestData.MinuteData; //使用测试数据
                        Log.i(TAG, "返回分时数据："+response);
                        //{"code":1,"msg":"success","data":[[1482715800000,20.13,408500],...,[1482729540000,20.55,0]]}
                        boolean isStop = false;
                        if (response != null) {
                            parame.pricedivider =-1;
                            MinuteResult result=null;
                            try {
                                //解析分时数据
                                result = (MinuteResult) parseData(response);
                            }catch (Exception e){
                            }
                            if (result != null&&result.getData()!=null && result.getData().size()>0) {
                                datas = result.getData();
                                //根据分时数据计算图表绘制的各项参数（最高价、最低价、振幅、最大成交量等）
                                computParmas();
                                isStop=false;
                            }else{
                                isStop = true;
                            }
                            if(isDestory)return;
                            minuteManager.setData(datas,parame,isStop);
                        }else{
                            if(isDestory)return;
                            minuteManager.setData(datas,parame,isStop);
                        }
                    }

                    @Override
                    public void onAfter() {
                        isRequesting=false;
                    }
                });

    }


    /** 分时数据解析*/
    private Object parseData(String data) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        minuteDeserializer = new MinuteDeserializer();
        gsonBuilder.registerTypeAdapter(MinuteResult.class, minuteDeserializer);
        Gson gson = gsonBuilder.create();
        MinuteResult result = gson.fromJson(data,MinuteResult.class);
        return result;
    }

    /**分时数据具体解析方法*/
    public class MinuteDeserializer implements JsonDeserializer<MinuteResult> {
        private float totalP=0;
        private float totalV=0;

        @Override
        public MinuteResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            MinuteResult result = new MinuteResult();
            List<MinutesBean> list=new ArrayList<>();
            if(json!=null){
                try {
                    JsonObject jsonObject = json.getAsJsonObject();
                    JsonArray array = jsonObject.getAsJsonArray("data");
                    if (array != null && array.size() > 0) {
                        for (int i = 0; i < array.size(); i++) {
                            //[1478050200000,"25.99",0]
                            JsonArray itemarry = array.get(i).getAsJsonArray();
                            MinutesBean mb = new MinutesBean();
                            //时间
                            long time = itemarry.get(0).getAsLong();
                            mb.time = DateUtil.unixTimestampToDate(time,DateUtil.DATE_YMRHM);

                            //获取小时分钟"yyyy-MM-dd HH:mm";
                            String hm[] = mb.time.substring(11).split(":");
                            //毫秒数
                            mb.timesecond = Integer.parseInt(hm[0])*60*60*1000+
                                    Integer.parseInt(hm[1])*60*1000;
                            //每天9点30开盘
                            if(mb.timesecond < Constants.startTime)
                                continue;

                            if(parame.pricedivider==-1) {
                                if (hm[0].equals("13") && hm[1].equals("00")) {
                                    parame.pricedivider = list.size();
                                }
                            }
                            //成交价
                            mb.cjprice = itemarry.get(1).getAsFloat();
                            //成交量
                            long vol=itemarry.get(2).getAsLong();
                            mb.cjnum = Math.round(vol*1.0f/100);
                            //当前成交总额
                            totalP += vol*mb.cjprice;
                            //当前成交总数
                            totalV += vol;
                            //计算平均价
                            if(totalP==0){
                                mb.avprice = mb.cjprice;
                            }else {
                                float avg = totalP / totalV;
                                mb.avprice = avg;
                            }

                            double cha = mb.cjprice - preprice;
                            if (Math.abs(cha) > permaxmin) {
                                permaxmin = (float) Math.abs(cha);
                            }
                            parame.maxVolume = Math.max(parame.maxVolume,mb.cjnum);
                            list.add(mb);
                        }
                    }
                    if( parame.pricedivider==-1){
                        parame.pricedivider =121;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            result.setData(list);
            return result;
        }
    }

    /**
     * 计算最大，最小及成交量曲线的最大值及单位的参数。
     */
    private void computParmas(){
        LogUtil.v(TAG, "preprice="+preprice+"  permaxmin="+permaxmin);
        parame.maxPrice = preprice+permaxmin;
        parame.minPrice = preprice-permaxmin;
        parame.volumeper = permaxmin / preprice;
        LogUtil.v(TAG, "最高价："+parame.maxPrice);
        LogUtil.v(TAG, "最低价："+parame.minPrice);
        LogUtil.v(TAG, "振幅："+parame.volumeper);
        String pricestr = Constants.twoPointFormat.format(parame.maxPrice);
        parame.maxpriceLenth = GlFontUtil.getFontlength(Constants.labelPaint,pricestr);
        String maxVolumestr = parame.maxVolume+"";
        parame.leftVolume = CommonUtil.getDisplayVolume(maxVolumestr.length(),parame.maxVolume);
    }

    /**
     * 请求盘口数据
     * @param symbol
     */
    private void getPankoudata(String symbol){
        HashMap<String, String> p = new HashMap<>();
        p.put("symbol", symbol);
        Log.v(TAG, "获取盘口数据："+Constants.PankouRequestUrl+"?symbol="+symbol);
        OkHttpUtils.post()
                .url(Constants.PankouRequestUrl)
                .tag(TAG)
                //TODO 参数
//                .params(p)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        if(isDestory)return;
                        minuteManager.setPankouData(null);
                    }
                    @Override
                    public void onResponse(String response) {
                        //TODO 如果使用真实接口，请注释下面一段代码
                        //使用测试数据
                        response = TestData.PankouData;
                        Log.i(TAG, "返回盘口数据："+response);

                        if (TextUtils.isEmpty(response)) {
                            PankouData pd = JSONUtil.jsonToBean(response,PankouData.class);
                            if(pd!=null && pd.getData() !=null){
                                PankouData.Data pankouData = pd.getData();
                                if(isDestory)return;
                                minuteManager.setPankouData(pankouData);
                            }else{
                                //盘口数据为空
                                if(isDestory)return;
                                minuteManager.setPankouData(null);
                            }
                        }else{
                            //服务没有返回数据
                            if(isDestory)return;
                            minuteManager.setPankouData(null);
                        }
                    }

                });
    }


}
