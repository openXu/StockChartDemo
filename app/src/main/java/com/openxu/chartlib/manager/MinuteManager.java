package com.openxu.chartlib.manager;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.openxu.chart.R;
import com.openxu.chartlib.bean.MinuteParame;
import com.openxu.chartlib.bean.MinutesBean;
import com.openxu.chartlib.bean.PankouData;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.request.MinuteRequest;
import com.openxu.chartlib.utils.LogUtil;
import com.openxu.chartlib.utils.TouchEventUtil;
import com.openxu.chartlib.view.MinuteView;

import java.util.List;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : StockChartManager
 * version : 1.0
 * class describe：分时图管理器
 */
public class MinuteManager extends BaseManager {

    private MinuteRequest mRequestUtil;
    public MinuteView minuteView;
    private MinuteParame parame;
    private List<MinutesBean> datas;    //分时图数据
    private PankouData.Data panKouDatas;//盘口数据

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.FocusCancelFlag:    //焦点结束
                    minuteView.updateFocusView(true, datas.get(datas.size() - 1));
                    listener.onfocusChange(true,null,datas.get(datas.size() - 1));
                    break;
                case Constants.FocusChangeFlag:    //焦点位置变化
                    MinutesBean minutesBean = (MinutesBean) msg.obj;
                    minuteView.updateFocusView(false, minutesBean);
                    listener.onfocusChange(false,null,minutesBean);
                    break;
                case Constants.ChartRefresh:
                    setLoadingViewVisibilty(View.GONE);
                    if(datas==null||datas.size()==0)return;
                    //重绘分时图
                    minuteView.updateChartView(parame, datas);
                    listener.onfocusChange(true,null,datas.get(datas.size() - 1));
                    break;
            }

        }
    };

    /**
     * 管理器构造方法
     * @param activity 图表展示的activity
     * @param symbol 股票代码
     */
    public MinuteManager(final Activity activity, String symbol) {
        super(activity, symbol);
        this.type = MINUTETYPE;       //当前展示的图标类型
        View lv = rootView.findViewById(R.id.minutelayout);
        minuteView = new MinuteView(rootView.getContext(), lv, foucsChangedListener);
    }

    /**展示分时图，请求数据*/
    @Override
    public void showChart() {
        if(mRequestUtil==null){
            mRequestUtil = new MinuteRequest(this, symbol, iszhishu,y_price);
        }
        mRequestUtil.request();
    }
    @Override
    public void cancelRequest() {
        if(mRequestUtil!=null){
            mRequestUtil.destoryRequest();
        }
    }
    /** 请求分数图数据成功后回调*/
    public void setData(List<MinutesBean> list, MinuteParame parame, boolean isStop) {
        LogUtil.v(TAG ,"展示分时图");
        this.datas = list;
        this.parame = parame;
        minuteView.setIsStop(isStop);
        handler.sendEmptyMessage(Constants.ChartRefresh);
    }
    /** 请求盘口数据成功后回调*/
    public void setPankouData(PankouData.Data pankouData) {
        LogUtil.v(TAG ,"展示盘口数据");
        panKouDatas = pankouData;
        minuteView.updatePankouData(pankouData, y_price);
        listener.onPankouChange(pankouData);
    }

    /**设置加载进度view的显示状态*/
    public void setLoadingViewVisibilty(int Visibilty){
        minuteView.getLoadingView().setVisibility(Visibilty);
    }

    private TouchEventUtil.OnFoucsChangedListener foucsChangedListener =
            new TouchEventUtil.OnFoucsChangedListener() {
                @Override
                public void foucsChanged(int flag, int index) {
                    if (flag == Constants.FocusCancelFlag) {
                        handler.sendEmptyMessage(Constants.FocusCancelFlag);
                    } else {
                        if (index >= datas.size() || index < 0) return;
                        final MinutesBean minutesBean = datas.get(index);
                        Message msg = handler.obtainMessage(Constants.FocusChangeFlag);
                        msg.obj = minutesBean;
                        handler.sendMessage(msg);
                    }
                }
            };

}
