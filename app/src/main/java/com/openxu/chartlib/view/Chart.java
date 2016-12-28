package com.openxu.chartlib.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.bean.KLineParame;
import com.openxu.chartlib.utils.CommonUtil;
import com.openxu.chartlib.utils.TouchEventUtil;


/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : Chart
 * version : 1.0
 * class describe：图表控件基类
 */
public abstract class Chart extends View {

    protected String TAG;
    protected Context context = null;

//    protected int orientation;
    protected boolean isStop = false;

    protected float distanceX, distanceY;    //表格内网格线之间的高度
    protected float linewidth, lineheight;   //表格的宽度和高度

    protected KLineParame parame;   //通过计算得到的相关表格参数
    public int datasize;                 //数据总量
    protected int start;                 //图表左边开始绘制的坐标

    protected Paint gridlinePaint;   //图表网格画笔
    protected Paint DashPaint;       //虚线画笔


    protected TouchEventUtil touchEventUtil;   //控制焦点、拖拽事件的工具处理类

    public Chart(Context context) {
        super(context);
    }

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);
        TAG = getClass().getSimpleName();
       this.context = context;
        Constants.init(context);
        init();
    }

    private void init(){
        gridlinePaint = new Paint();
        gridlinePaint.setStyle(Paint.Style.STROKE);
        gridlinePaint.setAntiAlias(true);
        gridlinePaint.setColor(Color.LTGRAY);
        gridlinePaint.setStrokeWidth(Constants.gridlinewidth);

        DashPaint = new Paint();
        DashPaint.setColor(Color.LTGRAY);
        DashPaint.setStyle(Paint.Style.STROKE);

//		//float数组,必须是偶数长度,且>=2,指定了多少长度的实线之后再画多少长度的空白
        PathEffect effects = new DashPathEffect(new float[] {
                6.0f, 10.0f, 6.0f, 10.0f }, 1);
        DashPaint.setPathEffect(effects);
        DashPaint.setAntiAlias(true);
        DashPaint.setSubpixelText(true);
        DashPaint.setStrokeWidth(CommonUtil.dip2px(context,0.1f));

    }

    /**通过某点的坐标，确定图表中数据的索引*/
    public abstract int  getMoveIndex(PointF pointF);

    public static int limitMaxItemW = Constants.LimiteMaxScale;  //K线实体宽度最大值
    public static int limitMinItemW = Constants.LimiteMinScale;  //K线实体宽度最小值
    protected static int itemw = Constants.KLineItemW;           //K线实体宽度
    protected static int spacing = Constants.KLineItemS;         //k线实体之间的距离
    public static int ONEL;                                      //k线单个实体所占宽度（itemw+spacing）

    protected static int START;                         //K线图绘制的开始索引
    protected static int END;                           //K线图绘制的最后数据的索引




    public static void reset(){
        itemw= Constants.KLineItemW;
        spacing=Constants.KLineItemS;
        START =0;
        END =0;
        ONEL =0;
        limitMaxItemW = Constants.LimiteMaxScale;
        limitMinItemW=Constants.LimiteMinScale;
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (touchEventUtil != null) touchEventUtil.destroy();
    }



}
