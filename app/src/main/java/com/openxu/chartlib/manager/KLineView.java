package com.openxu.chartlib.manager;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.openxu.chart.R;
import com.openxu.chartlib.bean.KLineParame;
import com.openxu.chartlib.bean.KLineTechParam;
import com.openxu.chartlib.bean.KLineType;
import com.openxu.chartlib.bean.KeyLineItem;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.request.KeylineRequest;
import com.openxu.chartlib.utils.TouchEventUtil;
import com.openxu.chartlib.view.Chart;
import com.openxu.chartlib.view.ChartContainerView;
import com.openxu.chartlib.view.FocusChart;
import com.openxu.chartlib.view.KeyLineChart;
import com.openxu.chartlib.view.TechnologyChart;

import java.util.List;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KeyLineView
 * version : 1.0
 * class describe： 横竖屏K线管理类的基类
 */
public class KLineView implements RadioGroup.OnCheckedChangeListener{

    protected KeyLineChart keyLineChart;            //K线图
    protected FocusChart focusView;                 //焦点图
    protected TechnologyChart technologyChart;      //技术指标
    protected Context context;
    protected KLineParame parame;
    protected View rootView;
    protected int orientation=0;
    private int offsetIndex=0;
    private int lastOffsetIndex;
    private View loadingView=null;
    protected List<KeyLineItem> datas=null;


    private LinearLayout avageLinear=null;
    //5日、10日、20日平均收盘价、日期
    private TextView avg5_tv,avg10_tv,avg20_tv,enddate_tv;
    private String avageformat="%1$s:%2$s";
    private RadioGroup furadio;      //复权
    private RadioGroup techradio;    //技术指标选择
    private ChartContainerView chartContainerView;    //控制缩放的容器
    private KeylineRequest keylineRequest;

    private KLineManager.FQ fqtype;
    private KLineManager keyLineManager=null;


    /**
     * k线数据初始化处理后的回调接口
     * 主要作用是将处理后的参数传递给底部技术曲线图
     */
    public interface KeyLineParameInitListener{
        void onPrameInit(KLineParame parame, boolean isDragging, int offset);
    }


    public KLineView(KLineManager keyLineManager, final Context context, View rootview,
                     TouchEventUtil.OnFoucsChangedListener foucsChangedListener,
                     TouchEventUtil.OnDragChangedListener dragChangedListener,
                     ChartContainerView.OnScalListener scalListener) {
        this.context = context;
        this.rootView = rootview;

        this.loadingView = rootview.findViewById(R.id.load_tv);
        this.keyLineChart = (KeyLineChart) rootview.findViewById(R.id.klinechart);
        this.technologyChart = (TechnologyChart) rootview.findViewById(R.id.techchart);
        this.focusView = (FocusChart) rootview.findViewById(R.id.focusview);
        this.keyLineManager = keyLineManager;
        this.rootView.setVisibility(View.GONE);
        this.chartContainerView = (ChartContainerView) rootview.findViewById(R.id.landkeylinelayout);
        furadio = (RadioGroup) rootview.findViewById(R.id.fq_group);
        techradio = (RadioGroup) rootview.findViewById(R.id.tech_group);

        furadio.setOnCheckedChangeListener(this);
        techradio.setOnCheckedChangeListener(this);

        //焦点监听
        keyLineChart.setOnFocusChangeListener(foucsChangedListener);
        technologyChart.setOnFocusChangeListener(foucsChangedListener);
        //拖动监听
        keyLineChart.setOnDragListener(dragChangedListener);
        technologyChart.setOnDragListener(dragChangedListener);
        //缩放监听
        chartContainerView.setScalListener(scalListener);

        keyLineChart.setEnable(true);
        technologyChart.setEnable(true);
        keyLineChart.setLongMoveEnable(true);
        keyLineChart.setCanDrag(true);
        technologyChart.setLongMoveEnable(true);
        technologyChart.setCanDrag(true);

        avg5_tv = (TextView) rootview.findViewById(R.id.keyline_m5_tv);
        avg10_tv = (TextView) rootview.findViewById(R.id.keyline_m10_tv);
        avg20_tv = (TextView) rootview.findViewById(R.id.keyline_m20_tv);
        enddate_tv = (TextView) rootview.findViewById(R.id.keyline_enddate_tv);
        avg5_tv.setTextColor(Constants.COLOR_A5);
        avg10_tv.setTextColor(Constants.COLOR_A10);
        avg20_tv.setTextColor(Constants.COLOR_A20);
        enddate_tv.setTextColor(Color.DKGRAY);

        /**当K线图中的某些变量发生变化后，技术指标图跟着变化*/
        keyLineChart.setKeyLineParameInitListener(new KeyLineParameInitListener() {
            @Override
            public void onPrameInit(KLineParame parame, boolean isDragging, int offset) {
                if (isDragging)
                    technologyChart.ondrag(parame, offset);
                else
                    technologyChart.setData(datas, parame);
            }
        });
    }

    public void setKeylineRequest(KeylineRequest keylineRequest){
        this.keylineRequest = keylineRequest;
    }

    protected int getStart() {
        return Constants.chartStart;
    }

    /**
     * 更新焦点线位置和数据
     * @param cancelFlag
     * @param keyLineItem
     */
    public void updateFocusView(boolean cancelFlag, KeyLineItem keyLineItem){
        synchronized (this){
        if(cancelFlag) {
            focusView.setCanceled(true);
            focusView.invalidate();
            updateFocus(keyLineItem,true);
        }else {
            focusView.setCanceled(false);
            if (!focusView.getFrameInit()) {
                focusView.setStart(getStart());
                focusView.setFrame((int) keyLineChart.getLinewidth(), keyLineChart.getHeight() + technologyChart.getHeight(), true);
            } else focusView.setIsLayout(false);
            focusView.update(keyLineItem);
            updateFocus(keyLineItem, false);
        }
        }
    }
    protected void updateFocus(KeyLineItem keyLineItem, boolean isEnd) {
        if(isEnd){
            updateAvageLable(keyLineChart.getLastItem());
            if(technologyChart!=null){
                technologyChart.updateDes(technologyChart.getLastItemIndex());
            }
        }else{
            updateAvageLable(keyLineItem);
            if(technologyChart!=null){
                technologyChart.updateDes(keyLineItem.index);
            }
        }
    }
    /**
     * 更新K线图数据展示
     * @param parame
     * @param data
     */
    public void updateChartView(KLineParame parame, List<KeyLineItem> data){
        this.parame = parame;
        this.datas = data;
        //重绘K线图
        keyLineChart.setData(data,parame);
        //设置最后一个K线实体的平均收盘价
        updateAvageLable(data==null||data.size()==0 ? null : data.get(data.size()-1));
    }

    /**
     * 设置是否停盘（这里是是否服务器返回的数据为空）
     * @param isStop
     */
    public void setIsStop(boolean isStop){
        keyLineChart.setIsStop(isStop);
        technologyChart.setIsStop(isStop);
    }


    public void setVisibility(int visibility){
        this.rootView.setVisibility(visibility);
    }

    public void setOrientation(int orientation) {
        this.orientation=orientation;
        focusView.setFrameinit(false);
        technologyChart.setKeyLineType(KLineType.VOL);
    }

    /**
     * 缩放行情图
     * @param scale
     * @param isEnd
     */
    public void setScale(float scale,boolean isEnd){
        synchronized (this) {
            keyLineChart.scaleChart(scale, isEnd);
        }
    }

    /**
     * 手指拖动
     * @param offset 偏移量
     */
    public void drag(float offset) {
        synchronized (this) {
            if (Constants.isEnd)
                return;
            //小于0，向右滑动
            if (offset < 0) {
                offsetIndex = -Math.round(Math.abs(offset) / Chart.ONEL);
            } else
                offsetIndex = Math.round(offset) / Chart.ONEL;
            if (offsetIndex == lastOffsetIndex || !keyLineChart.isCanOffset(offsetIndex))
                return;

            lastOffsetIndex = offsetIndex;

            keyLineChart.ondrag(offsetIndex);
            updateTopLable(keyLineChart.getLastItem());
        }
    }

    /**
     * 停止滚动
     */
    public void stopFline(){
        if(keyLineChart!=null)
        keyLineChart.stopFling();
    }

    /**
     * 结束滚动或者手指滑动
     */
    public void endData() {
        offsetIndex=0;
        lastOffsetIndex=0;
        Constants.isEnd = true;
        keyLineChart.endScaleData();
    }


    public View getLoadingView(){
        return this.loadingView;
    }

    protected void updateTopLable(KeyLineItem keyLineItem){}

    protected void updateAvageLable(KeyLineItem keyLineItem){
        if(keyLineItem==null)return;
        if(avageLinear==null){
            avageLinear = (LinearLayout) rootView.findViewById(R.id.avagelinear);
            avageLinear.setPadding(getStart(),0,0,0);
        }
        avg5_tv.setText(String.format(avageformat,"M5", keyLineItem.avg5==Float.NEGATIVE_INFINITY?"--": Constants.twoPointFormat.format(keyLineItem.avg5)));
        avg10_tv.setText(String.format(avageformat,"M10", keyLineItem.avg10==Float.NEGATIVE_INFINITY?"--": Constants.twoPointFormat.format(keyLineItem.avg10)));
        avg20_tv.setText(String.format(avageformat,"M20", keyLineItem.avg20==Float.NEGATIVE_INFINITY?"--": Constants.twoPointFormat.format(keyLineItem.avg20)));
        enddate_tv.setText(keyLineItem.date);
    }
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group.getId() == furadio.getId()) {
            if (checkedId == R.id.bfq_btn) {
                fqtype = KLineManager.FQ.fq;

            } else if (checkedId == R.id.qfq_btn) {
                fqtype = KLineManager.FQ.qfq;

            } else if (checkedId == R.id.hfq_btn) {
                fqtype = KLineManager.FQ.hfq;

            } else {
                fqtype = KLineManager.FQ.fq;

            }
            keyLineManager.switchKlineType(fqtype);
            keyLineManager.show();
        } else {
            if (checkedId == R.id.vol_btn) {
                keylineRequest.doTechData(KLineType.VOL);
                technologyChart.switchTech(KLineType.VOL);
            } else if (checkedId == R.id.macd_btn) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        keylineRequest.doTechData(KLineType.MACD);
                        handler.obtainMessage(0).sendToTarget();
                    }
                }).start();
//                technologyChart.switchTech(KLineType.MACD);
            } else if (checkedId == R.id.rsi_btn) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        keylineRequest.doTechData(KLineType.RSI);
                        handler.obtainMessage(1).sendToTarget();
                    }
                }).start();
//                technologyChart.switchTech(KLineType.RSI);

            } else if (checkedId == R.id.kdj_btn) {
//                technologyChart.switchTech(KLineType.KDJ);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        keylineRequest.doTechData(KLineType.KDJ);
                        handler.obtainMessage(2).sendToTarget();
                    }
                }).start();

            }
        }
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    technologyChart.switchTech(KLineType.MACD);
                    break;
                case 1:
                    technologyChart.switchTech(KLineType.RSI);
                    break;
                case 2:
                    technologyChart.switchTech(KLineType.KDJ);
                    break;
            }
        }
    };

    public void setkLineType(KLineType type){
        technologyChart.setKeyLineType(type);
    }
    public void setkLineTechParam(KLineTechParam kLineTechParam){
        technologyChart.setkLineTechParam(kLineTechParam);
    }
    public KeyLineChart getKeyLineChart(){
        return this.keyLineChart;
    }

    public KLineType getKeyLineType(){
        return technologyChart.getkLineType();
    }
}
