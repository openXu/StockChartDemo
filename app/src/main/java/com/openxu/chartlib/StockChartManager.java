package com.openxu.chartlib;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.openxu.chart.R;
import com.openxu.chartlib.bean.StockBaseInfo;
import com.openxu.chartlib.callback.BasicResultCallBack;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.kline.KeyLineManager;
import com.openxu.chartlib.kline.entity.KeyLineItem;
import com.openxu.chartlib.manager.BaseChartManager;
import com.openxu.chartlib.minute.MinuteManager;
import com.openxu.chartlib.minute.bean.MinutesBean;
import com.openxu.chartlib.minute.bean.PankouData;
import com.openxu.chartlib.request.StockBaseRequest;
import com.openxu.chartlib.utils.CommonUtil;
import com.openxu.chartlib.utils.LogUtil;
import com.openxu.chartlib.utils.ToastAlone;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : StockChartManager
 * version : 1.0
 * class describe：股票图表库操作入口类
 * 控制行情图的请求，展示，切换，销毁等操作以及顶部栏的展示和隐藏，数据更新等。
 */
public class StockChartManager implements RadioGroup.OnCheckedChangeListener {
    private String TAG = "StockChartManager";

    /**展示的activity以及其activity布局*/
    private Activity activity;
    private ViewGroup rootView;
    /**顶部lable控件，展示股票基本信息*/
    private TextView tv_top_name, tv_top_code, tv_top_stop,
            tv_top_price, tv_top_price_addvalue, tv_top_price_addp,
            tv_top_open, tv_top_heigh,
            tv_top_vol, tv_top_low, tv_top_turnover, tv_top_amplitude;
    private LinearLayout ll_top_price;

    private RadioGroup radioGroup;   //图表类型切换

    private StockBaseRequest stockBaseRequest;    //股票基本信息请求

    private StockBaseInfo gpInfo;        //股票基本数据
    private String symbol;               //股票代码
    private boolean iszhishu = false;    //是否是指数
    private int status = 1;              //股票状态 01：正常开始
    private String price;
    private float raiseValue, raisePer;

    private BaseChartManager baseChartManager;    //当前正在显示的图表的管理器
    private KeyLineManager keyLineManager;        //K线图管理器
    private MinuteManager minuteManager;          //分时图管理器
    /*四种图表类型*/
    public static final int MINUTETYPE = 0;
    public static final int KLINETYPE_DAY = 0X01;
    public static final int KLINETYPE_WEEK = 0X02;
    public static final int KLINETYPE_MONTH = 0X03;
    private int type = MINUTETYPE;       //当前展示的图标类型

    float y_price = Constants.EPSILON;    //昨收价格

    /**
     * 管理器构造方法
     * @param activity 图表展示的activity
     * @param symbol 股票代码
     */
    public StockChartManager(final Activity activity, String symbol) {
        //找到activity中根窗口mDecor中id为content的容器，然后获取其第一个子控件，也就是整个activity的布局
        this.rootView = (ViewGroup) ((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
        this.activity = activity;
        this.symbol = symbol;
        //初始化顶部基本信息相关控件
        initTopLable(rootView.findViewById(R.id.layoutBasicInfo));
        //初始化网络请求组件
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS)
                .build();
        OkHttpUtils.initClient(okHttpClient);

        //初始化K线图和分时图的管理器
        keyLineManager = new KeyLineManager(rootView);
        minuteManager = new MinuteManager(rootView);
        //设置焦点变化监听，当手指滑动到不同位置时，改变对应的顶部股票数据
        keyLineManager.setOnTopLableChangeListener(listener);
        minuteManager.setOnTopLableChangeListener(listener);
        //初始化图表切换控件，并设置选择监听
        radioGroup = (RadioGroup) rootView.findViewById(R.id.switchradiogroup);
        radioGroup.setOnCheckedChangeListener(this);
        //默认开始显示分时图
        baseChartManager = minuteManager;

        //初始化股票基本信息数据请求类
        stockBaseRequest = new StockBaseRequest(symbol, new BasicResultCallBack() {
            @Override
            public void callback(StockBaseInfo stockBaseInfo) {
                //请求成功后展示股票基本信息
                showBaseInfo(stockBaseInfo);
            }
        });

    }

    /**展示入口方法*/
    public void show() {
        baseChartManager.setLoadingViewVisibilty(View.VISIBLE);
        if (!CommonUtil.isConnected(rootView.getContext())) {
            //显示loading
            baseChartManager.setLoadingViewVisibilty(View.GONE);
            ToastAlone.show("网络错误");
            return;
        }
        //根据股票代码，请求相关数据
        stockBaseRequest.requestBasicData();
    }

    /**展示股票基本信息*/
    private void showBaseInfo(StockBaseInfo gpInfo) {
        if (gpInfo == null) {
            baseChartManager.setLoadingViewVisibilty(View.GONE);
            return;
        }
        this.gpInfo = gpInfo;
        LogUtil.v(TAG, "股票基本信息："+gpInfo);

        if (!TextUtils.isEmpty(gpInfo.getYesterday_price())||
                !TextUtils.isEmpty(gpInfo.getOpen())) {
            //获取昨收价
            y_price = TextUtils.isEmpty(gpInfo.getYesterday_price())
                    ? Float.parseFloat(gpInfo.getOpen()) :
                    Float.parseFloat(gpInfo.getYesterday_price());
        }
        //股票名称和代码
        tv_top_name.setText(gpInfo.getName());
        tv_top_code.setText(symbol);
        //根据状态控制显示停牌 还是 正常价格
        try {
            this.status = Integer.parseInt(gpInfo.getStatus());
            if (this.status == 2) {
                //股票状态，01：正常开始  02：停牌   04临时停牌  -1：未开市   -2：已收盘  -3：休市
                tv_top_stop.setVisibility(View.VISIBLE);
                ll_top_price.setVisibility(View.GONE);
            }
        } catch (Exception e) {
        }
        if (y_price == Constants.EPSILON) {
            return;
        }
        //将数据显示在顶部
        updateTopLableByMinute(null, true);
        //为管理器设置必要参数
        keyLineManager.setParams(gpInfo.getSymbol());
        iszhishu = gpInfo.getType().equals("2");
        minuteManager.setParams(gpInfo.getSymbol(), y_price, iszhishu);
        //展示相应的图标（分时图or K线图）
        switchType(type);
    }

    /**
     * 切换图表（分时，日K，周K，月K）
     * @param type
     */
    public void switchType(int type) {
        this.type = type;
        baseChartManager.cancelRequest();
        this.type = type;
        if (type == MINUTETYPE) {
            //将当前展示的管理器切换为分时图管理器
            baseChartManager = minuteManager;
            //如果当前type是分时图，那么找到分时图的布局让其显示，K线图隐藏
            rootView.findViewById(R.id.minutelayout).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.keylinelayout).setVisibility(View.GONE);
            if (gpInfo != null && status == 2) {
                minuteManager.setStopStatus();
                updateTopLableByStop();
                return;
            }
        } else {
            //将当前展示的管理器切换为K线图管理器
            baseChartManager = keyLineManager;
            rootView.findViewById(R.id.keylinelayout).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.minutelayout).setVisibility(View.GONE);
            switch (type) {
                case KLINETYPE_DAY:
                    keyLineManager.switchKlineType(KeyLineManager.K.daily);
                    break;
                case KLINETYPE_MONTH:
                    keyLineManager.switchKlineType(KeyLineManager.K.month);
                    break;
                case KLINETYPE_WEEK:
                    keyLineManager.switchKlineType(KeyLineManager.K.week);
                    break;
            }
        }
        //如果基本吗数据没有获取到 加载基本面数据
        if (gpInfo == null) {
            show();
            return;
        }
        /**调用相应图标的管理的show()方法，开始请求相应的数据*/
        baseChartManager.show();
    }

    /********************************顶部基本信息相关start**********************************/
    private void initTopLable(View topLableView) {
        tv_top_name = (TextView) topLableView.findViewById(R.id.tv_top_name);
        tv_top_code = (TextView) topLableView.findViewById(R.id.tv_top_code);
        tv_top_stop = (TextView) topLableView.findViewById(R.id.tv_top_stop);        //停牌
        tv_top_price = (TextView) topLableView.findViewById(R.id.tv_top_price);      //价格
        tv_top_price_addvalue = (TextView) topLableView.findViewById(R.id.tv_top_price_addvalue);   //涨值
        tv_top_price_addp = (TextView) topLableView.findViewById(R.id.tv_top_price_addp);    //涨幅
        tv_top_open = (TextView) topLableView.findViewById(R.id.tv_top_open);      //今开
        tv_top_heigh = (TextView) topLableView.findViewById(R.id.tv_top_heigh);    //最高
        tv_top_vol = (TextView) topLableView.findViewById(R.id.tv_top_vol);        //成交
        tv_top_low = (TextView) topLableView.findViewById(R.id.tv_top_low);        //最低
        tv_top_turnover = (TextView) topLableView.findViewById(R.id.tv_top_turnover);   //换手
        tv_top_amplitude = (TextView) topLableView.findViewById(R.id.tv_top_amplitude); //振幅
        this.ll_top_price = (LinearLayout) topLableView.findViewById(R.id.ll_top_price);
    }
    /**
     * 根据盘口接口返回的换手率数据刷新顶部栏
     * @param pankouData
     */
    private void updateTopLableByPankou(PankouData.Data pankouData) {
        if (pankouData == null) return;
        gpInfo.setHsl(pankouData.hsl);
        gpInfo.setZhenfu(pankouData.zf);
        tv_top_turnover.setText("换手 " + gpInfo.getHsl());
        tv_top_amplitude.setText("振幅 " + gpInfo.getZhenfu());
    }

    private void updateTopLableByStop() {
        tv_top_stop.setVisibility(View.VISIBLE);
        ll_top_price.setVisibility(View.GONE);
        tv_top_open.setText("今开 --");
        tv_top_heigh.setText("最高 --");
        tv_top_low.setText("最低 --");
        tv_top_turnover.setText("换手 --");
        tv_top_amplitude.setText("振幅 --");
        tv_top_vol.setText("成交 --");
    }

    /**分时图焦点变化后，刷新股票基本信息*/
    private void updateTopLableByMinute(MinutesBean mb, boolean isCancel) {
        if (mb == null)
            mb = new MinutesBean();
        tv_top_open.setText("今开 " + gpInfo.getOpen());
        tv_top_heigh.setText("最高 " + gpInfo.getHigh());
        tv_top_low.setText("最低 " + gpInfo.getLow());
        if (iszhishu) {
            tv_top_turnover.setText("换手 --");
        } else {
            tv_top_turnover.setText("换手 " + gpInfo.getHsl());
        }
        tv_top_amplitude.setText("振幅 " + gpInfo.getZhenfu());

        if (isCancel) {
            tv_top_vol.setText("成交 " + gpInfo.getVolume());
        } else {
            String[] vollables = CommonUtil.getDisplayVolume(String.valueOf(mb.cjnum).length(), mb.cjnum);
            tv_top_vol.setText("成交 " + vollables[0] + vollables[1]);
        }
        if(mb.cjprice== Constants.EPSILON){
            price="--";
            tv_top_price_addvalue.setTextColor(Color.DKGRAY);
            tv_top_price_addp.setTextColor(Color.DKGRAY);
            tv_top_price.setTextColor(Color.DKGRAY);
            tv_top_price.setText("--");
            tv_top_price_addvalue.setText("--");
            tv_top_price_addp.setText("--");
        }else {
            price = Constants.twoPointFormat.format(mb.cjprice);
            tv_top_price.setText(price);//此处应该为今日收盘价，基本面接口没有返回
            raiseValue = mb.cjprice - y_price;
            raisePer = raiseValue / y_price;
            if (raiseValue > 0) {
                tv_top_price_addvalue.setTextColor(Constants.C_RED_TEXT);
                tv_top_price_addp.setTextColor(Constants.C_RED_TEXT);
                tv_top_price.setTextColor(Constants.C_RED_TEXT);
                tv_top_price_addvalue.setText("+"+ Constants.twoPointFormat.format(raiseValue));
                tv_top_price_addp.setText("+"+ Constants.twoPointFormat.format(raisePer * 100) + "%");
            } else if (raiseValue < 0) {
                tv_top_price_addvalue.setTextColor(Constants.C_GREEN_TEXT);
                tv_top_price_addp.setTextColor(Constants.C_GREEN_TEXT);
                tv_top_price.setTextColor(Constants.C_GREEN_TEXT);
                tv_top_price_addvalue.setText(Constants.twoPointFormat.format(raiseValue));
                tv_top_price_addp.setText(Constants.twoPointFormat.format(raisePer * 100) + "%");
            } else {
                tv_top_price_addvalue.setTextColor(Color.DKGRAY);
                tv_top_price_addp.setTextColor(Color.DKGRAY);
                tv_top_price.setTextColor(Color.DKGRAY);
                tv_top_price_addvalue.setText(Constants.twoPointFormat.format(raiseValue));
                tv_top_price_addp.setText(Constants.twoPointFormat.format(raisePer * 100) + "%");
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
            tv_top_open.setText("今开 " + gpInfo.getOpen());
            tv_top_heigh.setText("最高 " + gpInfo.getHigh());
            tv_top_low.setText("最低 " + gpInfo.getLow());
            if (iszhishu) {
                tv_top_turnover.setText("换手 --");
            } else {
                tv_top_turnover.setText("换手 " + gpInfo.getHsl());
            }
            tv_top_amplitude.setText("振幅 " + gpInfo.getZhenfu());
            tv_top_vol.setText("成交 " + gpInfo.getVolume());

            tv_top_price.setText(price);
            raiseV = raiseValue;
            raisep = raisePer;
            if(price.equals("--")){
                tv_top_price_addvalue.setTextColor(Color.DKGRAY);
                tv_top_price_addp.setTextColor(Color.DKGRAY);
                tv_top_price.setTextColor(Color.DKGRAY);
                tv_top_price_addvalue.setText("--");
                tv_top_price_addp.setText("--");
                return;
            }
        } else {
            tv_top_stop.setVisibility(View.GONE);
            ll_top_price.setVisibility(View.VISIBLE);
            if (iszhishu) {
                tv_top_turnover.setText("换手 --");
            } else {
                tv_top_turnover.setText("换手 " + Constants.twoPointFormat.format(keyLineItem.turnover*100) + "%");
            }
            tv_top_amplitude.setText("振幅 " + Constants.twoPointFormat.format(keyLineItem.zhenfu*100) + "%");
            tv_top_open.setText("今开 " + Constants.twoPointFormat.format(keyLineItem.open));
            String[] vollables = CommonUtil.getDisplayVolume(String.valueOf(keyLineItem.vol).length(), keyLineItem.vol);
            tv_top_vol.setText("成交 " + vollables[0] + vollables[1]);
            tv_top_heigh.setText("最高 " + Constants.twoPointFormat.format(keyLineItem.high));
            tv_top_low.setText("最低 " + Constants.twoPointFormat.format(keyLineItem.low));
            tv_top_price.setText(Constants.twoPointFormat.format(keyLineItem.close));
            raiseV = keyLineItem.close - keyLineItem.open;
            raisep = raiseV / keyLineItem.open;
        }
        if (raiseV > 0) {
            tv_top_price_addvalue.setTextColor(Constants.C_RED_TEXT);
            tv_top_price_addp.setTextColor(Constants.C_RED_TEXT);
            tv_top_price.setTextColor(Constants.C_RED_TEXT);
            tv_top_price_addvalue.setText("+"+ Constants.twoPointFormat.format(raiseV));
            tv_top_price_addp.setText("+"+ Constants.twoPointFormat.format(raisep * 100) + "%");

        } else if(raiseV<0){
            tv_top_price_addvalue.setTextColor(Constants.C_GREEN_TEXT);
            tv_top_price_addp.setTextColor(Constants.C_GREEN_TEXT);
            tv_top_price.setTextColor(Constants.C_GREEN_TEXT);
            tv_top_price_addvalue.setText(Constants.twoPointFormat.format(raiseV));
            tv_top_price_addp.setText(Constants.twoPointFormat.format(raisep * 100) + "%");
        }else {
            tv_top_price_addvalue.setTextColor(Color.DKGRAY);
            tv_top_price_addp.setTextColor(Color.DKGRAY);
            tv_top_price.setTextColor(Color.DKGRAY);
            tv_top_price_addvalue.setText(Constants.twoPointFormat.format(raiseValue));
            tv_top_price_addp.setText(Constants.twoPointFormat.format(raisePer * 100) + "%");
        }
    }


    /********************************顶部基本信息相关over**********************************/
    /** 图表切换*/
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.fenshi_radio) {
            //展示分时图
            switchType(StockChartManager.MINUTETYPE);
        } else if (checkedId == R.id.day_radio) {
            //展示日K
            switchType(StockChartManager.KLINETYPE_DAY);
        } else if (checkedId == R.id.week_radio) {
            //展示周K
            switchType(StockChartManager.KLINETYPE_WEEK);
        } else if (checkedId == R.id.month_radio) {
            //展示月K
            switchType(StockChartManager.KLINETYPE_MONTH);
        }
    }


    /**
     *
     * @param outState
     */
    public void saveInstanceState(Bundle outState) {
        if (type == MINUTETYPE) return;
        keyLineManager.saveInstanceState(outState);
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
        if (keyLineManager != null)
            keyLineManager.destroyRequest();
    }

}
