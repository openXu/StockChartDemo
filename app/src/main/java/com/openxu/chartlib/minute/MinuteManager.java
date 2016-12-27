package com.openxu.chartlib.minute;

import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.openxu.chart.R;
import com.openxu.chartlib.StockChartManager;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.manager.BaseChartManager;
import com.openxu.chartlib.minute.bean.MinuteParame;
import com.openxu.chartlib.minute.bean.MinutesBean;
import com.openxu.chartlib.minute.bean.PankouData;
import com.openxu.chartlib.utils.TouchEventUtil;

import java.util.List;


/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : MinuteManager
 * version : 1.0
 * class describe：分时图管理类
 */
public class MinuteManager extends BaseChartManager {

    private MinuteRequest mRequestUtil;
    private List<MinutesBean> datas;    //分时图数据
    private PankouData.Data panKouDatas;//盘口数据

    private String sysmbol = null;
    // 昨日收盘价
    private float prePrice = (float) 12.82;
    private boolean isZhishu=false;

    public MinuteView minuteView;

    private MinuteParame parame;
    private StockChartManager.OnFocusChangeListener outListener=null;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.FocusCancelFlag:    //焦点结束
                    minuteView.updateFocusView(true, datas.get(datas.size() - 1));
                    outListener.onfocusChange(true,null,datas.get(datas.size() - 1));
                    break;
                case Constants.FocusChangeFlag:    //焦点位置变化
                    MinutesBean minutesBean = (MinutesBean) msg.obj;
                    minuteView.updateFocusView(false, minutesBean);
                    outListener.onfocusChange(false,null,minutesBean);
                    break;
                case Constants.ChartRefresh:
                    setLoadingViewVisibilty(View.GONE);
                    if(datas==null||datas.size()==0)return;
                    //重绘分时图
                    minuteView.updateChartView(parame, datas);
                    outListener.onfocusChange(true,null,datas.get(datas.size() - 1));
                    break;
            }

        }
    };

    @Override
    protected View getLoadingView() {
        return minuteView.getLoadingView();
    }

    public MinuteManager(View rootview) {
        super(rootview);
        this.rootView = rootview;
        View lv = rootview.findViewById(R.id.minutelayout);
        minuteView = new MinuteView(rootview.getContext(), lv, foucsChangedListener);
    }
    public void setParams(String symbol, float prePrice,boolean isZhishu){
       this.sysmbol = symbol;
        this.prePrice = prePrice;
        this.isZhishu=isZhishu;
        minuteView.setPrePrice(prePrice);

        minuteView.setIsZhishu(isZhishu);
    }
    public void setOnTopLableChangeListener(StockChartManager.OnFocusChangeListener outlistener){
        this.outListener=outlistener;
    }

    /**展示分时图，请求数据*/
    @Override
    public void showChart() {
        if(mRequestUtil==null){
            mRequestUtil = new MinuteRequest(this, sysmbol, isZhishu,prePrice);
        }
        mRequestUtil.request();
    }

    /** 请求分数图数据成功后回调*/
    public void setData(List<MinutesBean> list, MinuteParame parame, boolean isStop) {
        this.datas = list;
        this.parame = parame;
        minuteView.setIsStop(isStop);
        handler.sendEmptyMessage(Constants.ChartRefresh);
    }
    /** 请求盘口数据成功后回调*/
    public void setPankouData(PankouData.Data pankouData) {
        panKouDatas = pankouData;
        minuteView.updatePankouData(pankouData,prePrice);
        outListener.onPankouChange(pankouData);
    }


    @Override
    public void cancelRequest() {
        if(mRequestUtil!=null){
            mRequestUtil.destoryRequest();
        }
    }

    @Override
    public void destroyRequest(){
       cancelRequest();
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


    public void setStopStatus(){
        setLoadingViewVisibilty(View.GONE);
        isZhishu=true;
        minuteView.setIsZhishu(true);
    }
}
