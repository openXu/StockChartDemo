package com.openxu.chartlib.manager;

import android.view.View;
import android.widget.RadioGroup;

import com.openxu.chart.R;

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
    private RadioGroup radioGroup=null;
    protected View rootView=null;

    protected abstract View getLoadingView();
    protected abstract void showChart();
    public abstract void cancelRequest();
    public abstract void destroyRequest();

    public BaseChartManager(View rootView){
        this.rootView=rootView;
        this.radioGroup=(RadioGroup) rootView.findViewById(R.id.switchradiogroup);
    }
    /**设置切换按钮不能点击*/
    protected void disableRadioGroup() {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(false);
        }
    }
    /**设置切换按钮能点击*/
    protected void enableRadioGroup() {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(true);
        }
    }
    /**设置加载进度view的显示状态*/
    public void setLoadingViewVisibilty(int Visibilty){
        getLoadingView().setVisibility(Visibilty);
        if(Visibilty==View.VISIBLE){
            disableRadioGroup();
        }else{
            enableRadioGroup();
        }
    }

    /**请求数据，然后刷新图标*/
    public void show(){
        setLoadingViewVisibilty(View.VISIBLE);
        showChart();
    }
}
