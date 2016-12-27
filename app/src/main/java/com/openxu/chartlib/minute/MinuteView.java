package com.openxu.chartlib.minute;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.openxu.chart.R;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.minute.bean.MinuteParame;
import com.openxu.chartlib.minute.bean.MinutesBean;
import com.openxu.chartlib.minute.bean.PankouData;
import com.openxu.chartlib.minute.chart.FocusChart;
import com.openxu.chartlib.minute.chart.MinuteBarChart;
import com.openxu.chartlib.minute.chart.MinuteHourChart;
import com.openxu.chartlib.utils.CommonUtil;
import com.openxu.chartlib.utils.TouchEventUtil;

import java.util.List;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : MinuteView
 * version : 1.0
 * class describe：分时图各部件组织类
 */
public class MinuteView {

    protected View rootView;
    protected Context context;

    private View loadingView;
    private TextView tv_junxian, tv_shijian, tv_fenshi;
    private LinearLayout fenshilinear;

    protected MinuteHourChart minuteHourView;   //分时图控件
    protected MinuteBarChart barView;           //底部状态图控件
    protected FocusChart focusView;             //焦点图
    protected LinearLayout pankoulinear;        //盘口数据容器
    private View pankouline;

    protected MinuteParame parame;

    public boolean isZhishu=false;

    public int getStart() {
        return Constants.chartStart;
    }

    public MinuteView(final Context context, View rootview,
                      TouchEventUtil.OnFoucsChangedListener foucsChangedListener){
        this.rootView = rootview;
        this.context = context;
        this.loadingView =rootview.findViewById(R.id.load_tv);
        minuteHourView = (MinuteHourChart) rootview.findViewById(R.id.minutechart);
        pankoulinear = (LinearLayout) rootview.findViewById(R.id.pankoulinear);
        barView = (MinuteBarChart) rootview.findViewById(R.id.barchart);
        focusView = (FocusChart)rootview.findViewById(R.id.focuschart);
        minuteHourView.setOnFocusChangeListener(foucsChangedListener);
        barView.setOnFocusChangeListener(foucsChangedListener);
        pankouline = initPankouline();

        tv_junxian = (TextView) rootview.findViewById(R.id.tv_junxian);
        tv_shijian = (TextView) rootview.findViewById(R.id.tv_shijian);
        tv_fenshi = (TextView) rootview.findViewById(R.id.tv_fenshi);

        minuteHourView.setEnable(true);
        minuteHourView.setLongMoveEnable(false);
        minuteHourView.setIsMinute(true);
        barView.setIsMinute(true);
    }
    private View initPankouline(){
        FrameLayout frameLayout = new FrameLayout(context);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,10);
        lp.gravity=Gravity.CENTER;
        frameLayout.setLayoutParams(lp);
//        frameLayout.setPadding(0,2,0,2);
//        TextView line =new TextView(context);
//        line.setBackgroundColor(Constants.gridlineColor);
//        line.setLayoutParams(new FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,1));
//        frameLayout.addView(line);
        return frameLayout;
    }

    /**
     * 绑定盘口数据
     */
    public void updatePankouData(PankouData.Data pankouData, float prePrice){
        pankoulinear.setVisibility(View.VISIBLE);
        pankoulinear.removeAllViews();
        View view = null;
        if (pankouData == null) pankouData = new PankouData.Data();
        for (int i = 0; i < 10; i++) {
            switch (i) {
                case 0:
                    view = bondPankouData("卖" + 5,
                            Constants.twoPointFormat.format(pankouData.getS(4)),
                            pankouData.getSn(4)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getS(4)),
                            Constants.C_LABLE_TEXT);
                    break;
                case 1:
                    view = bondPankouData("卖" + 4,
                            Constants.twoPointFormat.format(pankouData.getS(3)),
                            pankouData.getSn(3)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getS(3)),
                            Constants.C_LABLE_TEXT);
                    break;
                case 2:
                    view = bondPankouData("卖" + 3,
                            Constants.twoPointFormat.format(pankouData.getS(2)),
                            pankouData.getSn(2)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getS(2)),
                            Constants.C_LABLE_TEXT);
                    break;
                case 3:
                    view = bondPankouData("卖" + 2,
                            Constants.twoPointFormat.format(pankouData.getS(1)),
                            pankouData.getSn(1)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getS(1)),
                            Constants.C_LABLE_TEXT);
                    break;
                case 4:
                    view = bondPankouData("卖" + 1,
                            Constants.twoPointFormat.format(pankouData.getS(0)),
                            pankouData.getSn(0)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getS(0)),
                            Constants.C_LABLE_TEXT);
                    break;
                case 5:
                    pankoulinear.addView(pankouline);

                    view = bondPankouData("买" + 1,
                            Constants.twoPointFormat.format(pankouData.getB(0)),
                            pankouData.getBn(0)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getB(0)),
                            Constants.C_LABLE_TEXT);

                    break;
                case 6:
                    view = bondPankouData("买" + 2,
                            Constants.twoPointFormat.format(pankouData.getB(1)),
                            pankouData.getBn(1)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getB(1)),
                            Constants.C_LABLE_TEXT);
                    break;
                case 7:
                    view = bondPankouData("买" + 3,
                            Constants.twoPointFormat.format(pankouData.getB(2)),
                            pankouData.getBn(2)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getB(2)),
                            Constants.C_LABLE_TEXT);
                    break;
                case 8:
                    view = bondPankouData("买" + 4,
                            Constants.twoPointFormat.format(pankouData.getB(3)),
                            pankouData.getBn(3)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getB(3)),
                            Constants.C_LABLE_TEXT);
                    break;
                case 9:
                    view = bondPankouData("买" + 5,
                            Constants.twoPointFormat.format(pankouData.getB(4)),
                            pankouData.getBn(4)/100 + "",
                            Constants.C_LABLE_TEXT,
                            getColorPankou(prePrice,pankouData.getB(4)),
                            Constants.C_LABLE_TEXT);
                    break;
            }
            view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,0,1));
            pankoulinear.addView(view);
        }
    }

    private View bondPankouData(String lable1, String lable2, String lable3, int c1, int c2, int c3){
        View view = LayoutInflater.from(context).inflate(R.layout.bug_sell_item, null);
        ((TextView) view.findViewById(R.id.label1)).setText(lable1);
        ((TextView) view.findViewById(R.id.label2)).setText(lable2);
        ((TextView) view.findViewById(R.id.label3)).setText(lable3);
        ((TextView) view.findViewById(R.id.label1)).setTextColor(c1);
        ((TextView) view.findViewById(R.id.label2)).setTextColor(c2);
        ((TextView) view.findViewById(R.id.label3)).setTextColor(c3);
        return view;
    }
    private int getColorPankou(float preprice,float price){
        if(price==0)
            return Constants.C_LABLE_TEXT;
        if(price > preprice)return Constants.C_RED_TEXT;
        if(price < preprice)return Constants.C_GREEN_TEXT;
        return Constants.C_LABLE_TEXT;
    }
    public void updateFocus(MinutesBean minutesBean, boolean isEnd) {
        updateTopLable(minutesBean);
    }

    public void updateFocusView(boolean cancelflag,MinutesBean minutesBean){
        if(cancelflag) {
            focusView.setCanceled(true);
            focusView.invalidate();
            updateFocus(minutesBean,true);
        }else{
            focusView.setCanceled(false);
            if(!focusView.getFrameInit()) {
                focusView.setStart(getStart());
                focusView.setFrame((int) minuteHourView.getLinewidth(), minuteHourView.getHeight() + barView.getHeight(),true);
            }else focusView.setIsLayout(false);
            focusView.update(minutesBean);
            updateFocus(minutesBean,false);
        }
    }

    public View getLoadingView(){
        return this.loadingView;
    }

    /**
     * 更新分时图表，当请求到分时图数据后，有MinuteManager中调用此方法
     * @param parame
     * @param data
     */
    public void updateChartView(MinuteParame parame, List<MinutesBean> data) {
        //重绘分时图
        minuteHourView.setData(data,parame);
        this.parame = parame;
        //重绘状态图
        barView.setData(data,parame);
        //更新分时图顶部价格和时间数据（默认最后一个数据，也就是最新的数据信息）
        updateTopLable(data==null||data.size()==0 ? null : data.get(data.size()-1));
    }

    public void setPrePrice(float preprice) {
//        this.minuteHourView.setPrePrice(preprice);
        this.barView.setPrePrice(preprice);
    }

    public void setIsZhishu(boolean isZhishu){
        this.isZhishu=isZhishu;
        pankoulinear.setVisibility(isZhishu?View.GONE:View.VISIBLE);
    }

    public void setIsStop(boolean isStop) {
        minuteHourView.setIsStop(isStop);
        barView.setIsStop(isStop);
    }

    public void setVisibility(int visibility){
        rootView.setVisibility(visibility);
    }

    public void setOrientation(int orientation) {
        focusView.setFrameinit(false);
    }

    protected void updateTopLable(MinutesBean minutesBean){
        if(minutesBean==null)return;
        if (fenshilinear == null) {
            fenshilinear = (LinearLayout) rootView.findViewById(R.id.fenshilinear);
            if(!isZhishu){
                fenshilinear.setPadding(getStart(), 0, CommonUtil.dip2px(context,116), Constants.S_LABLE_CHART_DIS);
            }else{
                fenshilinear.setPadding(getStart(), 0, 0, Constants.S_LABLE_CHART_DIS);
            }
        }
        tv_fenshi.setText("分时: " + (minutesBean.cjprice==Constants.EPSILON ? "--" : Constants.twoPointFormat.format(minutesBean.cjprice)));
        tv_fenshi.setTextColor(Constants.C_M_DATA_LINE);
        tv_junxian.setText("均线: " + (minutesBean.avprice==Float.NaN ? "--" : Constants.twoPointFormat.format(minutesBean.avprice)));
        tv_junxian.setTextColor(Constants.C_M_AVG_LINE);
        tv_shijian.setText(minutesBean.time);
        tv_shijian.setTextColor(Constants.C_LABLE_TEXT);
    }


    public MinuteBarChart getMinuteBarChart(){
        return this.barView;
    }
    public MinuteHourChart getMinuteHourChart(){
        return this.minuteHourView;
    }
    public LinearLayout getPankoulinear(){
        return this.pankoulinear;
    }
}
