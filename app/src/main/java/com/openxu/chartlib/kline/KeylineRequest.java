package com.openxu.chartlib.kline;


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
import com.openxu.chartlib.kline.entity.KLineParame;
import com.openxu.chartlib.kline.entity.KLineTechParam;
import com.openxu.chartlib.kline.entity.KLineType;
import com.openxu.chartlib.kline.entity.KeyLineItem;
import com.openxu.chartlib.kline.entity.KlineResult;
import com.openxu.chartlib.testdata.TestData;
import com.openxu.chartlib.utils.CommonUtil;
import com.openxu.chartlib.utils.DateUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Request;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KeyLineManager
 * version : 1.0
 * class describe：K线请求处理类
 */
public class KeylineRequest {

    private String TAG = "KeylineRequest";

    private KeyLineManager keyLineManager;

    private ArrayList<KeyLineItem> datas;         //K线图数据集合
    private KLineParame parame = new KLineParame();
    private KLineTechParam kLineTechParam = new KLineTechParam();
    private boolean isDestoryed = false;
    private boolean isRequesting = false;
    private KLineType keylineType;                //技术指标枚举
    private KeylineData keylineData;

    public KeylineRequest(KeyLineManager keyLineManager) {
        this.keyLineManager = keyLineManager;
    }

    /**
     * 获取K线数据
     * @param keylineType 技术指标
     * @param symbol
     * @param period  K线图类型
     * @param fq  分权
     */
    public void getKlineData(KLineType keylineType, String symbol, String period, String fq) {
        isDestoryed = false;
        this.keylineType = keylineType;
        if(datas!=null){
            datas.clear();
            datas=null;
        }
        this.keylineData=null;
        HashMap<String, String> p = new HashMap<String, String>();
        p.put("symbol", symbol);    //股票代码
        p.put("period", period);    //日k、周k、月k
        p.put("division", fq);      //复权
        Log.v(TAG, "获取Kline数据："+Constants.KeylineRequestUrl+"?symbol="+symbol+"&period="+period+"&symbol="+symbol);

        OkHttpUtils.post()
                .url(Constants.KeylineRequestUrl)
                .tag(TAG)
                .params(p)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onBefore(Request request) {
                        isRequesting = true;
                    }
                    @Override
                    public void onError(Call call, Exception e) {
                        if (isDestoryed) return;
                        keyLineManager.setkLineTechParam(kLineTechParam);
                        keyLineManager.setData(datas, parame, false);
                    }
                    @Override
                    public void onResponse(String response) {
                        boolean isStop;
                        KlineResult result = null;
                        try {
                            //TODO 如果使用真实接口，请注释下面一段代码
                            //使用测试数据
                            response = TestData.kLineData;
                            Log.i(TAG, "返回Kline数据："+response);

                            GsonBuilder gsonBuilder = new GsonBuilder();
                            gsonBuilder.registerTypeAdapter(KlineResult.class, new KlineDeserializer());
                            Gson gson = gsonBuilder.create();
                            Object obj = gson.fromJson(response, KlineResult.class);
                            if(obj!=null)
                            result = (KlineResult) obj;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (result != null && result.getData() != null && result.getData().size() > 0) {
                            datas = result.getData();
                            isStop = false;
                        } else {
                            isStop = true;
                        }
                        if (isDestoryed)
                            return;
                        keyLineManager.setkLineTechParam(kLineTechParam);
                        keyLineManager.setData(datas, parame, isStop);
                    }

                    @Override
                    public void onAfter() {
                        isRequesting = false;
                    }
                });
    }


    public class KlineDeserializer implements JsonDeserializer<KlineResult> {

        @Override
        public KlineResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            KlineResult result = new KlineResult();
            ArrayList<KeyLineItem> list = new ArrayList<KeyLineItem>();
            if (json != null) {
                try {
                    JsonObject jsonObject = json.getAsJsonObject();
                    JsonArray array = jsonObject.getAsJsonArray("data");
                    Log.i(TAG, "返回Kline数据array：" + array);
                    if (array != null && array.size() > 0) {
                        int len = array.size();
                        kLineTechParam.resetTecnology();

                        float close = 0;

                        for (int i = 0; i < len; i++) {
                            JsonArray itemarry = array.get(i).getAsJsonArray();
//                              时间date    开盘价   最高价  最低价  收盘价    成交量     转手率
//                          [1325606400000,"6.38","6.53","6.25","6.26","47432960","0.0049"]
                            KeyLineItem mb = new KeyLineItem();
                            //时间date
                            long time = itemarry.get(0).getAsLong();
                            mb.date = DateUtil.unixTimestampToDate(time, DateUtil.DATE_YMR);
                            //开盘价
                            mb.open = itemarry.get(1).getAsFloat();
                            //最高价
                            mb.high = itemarry.get(2).getAsFloat();
                            //最低价
                            mb.low = itemarry.get(3).getAsFloat();
                            //收盘价
                            mb.close = itemarry.get(4).getAsFloat();
                            long vol = itemarry.get(5).getAsLong();
                            mb.vol = Math.round(vol * 1.0f / 100);
                            if (close < Constants.EPSILON) {
                                //第一个数据的昨日收盘价等于今天的开盘价
                                mb.close_yestoday = mb.open;
                            } else {
                                mb.close_yestoday = close;
                            }
                            close = mb.close;
                            //开盘价大于收盘价，说明跌了
                            mb.isFall = mb.open > mb.close;

                            if (mb.close == mb.open && mb.high == mb.low) {
                                //前提条件最高最低，开盘收盘相等的情况下
                                //涨停：=(收盘-昨收)/昨收*100>9.92
                                //跌停: =(收盘-昨收)/昨收*100<-9.92;
                                try {
                                    //涨停（价格在一个交易日中的最大波动幅度为前一交易日收盘价上下百分之几）
                                    if ((mb.close - mb.close_yestoday) * 100 / mb.close_yestoday > 9.92) {
//                                        mb.close*=1.0001;
                                        mb.isFall = false;
                                        mb.isRaiseStop = true;
                                    }
                                    //跌停
                                    if ((mb.close - mb.close_yestoday) * 100 / mb.close_yestoday < -9.92) {
//                                        mb.close*=0.9991;
                                        mb.isFall = true;
                                        mb.isFallStop = true;
                                    }

                                } catch (Exception e) {
                                }
                            }
                            try {
                                //转手率
                                mb.turnover = itemarry.get(6).getAsFloat();
                            } catch (Exception e) {

                            }
                            try {
                                //振幅
                                mb.zhenfu = (mb.high - mb.low) / mb.close_yestoday;
                            } catch (Exception e) {
                            }
                            list.add(mb);
                            //5、10、20日平均收盘价
                            if (i >= 4) {
                                mb.avg5 = CommonUtil.getSum(list, i - 4, i) / 5;
                            }
                            if (i >= 9) {
                                mb.avg10 = CommonUtil.getSum(list, i - 9, i) / 10;
                            }
                            if (i > 19) {
                                mb.avg20 = CommonUtil.getSum(list, i - 19, i) / 20;
                            }
                        }
                        doTechData(list);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            result.setData(list);
            return result;
        }
    }

    /**
     * 得到对应的技术指标数据
     * @param list
     */
    public void doTechData(List<KeyLineItem> list) {
        if (list == null || list.size() == 0) return;
        if (keylineData == null) {
            keylineData = new KeylineData(list, kLineTechParam);
        }
        keylineData.doTechData(keylineType);
    }
    public void doTechData(KLineType keylineType) {
        this.keylineType = keylineType;
        doTechData(datas);
    }
    /**
     * 销毁请求，清空缓存
     */
    public void destroy() {
       cancelRequest();
    }
    /**
     * 销毁请求
     */
    public void cancelRequest(){
        isDestoryed = true;
        OkHttpUtils.getInstance().cancelTag(TAG);
    }

}
