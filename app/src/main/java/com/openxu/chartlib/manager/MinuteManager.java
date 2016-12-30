package com.openxu.chartlib.manager;

import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.openxu.chart.R;
import com.openxu.chartlib.StockChartManager;
import com.openxu.chartlib.bean.MinuteParame;
import com.openxu.chartlib.bean.MinutesBean;
import com.openxu.chartlib.bean.PankouData;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.request.MinuteRequest;
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
 * class describe：分时管理类，主要是管理分时的横竖屏的不同处理
 */
public class MinuteManager extends BaseChartManager {

    private boolean isStop = false;

    private MinuteRequest mRequestUtil;
    private List<MinutesBean> datas;    //分时图数据
    private PankouData.Data panKouDatas;//盘口数据

    private String sysmbol = null;
    // 昨日收盘价
    private float prePrice = (float) 12.82;

    private MinuteView viewManager;

    private MinuteParame parame;
    private StockChartManager.OnFocusChangeListener outListener=null;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.FocusCancelFlag:    //焦点结束
                    viewManager.updateFocusView(true, datas.get(datas.size() - 1));
                    outListener.onfocusChange(true,null,datas.get(datas.size() - 1));
                    break;
                case Constants.FocusChangeFlag:    //焦点位置变化
                    MinutesBean minutesBean = (MinutesBean) msg.obj;
                    viewManager.updateFocusView(false, minutesBean);
                    outListener.onfocusChange(false,null,minutesBean);
                    break;
                case Constants.ChartRefresh:
                    setLoadingViewVisibilty(View.GONE);
                    if(datas==null||datas.size()==0)return;
                    //重绘分时图
                    viewManager.updateChartView(parame, datas);
                    outListener.onfocusChange(true,null,datas.get(datas.size() - 1));
                    break;
            }


        }
    };


    public MinuteManager(View rootview) {
        View layout_minute = rootview.findViewById(R.id.layout_minute);
        viewManager = new MinuteView(rootview.getContext(), layout_minute, foucsChangedListener);
    }
    public void setParams(String symbol, float prePrice){
        this.sysmbol = symbol;
        this.prePrice = prePrice;
        viewManager.setPrePrice(prePrice);
    }
    public void setOnTopLableChangeListener(StockChartManager.OnFocusChangeListener outlistener){
        this.outListener=outlistener;
    }

    @Override
    public void showChart() {
        if(mRequestUtil==null){
            mRequestUtil = new MinuteRequest(this, sysmbol, prePrice);
        }
        mRequestUtil.request();
    }
    @Override
    public void destroyRequest(){
        if(mRequestUtil!=null){
            mRequestUtil.destoryRequest();
        }
    }
    @Override
    protected View getLoadingView() {
        return viewManager.getLoadingView();
    }
    @Override
    public void setVisible(int visibilty) {
        viewManager.setVisibility(visibilty);
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


    /**
     * 请求分数图数据成功后回调
     * @param list
     * @param parame
     * @param isStop
     */
    public void setData(List<MinutesBean> list, MinuteParame parame, boolean isStop) {
        this.datas = list;
        this.isStop = isStop;
        this.parame = parame;
        viewManager.setIsStop(isStop);
        handler.sendEmptyMessage(Constants.ChartRefresh);
    }

    /**
     * 请求盘口数据成功后回调
     * @param pankouData
     */
    public void setPankouData(PankouData.Data pankouData) {
        panKouDatas = pankouData;
        viewManager.updatePankouData(pankouData,prePrice);
        outListener.onPankouChange(pankouData);
    }

    public void setStopStatus(){
        setLoadingViewVisibilty(View.GONE);
    }
}
