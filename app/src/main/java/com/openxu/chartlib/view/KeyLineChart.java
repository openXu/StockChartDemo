package com.openxu.chartlib.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.bean.KLineParame;
import com.openxu.chartlib.bean.KeyLineItem;
import com.openxu.chartlib.utils.CommonUtil;
import com.openxu.chartlib.utils.GlFontUtil;
import com.openxu.chartlib.utils.LogUtil;
import com.openxu.chartlib.utils.TouchEventUtil;

import java.util.List;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KeyLineChart
 * version : 1.0
 * class describe：K线图表控件
 */
public class KeyLineChart extends Chart {

    //K线图数据集合
    private List<KeyLineItem> keyLineItems;

    //K线参数初始化完毕后回调
    private KeyLineView.KeyLineParameInitListener keyLineParameInitListener;

    private boolean initFinished = false;

    private int number;                  //当前k线图上绘制的k线实体的数量
    private int indexStart, indexEnd;    //K线图展示的开始位置和结束位置（移动之后的索引）


    private Paint falllinePaint;     //用于绘制跌的情况的影线
    private Paint raiselinePaint;    //用于绘制涨的情况的影线

    private Paint avgPaint;
    private int offset;
    private boolean isDraging = false;    //是否正在拖动
    private int lastItemW;                //记录缩放结束（手指抬起）后K线实体的宽度

    public KeyLineChart(Context context) {
        this(context, null);
    }

    public KeyLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }
    private void init() {
        touchEventUtil = new TouchEventUtil(context, this);

        //实体宽度最大值最小值
        if (limitMinItemW == Constants.LimiteMinScale) {
            limitMinItemW = CommonUtil.dip2px(context,limitMinItemW);
            limitMaxItemW = CommonUtil.dip2px(context,limitMaxItemW);
        }

        //K线图实体之间的左右间距
        spacing = CommonUtil.dip2px(context, Constants.KLineItemS);
        //实体宽度
        itemw = CommonUtil.dip2px(context, Constants.KLineItemW);
        ONEL = spacing + itemw;
        lastItemW = itemw;

        falllinePaint = new Paint();
        falllinePaint.setAntiAlias(true);
        falllinePaint.setColor(Constants.C_GREEN_PILLAR);
        falllinePaint.setStrokeWidth(CommonUtil.dip2px(context, 0.5f));

        raiselinePaint = new Paint();
        raiselinePaint.setAntiAlias(true);
        raiselinePaint.setColor(Constants.C_RED_PILLAR);
        raiselinePaint.setStrokeWidth(CommonUtil.dip2px(context, 0.5f));
        Constants.labelPaint.setColor(Color.DKGRAY);

        avgPaint = new Paint();
        avgPaint.setAntiAlias(true);
        avgPaint.setStrokeWidth(Constants.pathlinewidth);
        avgPaint.setStyle(Paint.Style.STROKE);
        avgPaint.setStrokeCap(Paint.Cap.ROUND);
        avgPaint.setStrokeJoin(Paint.Join.ROUND);
    }
    /**通过某点的坐标，确定图表中数据的索引*/
    @Override
    public int getMoveIndex(PointF pointF) {
        synchronized (this) {
            int index = indexEnd - Math.round((keyLineItems.get(indexEnd).pointF.x - pointF.x) / ONEL);
            if (index < indexStart)
                index = indexStart;
            if (index > indexEnd) index = indexEnd;
            return index;
        }
    }


    public void setKeyLineParameInitListener(KeyLineView.KeyLineParameInitListener keyLineParameInitListener) {
        this.keyLineParameInitListener = keyLineParameInitListener;
    }


    public void setIsStop(boolean isStop) {
        this.isStop = isStop;
    }

    public void stopFling(){
        if(touchEventUtil!=null)
        touchEventUtil.stopFling();
    }

    /***********************事件处理**********************/
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean b = super.dispatchTouchEvent(event);
        if (!isDrawFinished)
            return true;
        if (!isEnable)
            return b;
        if (touchEventUtil == null)
            touchEventUtil = new TouchEventUtil(context, this);
        return touchEventUtil.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrawFinished)
            return true;
        if (!isEnable)
            return super.onTouchEvent(event);
        if(datasize==0)
            return true;
        return touchEventUtil.onTouchEvent(event);
    }
    public void setOnFocusChangeListener(TouchEventUtil.OnFoucsChangedListener listener) {
        if (touchEventUtil == null)
            touchEventUtil = new TouchEventUtil(context, this);
        touchEventUtil.setFoucsChangedListener(listener);
    }

    public void setOnDragListener(TouchEventUtil.OnDragChangedListener listener) {
        if (touchEventUtil == null)
            touchEventUtil = new TouchEventUtil(context, this);
        touchEventUtil.setDragChangeListener(listener);
    }
    public void setLongMoveEnable(boolean moveEnable){
        touchEventUtil.setLongMoveEnable(moveEnable);
    }
    public void setCanDrag(boolean canDrag){
        touchEventUtil.setCanDrag(canDrag);
    }


    /********************拖拽相关***********************/
    /**根据当前展示数据的位置判断是否能拖动，如果展示的数据已经到最后或者第一个数据则不能拖动*/
    public boolean isCanOffset(float offsetindex){
        if(offsetindex==0)
            return false;
        if(offsetindex>0 && indexEnd==datasize-1)
            return false;
        if(offsetindex<0 && indexStart==0)
            return false;
        return true;
    }
    /**拖拽*/
    public void ondrag(int offsetindex) {
        if(!initFinished)
            return;
        if (offset == offsetindex) {
            return;
        }
        isDraging = true;
        offset = offsetindex;

        initStartAndEnd(offsetindex);
        invalidate();
    }
    /**获取滑动后偏移量之后的数据端索引*/
    private void initStartAndEnd(int offsetindex) {
        indexStart = START + offsetindex;
        indexEnd = END + offsetindex;
        if (indexStart < 0) {
            indexStart = 0;
            indexEnd = number - 1;
        }
        if (indexEnd >= datasize) {
            indexEnd = datasize - 1;
            indexStart = indexEnd - number + 1;
        }
    }



    /******************缩放相关start*******************/
    /**多点触碰缩放*/
    public void scaleChart(float scale,boolean isEnd) {
        LogUtil.w(TAG, "K线图缩放scale="+scale+"  isEnd="+isEnd);
        if (isEnd) {
            endScaleData();
        } else {
            if (scale == 1 || scale == 0)
                return;
            if (scale > 1)
                itemw = (int) Math.ceil(lastItemW * scale);   //返回大于参数的最小整数,即对浮点数向上取整
            else
                itemw = (int) Math.floor(lastItemW * scale);  //返回小于参数x的最大整数,即对浮点数向下取整
            if (itemw == lastItemW)
                return;
            LogUtil.i(TAG, "初始实体宽度lastItemW="+lastItemW+"缩放后实体宽度itemw="+itemw);
            //缩放不能超过范围
            if (itemw < limitMinItemW)
                itemw = limitMinItemW;
            if (itemw > limitMaxItemW)
                itemw = limitMaxItemW;
            //单个实体需要占用的宽度
            ONEL = spacing + itemw;
            computerNumberAfterScale();
//            computVars();
            invalidate();
        }
    }
    /**缩放后重新计算相关变量值*/
    private void computerNumberAfterScale(){
        number =  (getWidth() - start) / ONEL;
        if (number > datasize) {
            START = 0;
            number = datasize;
            END = datasize - 1;
        } else if(START==0){
            END = number-1;
        }else{
            START = indexEnd - number+1;
            END = indexEnd;
        }
        if(START<0)
            START=0;
        if(END>=datasize)
            END = datasize-1;
        parame.number = number;
        indexStart = START;
        indexEnd = END;
    }

    /**缩放完毕后重置数据*/
    public void endScaleData() {
        offset=0;
        START = indexStart;
        END = indexEnd;
        isDraging = false;
        lastItemW = itemw;
        if(START==0)
            Constants.IsFirst=true;
        if(END==datasize-1)
            Constants.IsLast=true;
    }

    /******************缩放相关end*******************/

    /**
     * 设置数据
     * @param keyLineItems
     * @param parame
     */
    public void setData(List<KeyLineItem> keyLineItems, KLineParame parame) {
        LogUtil.v(TAG, "重绘K线图："+keyLineItems);
        this.keyLineItems = keyLineItems;
        this.parame = parame;
        datasize = keyLineItems == null ? 0 : keyLineItems.size();
        initFinished = false;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int specMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int specSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (specMode == View.MeasureSpec.EXACTLY || specMode == View.MeasureSpec.AT_MOST) {
            int height;
            height = Math.min(Constants.defaultChartHeight, specSize);
            setMeasuredDimension(widthMeasureSpec, height);
        } else
            setMeasuredDimension(widthMeasureSpec, Constants.defaultChartHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        datasize=keyLineItems==null ? 0 : keyLineItems.size();
        isDrawFinished = false;

        computVars();
        drawGridLine(canvas);
        if (datasize == 0) {
            isDrawFinished=true;
            return;
        }
        drawLabelY(canvas);
        drawLabelX(canvas);
        drawKData(canvas);
        isDrawFinished = true;
    }

    private void computVars() {
        //每次请求到数据后，重新计算绘制表格相关的变量
        if (!initFinished) {
            //y刻度显示在外面
            start = Constants.chartStart;
            //图表的宽度
            linewidth = getWidth() - start;
            //图表的高度 = 总高度 - x刻度高度 - 字体间距
            lineheight = getHeight() - GlFontUtil.getFontHeight(Constants.labelPaint)
                    - Constants.S_LABLE_CHART_DIS*2;
            distanceX = linewidth / 4;
            distanceY = lineheight / 4;

            computeNumber();

            initFinished = true;
        }
        if(datasize>0) {
            restroreState();
            parame.maxPrice = 0;
            parame.minPrice = Float.MAX_VALUE;
            KeyLineItem item;
            for (int i = indexStart; i <= indexEnd; i++) {
                item = keyLineItems.get(i);
                parame.maxPrice = Math.max(getMax(item), parame.maxPrice);
                parame.minPrice = Math.min(getMin(item), parame.minPrice);

            }
            parame.maxpriceLenth = GlFontUtil.getFontlength(
                    Constants.labelPaint, Constants.twoPointFormat.format(parame.maxPrice));
        }

        //通知指标图参数变化了
        if (keyLineParameInitListener != null) {
            keyLineParameInitListener.onPrameInit(parame, isDraging, offset);
        }
    }

    /**获取数据后第一次计算、初始化变量*/
    private void computeNumber() {
        if(datasize==0)
            return;
        number = (getWidth() - start) / ONEL;
        LogUtil.v(TAG, "图表总共能画number个k线实体："+number+"   总共有datasize个数据："+datasize);
        if (number > datasize) {
            //如果数据个数不能填满表格，那从第一个数据开始，到最后一个数据结束
            START = 0;
            number = datasize;
            END = datasize - 1;
        } else {
            //如果数据非常多，那么绘制最新时间的K线
            START = datasize - number;
            END = datasize - 1;
        }
        parame.number = number;
        //开始默认偏移量为0
        initStartAndEnd(0);
    }


    /**1、绘制图表格子线*/
    private void drawGridLine(Canvas canvas) {
        float x, y;
        Path path = new Path();
        //绘制y轴方向格线
        for (int i = 0; i < 5; i++) {
            path.reset();
            x = start + i * distanceX;
            switch (i){
                case 0:
                    gridlinePaint.setColor(Constants.C_AROUND_LINE);
                    canvas.drawLine(x, 0, x, lineheight, gridlinePaint);
                    break;
                case 4:
                    gridlinePaint.setColor(Constants.C_AROUND_LINE);
                    canvas.drawLine(x-Constants.gridlinewidth/2, 0, x-Constants.gridlinewidth/2, lineheight, gridlinePaint);
                    break;
                default:
                    gridlinePaint.setColor(Constants.C_GRID_LINE);
                    path.moveTo(x, 0);
                    path.lineTo(x, lineheight);
                    canvas.drawPath(path, gridlinePaint);
                    break;

            }
        }

        //绘制x轴方向格线
        for (int j = 0; j < 5; j++) {
            path.reset();
            y = distanceY * j;
            switch (j) {
                case 0:
                    gridlinePaint.setColor(Constants.C_AROUND_LINE);
                    canvas.drawLine(start, y+Constants.gridlinewidth/2, linewidth + start, y+Constants.gridlinewidth/2, gridlinePaint);
                    break;
                case 4:
                    gridlinePaint.setColor(Constants.C_AROUND_LINE);
                    canvas.drawLine(start, y, linewidth + start, y, gridlinePaint);
                    break;
                case 2:
                    DashPaint.setStrokeWidth(2f);
                    path.moveTo(start, y);
                    path.lineTo(linewidth + start, y);
                    canvas.drawPath(path, DashPaint);
                    break;
                default:
                    gridlinePaint.setColor(Constants.C_GRID_LINE);
                    path.moveTo(start, y);
                    path.lineTo(linewidth + start, y);
                    canvas.drawPath(path, gridlinePaint);
                    break;
            }
        }
    }

    /**2、绘制Y轴刻度（价格）*/
    private void drawLabelY(Canvas canvas) {
        Constants.labelPaint.setColor(Constants.C_LABLE_TEXT);
        //所有实体数据中最高价的最大值
        String max = Constants.twoPointFormat.format(parame.maxPrice);
        //根据最高价字体大小获取在表格中的坐标
        PointF point = getYPointF(Constants.labelPaint, 0,
                GlFontUtil.getFontlength(Constants.labelPaint, max));
        canvas.drawText(max, point.x, point.y, Constants.labelPaint);
        //所有实体数据中最低价的最小值
        String min = Constants.twoPointFormat.format(parame.minPrice);
        point = getYPointF(Constants.labelPaint, 1,
                GlFontUtil.getFontlength(Constants.labelPaint, min));
        canvas.drawText(Constants.twoPointFormat.format(parame.minPrice),
                point.x, point.y, Constants.labelPaint);
    }

    /**2.1获取Y轴价格刻度在控件中绘制的坐标*/
    private PointF getYPointF(Paint paint, int p, float fontlenth) {
        //获取lable的baseline
        float leading = GlFontUtil.getFontLeading(paint);
        float h = GlFontUtil.getFontHeight(paint);
        //X坐标：如果表格与左边没有留空间，就贴着表格写在里面；否则=控件-字宽度-间距
        float x = start == 0 ? 0 : start - fontlenth - Constants.S_LABLE_CHART_DIS;
        if (p == 1) {
            //最低价
            return new PointF(start == 0 ? 0 : x, lineheight - h + leading);
        } else if (p == 0) {
            //最高价
            return new PointF(start == 0 ? 0 : x, leading);
        }

        return new PointF();
    }

    /**3、绘制X轴刻度（时间）*/
    private void drawLabelX(Canvas canvas) {
        Constants.labelPaint.setColor(Constants.C_LABLE_TEXT);
        //X轴上能画多少个实体
        int numberW = (getWidth() - start) / ONEL;

        String datelabel;
        KeyLineItem item;
        PointF p;

        //根据获取到的数据个数以及X轴能容纳的实体个数，计算X轴时间刻度个数，最多为5个时间刻度
        int l;
        if(datasize<=10){
            l=1;
        }else if(datasize<numberW){
           l = datasize/10;
            if(l>5)l=5;
        }else l=5;

        //获取第一个时间对应的实体数据的索引
        int index = getLableXIndex(0,l);
        //第一个时间
        String lastdate = keyLineItems.get(index).date;
        for (int i = 0; i < l; i++) {
            switch (i) {
                case 0:
                    //2015/10/01
                    datelabel = lastdate.replaceAll("-", "/");
                    break;
                default:
                    //获取第i个事件对应的数据的索引
                    index = getLableXIndex(i,l);
                    item = keyLineItems.get(index);
                    if (lastdate.substring(0, 4).equals(item.date.substring(0, 4))) {
                        //其他的时间刻度，如果和前面的年份相同，就只展示10/10(月日)
                        datelabel = item.date.substring(5, 10).replace("-", "/");
                    } else {
                        //如果年份和前面的刻度不同，则需要展示新的年2016/01/10
                        datelabel = item.date.replaceAll("-", "/");
                    }
                    lastdate = item.date;
                    break;
            }
            //根据时间刻度lable获取在控件中的绘制坐标
            p = getXPointF(Constants.labelPaint, datelabel, i,l);
            //绘制时间
            canvas.drawText(datelabel, p.x, p.y, Constants.labelPaint);
        }
    }

    /**3.1根据X周坐标的索引 获取 数据集合的索引*/
    private int getLableXIndex(int i,int l) {
        switch (i){
            case 0:   //第一个刻度
                return indexStart;   //开始绘制的位置索引
            case 4:   //第五个刻度
                //如果数据个数正好==控件能容纳实体的个数，最后一个时间就是最后一个数据的时间
                if(datasize==number)
                    return datasize-1;
                return indexEnd;
            default:  //中间刻度
                if(datasize==number){
                    if(i==l-1){
                        return datasize-1;
                    }
                    return (int) Math.ceil(i*1.0f/(l-1)*number);
                }
                float d = (number * 1.0f) / 4;
                return (int) Math.ceil(indexStart + i * d);
        }
    }

    /**3.2获取X轴时间刻度在控件中绘制的坐标*/
    private PointF getXPointF(Paint paint, String datestr, int p,int l) {
        float w = GlFontUtil.getFontlength(paint, datestr);
        float h = GlFontUtil.getFontLeading(paint);
        float y = lineheight + h + Constants.S_LABLE_CHART_DIS;
        if (p == 0) {
            return new PointF(start, y);
        } else {
            if(p==4){
                if(datasize==number){
                    return new PointF(getKDataX(datasize-1)+itemw/2-w/2, y);
                }
                return new PointF(start + linewidth - w, y);
            }
            if(datasize==number){
                if(p==l-1)
                return new PointF(getKDataX(datasize-1)+itemw/2-w/2,y);
                int index = (int) Math.ceil(p*1.0f/(l-1)*number);
                return new PointF(getKDataX(index)+itemw/2-w/2,y);
            }
            return new PointF(start + p * distanceX - w / 2, y);
        }
//        return new PointF(start + p * distanceX - w / 2, lineheight + h);
    }

    /**4、绘制K线实体*/
    private void drawKData(Canvas canvas) {
        int index = 0;
        float lastX, centerX, openY, closeY;
        Path path5 = new Path();
        Path path10 = new Path();
        Path path20 = new Path();

        boolean av5moved=false,av10moved=false,av20moved=false;

        for (int i = indexStart; i <= indexEnd; i++) {
            //①、计算坐标
            KeyLineItem item = keyLineItems.get(i);
            lastX = getKDataX(index);         //实体X轴坐标
            openY = getKDataY(item.open);     //开盘价Y轴坐标
            closeY = getKDataY(item.close);   //收盘价Y轴坐标
            if(item.pointF==null)
                item.pointF=new PointF();
            item.pointF.x = lastX + itemw / 2;   //k线实体中点X坐标
            item.pointF.y = closeY;
            //②、绘制实体及影线
            if (item.isFall) {  //true跌
                if(item.isFallStop){
                    //跌停，画实体
                    canvas.drawRect(lastX, openY, lastX + itemw, closeY+1, Constants.fallPaint);
                }else {
                    //非跌停，画实体
                    canvas.drawRect(lastX, openY, lastX + itemw, closeY, Constants.fallPaint);
                    //画影线
                    canvas.drawLine(item.pointF.x, getKDataY(item.high),
                            item.pointF.x, getKDataY(item.low), falllinePaint);
                }
            } else {
                if(item.isRaiseStop){
                    //涨停
                    canvas.drawRect(lastX, closeY-1, lastX + itemw, openY, Constants.raisePaint);
                }else {
                    //非涨停
                    canvas.drawRect(lastX, closeY, lastX + itemw, openY, Constants.raisePaint);
                    canvas.drawLine(item.pointF.x, getKDataY(item.high),
                            item.pointF.x, getKDataY(item.low), raiselinePaint);
                }
            }
            //③、绘制均线
            if (item.avg5 > Float.NEGATIVE_INFINITY) {
                if(av5moved)
                    path5.lineTo(lastX, getKDataY(item.avg5));
                else {
                    path5.moveTo(lastX, getKDataY(item.avg5));
                    av5moved=true;
                }
            }
            if (item.avg10 > Float.NEGATIVE_INFINITY) {
                if(av10moved)
                    path10.lineTo(lastX, getKDataY(item.avg10));
                else{
                    path10.moveTo(lastX, getKDataY(item.avg10));
                    av10moved=true;
                }
            }
            if (item.avg20 > Float.NEGATIVE_INFINITY) {
                if(av20moved)
                    path20.lineTo(lastX, getKDataY(item.avg20));
                else{
                    path20.moveTo(lastX, getKDataY(item.avg20));
                    av20moved=true;
                }
            }
            index++;
        }
        //5日均线（5日平均收盘价）
        avgPaint.setColor(Constants.COLOR_A5);
        canvas.drawPath(path5, avgPaint);
        //10日均线
        avgPaint.setColor(Constants.COLOR_A10);
        canvas.drawPath(path10, avgPaint);
        //20日均线
        avgPaint.setColor(Constants.COLOR_A20);
        canvas.drawPath(path20, avgPaint);
    }

    /**4.1获取K线实体X轴的坐标*/
    private float getKDataX(int index) {
        return start+index * ONEL;
    }
    /**4.2获取K线实体四个价格的Y轴的坐标*/
    private float getKDataY(float close) {
        return lineheight * (parame.maxPrice - close) / (parame.maxPrice - parame.minPrice);
    }


    private float getMax(KeyLineItem item) {
        float maxc = item.high;
        maxc = Math.max(maxc, item.avg5);
        maxc = Math.max(maxc, item.avg10);
        maxc = Math.max(maxc, item.avg20);
        return maxc;
    }

    private float getMin(KeyLineItem item) {
        float minc = item.low;
        if (item.avg5 > 0) {
            minc = Math.min(minc, item.avg5);
        }
        if (item.avg10 > 0) {
            minc = Math.min(minc, item.avg10);
        }

        if (item.avg20 > 0) {
            minc = Math.min(minc, item.avg20);
        }
        return minc;
    }

    public float getLinewidth() {
        return linewidth;
    }

    public float getLineHeight() {
        return lineheight;
    }



    private boolean isDrawFinished = false;
    private boolean isEnable = true;

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    public KeyLineItem getLastItem() {
        return keyLineItems.get(indexEnd);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }
    Bundle bundle=null;


    /********************意外退出的情况保存和获取当前状态数据********************/
    public void saveInstanceState(){
        bundle=new Bundle();
        bundle.putInt("IndexStart",indexStart);
        bundle.putInt("IndexEnd",indexEnd);
        bundle.putInt("itemw",itemw);
    }
    public void restroreState(){
        if(bundle==null)
            return;
        indexStart = bundle.getInt("IndexStart");
        indexEnd = bundle.getInt("IndexEnd");
        itemw = bundle.getInt("itemw");
        ONEL = spacing + itemw;
        computerNumberAfterScale();
        bundle=null;
    }
}
