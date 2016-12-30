package com.openxu.chartlib;

import android.app.Activity;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.openxu.chart.R;
import com.openxu.chartlib.bean.KeyLineItem;
import com.openxu.chartlib.bean.MinutesBean;
import com.openxu.chartlib.bean.PankouData;
import com.openxu.chartlib.bean.StockBaseInfo;
import com.openxu.chartlib.bean.StockBaseResult;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.manager.BaseChartManager;
import com.openxu.chartlib.manager.KLineManager;
import com.openxu.chartlib.manager.MinuteManager;
import com.openxu.chartlib.testdata.TestData;
import com.openxu.chartlib.utils.CommonUtil;
import com.openxu.chartlib.utils.JSONUtil;
import com.openxu.chartlib.utils.LogUtil;
/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : StockChartManager
 * version : 1.0
 * class describe：股票图表库操作入口类
 */
public class StockChartManager implements View.OnClickListener {
    private String TAG = "StockChartManager";

    /**展示的activity以及其activity布局*/
    private Activity activity;
    private ViewGroup rootView;
    /**顶部lable控件，展示股票基本信息*/
    private View layout_baseinfo;      //顶部基本信息
    private TextView tv_name, tv_code, tv_stop,
    tv_price, tv_price_add, tv_price_addp,
    tv_open, tv_heigh,
    tv_vol, tv_low, tv_turnover, tv_amplitude;
    private LinearLayout ll_price;

    private Button btn_minute, btn_kline;   //图表切换按钮

    private StockBaseInfo gpInfo;        //股票基本数据
    private String symbol;               //股票代码
    private boolean iszhishu = false;    //是否是指数
    private int status = 1;              //股票状态 01：正常开始
    private String price;
    private float raiseValue, raisePer;

    private BaseChartManager baseChartManager;    //图表管理器
    private KLineManager kLineManager;        //K线图管理器
    private MinuteManager minuteManager;          //分时图管理器
    /*四种图表类型*/
    public static final int MINUTETYPE = 0;
    public static final int KLINETYPE_DAY = 0X01;
    private int type = MINUTETYPE;       //当前展示的图标类型

    float y_price = Constants.EPSILON;    //昨收价格

    /**
     * 管理器构造方法
     * @param activity 图表展示的activity
     * @param symbol 股票代码
     */
    public StockChartManager(final Activity activity, String symbol) {
        //找到activity中根窗口mDecor中id为content的容器，然后获取其第一个子控件，也就是activity的布局
        this.rootView = (ViewGroup) ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
        this.activity = activity;
        this.symbol = symbol;

        //顶部基本信息
        this.layout_baseinfo = rootView.findViewById(R.id.layout_baseinfo);
        tv_name = (TextView) layout_baseinfo.findViewById(R.id.tv_name);
        tv_code = (TextView) layout_baseinfo.findViewById(R.id.tv_code);
        tv_stop = (TextView) layout_baseinfo.findViewById(R.id.tv_stop);        //停牌
        tv_price = (TextView) layout_baseinfo.findViewById(R.id.tv_price);      //价格
        tv_price_add = (TextView) layout_baseinfo.findViewById(R.id.tv_price_add);   //涨值
        tv_price_addp = (TextView) layout_baseinfo.findViewById(R.id.tv_price_addp);    //涨幅
        tv_open = (TextView) layout_baseinfo.findViewById(R.id.tv_open);      //今开
        tv_heigh = (TextView) layout_baseinfo.findViewById(R.id.tv_heigh);    //最高
        tv_vol = (TextView) layout_baseinfo.findViewById(R.id.tv_vol);        //成交
        tv_low = (TextView) layout_baseinfo.findViewById(R.id.tv_low);        //最低
        tv_turnover = (TextView) layout_baseinfo.findViewById(R.id.tv_turnover);   //换手
        tv_amplitude = (TextView) layout_baseinfo.findViewById(R.id.tv_amplitude); //振幅
        ll_price = (LinearLayout) layout_baseinfo.findViewById(R.id.ll_price);
        //图表切换按钮
        btn_minute = (Button)rootView.findViewById(R.id.btn_minute);
        btn_kline = (Button)rootView.findViewById(R.id.btn_kline);
        btn_minute.setOnClickListener(this);
        btn_kline.setOnClickListener(this);

        //图表管理器
        kLineManager = new KLineManager(rootView);
        kLineManager.setOnTopLableChangeListener(listener);
        minuteManager = new MinuteManager(rootView);
        minuteManager.setOnTopLableChangeListener(listener);
        baseChartManager = minuteManager;
    }

    /**
     * 展示入口
     */
    public void show() {
        baseChartManager.setLoadingViewVisibilty(View.VISIBLE);
        //TODO 模拟请求股票基本数据
        new Thread(){
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    String response= TestData.StockBaseData;
                    gpInfo = JSONUtil.jsonToBean(response,StockBaseResult.class).getData();
                    if (!TextUtils.isEmpty(gpInfo.getYesterday_price())||
                            !TextUtils.isEmpty(gpInfo.getOpen())) {
                        //获取昨收价
                        y_price = TextUtils.isEmpty(gpInfo.getYesterday_price())
                                ? Float.parseFloat(gpInfo.getOpen()) :
                                Float.parseFloat(gpInfo.getYesterday_price());
                    }
                    iszhishu = gpInfo.getType().equals("2");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //请求成功后展示股票基本信息
                            showBaseInfo(gpInfo);
                        }
                    });
                }catch (Exception e){
                }
            }
        }.start();
    }

    private void showBaseInfo(StockBaseInfo gpInfo) {
        if (gpInfo == null) {
            baseChartManager.setLoadingViewVisibilty(View.GONE);
            return;
        }
        this.gpInfo = gpInfo;

        if (TextUtils.isEmpty(gpInfo.getYesterday_price())
                && TextUtils.isEmpty(gpInfo.getOpen())) {
        } else
            y_price = TextUtils.isEmpty(gpInfo.getYesterday_price())
                    ? Float.parseFloat(gpInfo.getOpen()) :
                    Float.parseFloat(gpInfo.getYesterday_price());

        tv_name.setText(gpInfo.getName());
        tv_code.setText(symbol);
        try {
            this.status = Integer.parseInt(gpInfo.getStatus());
            if (this.status == 2) {
                //股票状态，01：正常开始  02：停牌   04临时停牌  -1：未开市   -2：已收盘  -3：休市
                tv_stop.setVisibility(View.VISIBLE);
                ll_price.setVisibility(View.GONE);
            }
        } catch (Exception e) {
        }

        if (y_price == Constants.EPSILON) {
            return;
        }
        updateTopLableByMinute(null, true);
        LogUtil.v(TAG, "昨收价："+y_price);

        kLineManager.setParams(gpInfo.getSymbol());
        iszhishu = gpInfo.getType().equals("2");
        minuteManager.setParams(gpInfo.getSymbol(), y_price);
        switchType(type);
    }

    /**
     * 切换图表（分时，日K，周K，月K）
     * @param type
     */
    public void switchType(int type) {
        this.type = type;
        baseChartManager.destroyRequest();
        this.type = type;
        if (type == MINUTETYPE) {
            baseChartManager = minuteManager;
            //分时图显示，K线图隐藏
            minuteManager.setVisible(View.VISIBLE);
            kLineManager.setVisible(View.GONE);
            if (gpInfo != null && status == 2) {
                minuteManager.setStopStatus();
                updateTopLableByStop();
                return;
            }
        } else {
            baseChartManager = kLineManager;
            minuteManager.setVisible(View.GONE);
            kLineManager.setVisible(View.VISIBLE);
            switch (type) {
                case KLINETYPE_DAY:
                    kLineManager.switchKlineType(KLineManager.K.daily);
                    break;
            }
        }
        /**
         * 如果基本吗数据没有获取到
         * 加载基本面数据
         */
        if (gpInfo == null) {
            show();
            return;
        }
        /**
         * 加载具体的图表数据
         */
        baseChartManager.show();
    }

    /********************************顶部基本信息相关start**********************************/
    /**
     * 根据盘口接口返回的换手率数据刷新顶部栏
     * @param pankouData
     */
    private void updateTopLableByPankou(PankouData.Data pankouData) {
        if (pankouData == null) return;
        gpInfo.setHsl(pankouData.hsl);
        gpInfo.setZhenfu(pankouData.zf);
        tv_turnover.setText("换手 " + gpInfo.getHsl());
        tv_amplitude.setText("振幅 " + gpInfo.getZhenfu());
    }

    private void updateTopLableByStop() {
        tv_stop.setVisibility(View.VISIBLE);
        ll_price.setVisibility(View.GONE);
        tv_open.setText("今开 --");
        tv_heigh.setText("最高 --");
        tv_low.setText("最低 --");
        tv_turnover.setText("换手 --");
        tv_amplitude.setText("振幅 --");
        tv_vol.setText("成交 --");
    }

    private void updateTopLableByMinute(MinutesBean mb, boolean isCancel) {
        LogUtil.v(TAG, "修改顶部控件："+mb);
        if (mb == null) mb = new MinutesBean();
        tv_open.setText("今开 " + gpInfo.getOpen());
        tv_heigh.setText("最高 " + gpInfo.getHigh());
        tv_low.setText("最低 " + gpInfo.getLow());
        if (iszhishu) {
            tv_turnover.setText("换手 --");
        } else {
            tv_turnover.setText("换手 " + gpInfo.getHsl());
        }
        tv_amplitude.setText("振幅 " + gpInfo.getZhenfu());

        if (isCancel) {
            tv_vol.setText("成交 " + gpInfo.getVolume());
        } else {
            String[] vollables = CommonUtil.getDisplayVolume(String.valueOf(mb.cjnum).length(), mb.cjnum);
            tv_vol.setText("成交 " + vollables[0] + vollables[1]);
        }
        if(mb.cjprice== Constants.EPSILON){
            price="--";
            tv_price_add.setTextColor(Color.DKGRAY);
            tv_price_addp.setTextColor(Color.DKGRAY);
            tv_price.setTextColor(Color.DKGRAY);
            tv_price.setText("--");
            tv_price_add.setText("--");
            tv_price_addp.setText("--");
        }else {
            price = Constants.twoPointFormat.format(mb.cjprice);
            tv_price.setText(price);//此处应该为今日收盘价，基本面接口没有返回
            raiseValue = mb.cjprice - y_price;  //20.84 - 21.2
            raisePer = raiseValue / y_price;
            LogUtil.e(TAG, "price="+price+"  y_price ="+y_price+"  价格增长值："+raiseValue+"   涨幅："+raisePer);
            if (raiseValue > 0) {
                tv_price_add.setTextColor(Constants.C_RED_TEXT);
                tv_price_addp.setTextColor(Constants.C_RED_TEXT);
                tv_price.setTextColor(Constants.C_RED_TEXT);
                tv_price_add.setText("+"+ Constants.twoPointFormat.format(raiseValue));
                tv_price_addp.setText("+"+ Constants.twoPointFormat.format(raisePer * 100) + "%");
            } else if (raiseValue < 0) {
                tv_price_add.setTextColor(Constants.C_GREEN_TEXT);
                tv_price_addp.setTextColor(Constants.C_GREEN_TEXT);
                tv_price.setTextColor(Constants.C_GREEN_TEXT);
                tv_price_add.setText(Constants.twoPointFormat.format(raiseValue));
                tv_price_addp.setText(Constants.twoPointFormat.format(raisePer * 100) + "%");
            } else {
                tv_price_add.setTextColor(Color.DKGRAY);
                tv_price_addp.setTextColor(Color.DKGRAY);
                tv_price.setTextColor(Color.DKGRAY);
                tv_price_add.setText(Constants.twoPointFormat.format(raiseValue));
                tv_price_addp.setText(Constants.twoPointFormat.format(raisePer * 100) + "%");
            }
        }
    }
    private void updateTopLableByKeyLine(KeyLineItem keyLineItem, boolean isCancel) {
        float raiseV, raisep;
        if (isCancel) {
            if (status == 2) {
                updateTopLableByStop();
                return;
            }
            tv_open.setText("今开 " + gpInfo.getOpen());
            tv_heigh.setText("最高 " + gpInfo.getHigh());
            tv_low.setText("最低 " + gpInfo.getLow());
            if (iszhishu) {
                tv_turnover.setText("换手 --");
            } else {
                tv_turnover.setText("换手 " + gpInfo.getHsl());
            }
            tv_amplitude.setText("振幅 " + gpInfo.getZhenfu());
            tv_vol.setText("成交 " + gpInfo.getVolume());

            tv_price.setText(price);
            raiseV = raiseValue;
            raisep = raisePer;
            if(price.equals("--")){
                tv_price_add.setTextColor(Color.DKGRAY);
                tv_price_addp.setTextColor(Color.DKGRAY);
                tv_price.setTextColor(Color.DKGRAY);
                tv_price_add.setText("--");
                tv_price_addp.setText("--");
                return;
            }
        } else {
            tv_stop.setVisibility(View.GONE);
            ll_price.setVisibility(View.VISIBLE);
            if (iszhishu) {
                tv_turnover.setText("换手 --");
            } else {
                tv_turnover.setText("换手 " + Constants.twoPointFormat.format(keyLineItem.turnover*100) + "%");
            }
            tv_amplitude.setText("振幅 " + Constants.twoPointFormat.format(keyLineItem.zhenfu*100) + "%");
            tv_open.setText("今开 " + Constants.twoPointFormat.format(keyLineItem.open));
            String[] vollables = CommonUtil.getDisplayVolume(String.valueOf(keyLineItem.vol).length(), keyLineItem.vol);
            tv_vol.setText("成交 " + vollables[0] + vollables[1]);
            tv_heigh.setText("最高 " + Constants.twoPointFormat.format(keyLineItem.high));
            tv_low.setText("最低 " + Constants.twoPointFormat.format(keyLineItem.low));
            tv_price.setText(Constants.twoPointFormat.format(keyLineItem.close));
            raiseV = keyLineItem.close - keyLineItem.open;
            raisep = raiseV / keyLineItem.open;
        }
        if (raiseV > 0) {
            tv_price_add.setTextColor(Constants.C_RED_TEXT);
            tv_price_addp.setTextColor(Constants.C_RED_TEXT);
            tv_price.setTextColor(Constants.C_RED_TEXT);
            tv_price_add.setText("+"+ Constants.twoPointFormat.format(raiseV));
            tv_price_addp.setText("+"+ Constants.twoPointFormat.format(raisep * 100) + "%");

        } else if(raiseV<0){
            tv_price_add.setTextColor(Constants.C_GREEN_TEXT);
            tv_price_addp.setTextColor(Constants.C_GREEN_TEXT);
            tv_price.setTextColor(Constants.C_GREEN_TEXT);
            tv_price_add.setText(Constants.twoPointFormat.format(raiseV));
            tv_price_addp.setText(Constants.twoPointFormat.format(raisep * 100) + "%");
        }else {
            tv_price_add.setTextColor(Color.DKGRAY);
            tv_price_addp.setTextColor(Color.DKGRAY);
            tv_price.setTextColor(Color.DKGRAY);
            tv_price_add.setText(Constants.twoPointFormat.format(raiseValue));
            tv_price_addp.setText(Constants.twoPointFormat.format(raisePer * 100) + "%");
        }
    }

    /********************************顶部基本信息相关over**********************************/
    /** 图表切换*/
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_minute:
                switchType(StockChartManager.MINUTETYPE);
                break;
            case R.id.btn_kline:
                switchType(StockChartManager.KLINETYPE_DAY);
                break;
        }
    }

    public interface OnFocusChangeListener {
        void onPankouChange(PankouData.Data data);

        void onfocusChange(boolean isCancel, KeyLineItem keyLineItem, MinutesBean minutesBean);
    }
    OnFocusChangeListener listener = new OnFocusChangeListener() {
        @Override
        public void onPankouChange(PankouData.Data data) {
            updateTopLableByPankou(data);
        }
        @Override
        public void onfocusChange(boolean isCancel, KeyLineItem keyLineItem, MinutesBean minutesBean) {
            if (type == MINUTETYPE) {
                updateTopLableByMinute(minutesBean, isCancel);
            } else
                updateTopLableByKeyLine(keyLineItem, isCancel);
        }
    };

    public void destroryRequest() {
        if (minuteManager != null)
            minuteManager.destroyRequest();
        if (kLineManager != null)
            kLineManager.destroyRequest();
    }

}
