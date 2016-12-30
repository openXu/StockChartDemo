package com.openxu.chartlib.manager;

import android.view.View;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : BaseChartManager
 * version : 1.0
 * class describe：k线和分时管理类的基类
 */
public abstract class BaseChartManager {

    public abstract void destroyRequest();
    protected abstract void showChart();
    protected abstract View getLoadingView();
    protected abstract void setVisible(int Visibilty);

    /**
     * 设置加载进度view的显示状态
     */
    public void setLoadingViewVisibilty(int Visibilty){
        getLoadingView().setVisibility(Visibilty);
    }

    /**
     * 请求网络数据，显示行情图
     */
    public void show(){
        setLoadingViewVisibilty(View.VISIBLE);
        showChart();
    }
}
