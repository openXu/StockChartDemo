package com.openxu.chartlib.manager;

import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.openxu.chart.R;
import com.openxu.chartlib.StockChartManager;
import com.openxu.chartlib.bean.KLineParame;
import com.openxu.chartlib.bean.KLineTechParam;
import com.openxu.chartlib.bean.KLineType;
import com.openxu.chartlib.bean.KeyLineItem;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.request.KeylineRequest;
import com.openxu.chartlib.utils.TouchEventUtil;
import com.openxu.chartlib.view.ChartContainerView;

import java.util.ArrayList;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KeyLineManager
 * version : 1.0
 * class describe：K线管理类,主要是管理横屏和竖屏K线的不同处理
 */
public class KLineManager extends BaseChartManager{

    private KLineView viewManager;
    private KeylineRequest keylineRequest;
    private KLineParame parame;
    private ArrayList<KeyLineItem> datas = null;
    private boolean isStop = false;
    private KLineTechParam kLineTechParam = null;
    private StockChartManager.OnFocusChangeListener outListener=null;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.FocusCancelFlag:
                    viewManager.updateFocusView(true, null);
                    outListener.onfocusChange(true,datas.get(datas.size()-1),null);
                    break;
                case Constants.FocusChangeFlag:
                    KeyLineItem keyLineItem = (KeyLineItem) msg.obj;
                    viewManager.updateFocusView(false, keyLineItem);
                    outListener.onfocusChange(false,keyLineItem,null);
                    break;
                case Constants.ChartRefresh:            //刷新数据
                    setLoadingViewVisibilty(View.GONE);
//                    radioGroup.setEnabled(true);
//                    keyLineView.getLoadingView().setVisibility(View.GONE);
                    viewManager.updateChartView(parame, datas);
                    if(datas==null||datas.size()==0)return;
                    outListener.onfocusChange(true,datas.get(datas.size()-1),null);
                    break;
                case Constants.DragChangeFlag:        //
                    viewManager.drag((Float) msg.obj);
                    break;
                case Constants.DragRecetFlag:
                    viewManager.endData();
                    if((Float)msg.obj==Constants.EPSILON) Constants.isEnd = false;
                    break;
                case Constants.ScaleChartFlag:        //缩放
                    viewManager.setScale((Float) msg.obj, msg.arg1 == 0 ? true : false);
                    break;
            }
        }
    };


    public KLineManager(View rootView) {
        View layout_keyline = rootView.findViewById(R.id.layout_keyline);
        viewManager = new KLineView(this,rootView.getContext(), layout_keyline,
                foucsChangedListener, dragChangedListener, scalListener);
        viewManager.setkLineType(KLineType.VOL);
    }

    public void setOnTopLableChangeListener(StockChartManager.OnFocusChangeListener outlistener){
        this.outListener=outlistener;
    }


    /**请求数据的参数*/
    private String symbol;           //股票代码
    private String period = "daily"; //K线图频率（日、周、月）
    private String fq = "div";       //复权

    public void setParams(String symbol) {
        setParams(symbol, period, fq);
    }

    public void setParams(String symbol, String period, String fq) {
        this.symbol = symbol;
        this.period = period;
        this.fq = fq;
    }
    private K ktype;               //日k、周k、月k
    private FQ fqtype= FQ.qfq;
    public enum K {
        daily, week, month
    }
    //复权
    public enum FQ {
        fq, qfq, hfq
    }

    public void switchKlineType(K ktype) {
        switchKlineType(fqtype, ktype);
    }
    public void switchKlineType(FQ fqtype) {
        switchKlineType(fqtype, ktype);
    }
    public void switchKlineType(FQ fqtype, K ktype) {
        this.ktype = ktype;
        this.fqtype = fqtype;
        switch (fqtype) {
            case fq:
                fq = "div";
                break;
            case qfq:
                fq = "f";
                break;
            case hfq:
                fq = "af";
                break;
            default:
                fq = "div";
                break;
        }
        switch (ktype) {
            case daily:
                period = "daily";
                break;
            case week:
                period = "weekly";
                break;
            case month:
                period = "monthly";
                break;
//            case year:
//                period = "yearly";
//                break;
            default:
                period = "daily";
                break;
        }
        viewManager.stopFline();
    }

    /**显示、请求数据*/
    @Override
    public void showChart() {
        if (keylineRequest == null) {
            keylineRequest = new KeylineRequest(this);
            viewManager.setKeylineRequest(keylineRequest);
        }
        keylineRequest.getKlineData(viewManager.getKeyLineType(),symbol, period, fq);
    }
    @Override
    public void destroyRequest(){
        if(keylineRequest!=null)
            keylineRequest.destroy();
    }
    @Override
    protected View getLoadingView() {
        return viewManager.getLoadingView();
    }
    @Override
    public void setVisible(int visibilty) {
        viewManager.setVisibility(visibilty);
    }

    public void setkLineTechParam(KLineTechParam kLineTechParam) {
        this.kLineTechParam = kLineTechParam;
        viewManager.setkLineTechParam(kLineTechParam);
    }

    public void setData(ArrayList<KeyLineItem> datas, KLineParame parame, boolean isStop) {
        this.datas = datas;
        this.parame = parame;
        this.isStop = isStop;
        handler.sendEmptyMessage(Constants.ChartRefresh);
    }


    /**焦点监听*/
    private TouchEventUtil.OnFoucsChangedListener foucsChangedListener =
            new TouchEventUtil.OnFoucsChangedListener() {
                @Override
                public void foucsChanged(int tag, int index) {
                    if (tag == Constants.FocusCancelFlag) {
                        handler.sendEmptyMessage(Constants.FocusCancelFlag);
                    } else {
                        if (index >= datas.size() || index < 0) return;
                        KeyLineItem keyLineItem = datas.get(index);
                        keyLineItem.index = index;
                        Message msg = handler.obtainMessage(Constants.FocusChangeFlag);
                        msg.obj = keyLineItem;
                        handler.sendMessage(msg);
                    }
                }
            };

    /**拖拽监听*/
    private TouchEventUtil.OnDragChangedListener dragChangedListener =
            new TouchEventUtil.OnDragChangedListener() {
                @Override
                public void dragChanged(boolean dragable, float offset) {
                    Message msg;
                    if (dragable) {
                        msg = handler.obtainMessage(Constants.DragChangeFlag);
                    } else {
                        msg = handler.obtainMessage(Constants.DragRecetFlag);
                    }
                    msg.obj = offset;
                    handler.sendMessage(msg);

                }
            };

    /**缩放监听*/
    private ChartContainerView.OnScalListener scalListener =
            new ChartContainerView.OnScalListener() {
                @Override
                public void scaleChart(float scale, boolean isEnd) {
                    Message msg = handler.obtainMessage(Constants.ScaleChartFlag);
                    msg.obj = scale;
                    msg.arg1 = isEnd ? 0 : 1;
                    handler.sendMessage(msg);
                }
            };


}
