package com.openxu.chartlib.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.bean.KLineParame;
import com.openxu.chartlib.bean.KLineTechItem;
import com.openxu.chartlib.bean.KLineTechParam;
import com.openxu.chartlib.bean.KLineType;
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
 * class name : TechnologyChart
 * version : 1.0
 * class describe：K线底部技术曲线图
 */
public class TechnologyChart extends Chart {

    //RectF(123.0, 0.0, 1692.0, 180.0)
    private RectF rectF = new RectF();

    /**K线实体集合*/
    private List<KeyLineItem> keyLineItems;
    /**指标数据集合*/
    private KLineTechParam kLineTechParam;
    //实体个数
    public int datasize;
    //当前技术指标类型
    private KLineType kLineType = KLineType.VOL;

    //当前展示的数据量
    private int number;
    //当前展示的数据的开始索引和结束索引
    private int indexStart, indexEnd;
    //最后一个数据的索引
    private int desIndex;

    //VOL成交量指标最大值
    private long maxValue;
    private String[] leftLable = new String[]{"0", ""};
    //MACD指标最大值和最小值
    private float maxMACD=Float.MIN_VALUE, minMACD = Float.MAX_VALUE;
    //KDJ指标最大值和最小值
    private float maxKDJ,minKDJ;

    private boolean isDrawFinished = false;

    private boolean isEnable = true;
    private int offsetindex=0;


    private Paint techPaint;    //技术曲线的画笔
    private Paint techDesPaint; //技术数据lable画笔


    public TechnologyChart(Context context) {
        this(context, null);
    }

    public TechnologyChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        touchEventUtil = new TouchEventUtil(context, this);
        if (ONEL == 0) {
            spacing = CommonUtil.dip2px(context, spacing);
            itemw = CommonUtil.dip2px(context, itemw);
            ONEL = spacing + itemw;
        }
//        touchEventUtil.setLongMoveEnable(true);
//        touchEventUtil.setCanDrag(true);
        Constants.labelPaint.setColor(Color.DKGRAY);

        techPaint = new Paint();
        techPaint.setAntiAlias(true);
        techPaint.setStrokeWidth(Constants.pathlinewidth);
        techPaint.setStyle(Paint.Style.STROKE);
        techPaint.setStrokeCap(Paint.Cap.ROUND);
        techPaint.setStrokeJoin(Paint.Join.ROUND);

        techDesPaint = new Paint(Constants.labelPaint);
    }
    public void setLongMoveEnable(boolean moveEnable){
        touchEventUtil.setLongMoveEnable(moveEnable);
    }
    public void setCanDrag(boolean canDrag){
        touchEventUtil.setCanDrag(canDrag);
    }

    /**选择不同的技术指标类型*/
    public void switchTech(KLineType tech) {
        this.kLineType = tech;
        computeVars();
        invalidate();
    }
    public void setKeyLineType(KLineType type) {
        this.kLineType = type;
    }
    /**通过某点的坐标，确定图表中数据的索引*/
    @Override
    public int getMoveIndex(PointF pointF) {
        int index = indexEnd - Math.round((keyLineItems.get(indexEnd).pointF.x - pointF.x) / ONEL);
        if (index < indexStart)
            index = indexStart;
        if (index > indexEnd) index = indexEnd;
        return index;
    }

    public void setkLineTechParam(KLineTechParam kLineTechParam) {
        this.kLineTechParam = kLineTechParam;
    }



    /**数据恢复*/
    Bundle bundle =null;
    public void saveInstanceState(){
        if(bundle == null)
            bundle=new Bundle();
        bundle.putSerializable("TYPE",kLineType);
    }
    public void restroreState() {
        if (bundle == null)
            return;
        kLineType = (KLineType) bundle.getSerializable("TYPE");
        bundle=null;
    }


    /**************************设置数据参数相关start************************/
    /**拖动之后回调*/
    public void ondrag(KLineParame parame, int offset) {
        if (offset == offsetindex) {
            return;
        }
        offsetindex = offset;
        initStartAndEnd(offsetindex);
        computeVars();
        invalidate();
    }
    /**设置偏移量*/
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
        desIndex = indexEnd;
    }

    /**设置新数据或者参数变化后（缩放）重画*/
    public void setData(List<KeyLineItem> keyLineItems, KLineParame parame) {
        this.keyLineItems = keyLineItems;
        this.parame = parame;
        initLineData();
        computeVars();
        invalidate();
    }
    private void initLineData() {
        if (isStop) {
            maxValue = 0;
            return;
        }
        datasize = keyLineItems == null ? 0 : keyLineItems.size();

        //y刻度显示在外面
        start = Constants.chartStart;

        if(parame==null)
            return;
        number = parame.number;
        initStartAndEnd(0);
    }
    /**************************设置数据参数相关end************************/

    private void computeVars() {
        if(datasize==0)
            return;
//        System.out.println("indexstart:"+indexStart+" indexEnd:"+indexEnd);
        KeyLineItem item = null;
        KLineTechItem kLineTechItem = null;
        maxMACD = Float.MIN_VALUE;
        minMACD = Float.MAX_VALUE;

        maxKDJ = Float.MIN_VALUE;
        minKDJ = Float.MAX_VALUE;
        maxValue = 0;
        for (int i = indexStart; i <= indexEnd; i++) {
            item = keyLineItems.get(i);
            if (kLineType == KLineType.MACD) {
                if (kLineTechParam != null) {
                    //获取MACD指标最大值和最小值
                    kLineTechItem = kLineTechParam.getTechItem(i);
                    maxMACD = getMaxMACD(kLineTechItem);
                    minMACD = getMinMACD(kLineTechItem);
                }
            } else if (kLineType == KLineType.VOL) {
                //获取VOL指标最大成交量
                maxValue = Math.max(maxValue, item.vol);
            } else if(kLineType == KLineType.KDJ){
                if (kLineTechParam != null) {
                    kLineTechItem = kLineTechParam.getTechItem(i);
                    maxKDJ = getMaxKDJ(kLineTechItem);
                    minKDJ = getMinKDJ(kLineTechItem);
                }
            }
        }
        leftLable = CommonUtil.getDisplayVolume(
                String.valueOf(maxValue).length(), maxValue);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int specMode = MeasureSpec.getMode(heightMeasureSpec);
        int specSize = MeasureSpec.getSize(heightMeasureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        } else {
            int  height = Math.min(Constants.defaultChartHeightL, specSize);
            setMeasuredDimension(widthMeasureSpec, height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        isDrawFinished = false;

        initFrameRect();
        drawGridLine(canvas);
        if (datasize == 0){
            isDrawFinished=true;
            return;
        }
        restroreState();

        //四种技术指标
        if (kLineType == KLineType.VOL) {           //VOL成交量指标
            drawVOLLabel(canvas);
            drawVOLBars(canvas);
        } else if (kLineType == KLineType.MACD) {   //MACD指数平滑移动平均线
            drawMACDLable(canvas);
            drawMACDDes(canvas, kLineTechParam.getTechItem(desIndex));
            drawMACDData(canvas);
        } else if(kLineType == KLineType.KDJ){      //KDJ随机指标
            drawKDJLabel(canvas);
            drawKDJDes(canvas, kLineTechParam.getTechItem(desIndex));
            drawKDJData(canvas);
        } else if(kLineType == KLineType.RSI){      //RSI相对强弱指标
            drawRSILabel(canvas);
            drawRSIDes(canvas, kLineTechParam.getTechItem(desIndex));
            drawRSIData(canvas);
        }
        isDrawFinished = true;
    }

    /**表格绘制区域*/
    public void initFrameRect() {
        this.rectF.set(start, 0, getWidth(), getHeight());
        //RectF(123.0, 0.0, 1692.0, 180.0)
        LogUtil.v(TAG, "技术指标绘制区域："+rectF);
    }

    /**①、绘制网格*/
    private void drawGridLine(Canvas canvas){
        float x , y;
        //绘制y轴方向格线
        for (int i = 0; i < 2; i++) {
            if(i==0)
                x = rectF.left + Constants.gridlinewidth/2;
            else
                x = rectF.left + rectF.width() - Constants.gridlinewidth/2;
            //float startX, float startY, float stopX, float stopY,
            canvas.drawLine(x, 0, x, rectF.bottom, gridlinePaint);
        }
        Path path = new Path();
        //绘制x轴方向格线
        for (int j = 0; j < 5; j++) {
            y = rectF.bottom * j / 4;
            path.reset();
            switch (j){
                case 0:
                    gridlinePaint.setColor(Constants.C_AROUND_LINE);
                    canvas.drawLine(rectF.left, y+ Constants.gridlinewidth/2, rectF.right, y+ Constants.gridlinewidth/2, gridlinePaint);
                    break;
                case 4:
                    gridlinePaint.setColor(Constants.C_AROUND_LINE);
                    canvas.drawLine(rectF.left, y- Constants.gridlinewidth/2, rectF.right, y- Constants.gridlinewidth/2, gridlinePaint);
                    break;
                default:
                    path.moveTo(rectF.left, y);
                    path.lineTo(rectF.right, y);
                    gridlinePaint.setColor(Constants.C_GRID_LINE);
                    canvas.drawPath(path, gridlinePaint);
                    break;
            }
        }
    }


    /***************************1、VOL成交量指标*******************************/
    /**1.1绘制VOL指标Y刻度*/
    private void drawVOLLabel(Canvas canvas) {
        PointF pointF;
        Constants.labelPaint.setColor(Constants.C_LABLE_TEXT);
        pointF = getPointFY(Constants.labelPaint, 0, KLineType.VOL);
        canvas.drawText("0", pointF.x, pointF.y, Constants.labelPaint);
        pointF = getPointFY(Constants.labelPaint, 1, KLineType.VOL);
        //绘制最大刻度
        canvas.drawText(leftLable[0] + leftLable[1], pointF.x, pointF.y, Constants.labelPaint);
    }
    /**1.2绘制VOL成交量实体*/
    private void drawVOLBars(Canvas canvas) {
        if (isStop)
            return;
        int index = 0;
        Paint paint;
        float bl;
        PointF p;
        for (int i = indexStart; i <= indexEnd; i++) {
            KeyLineItem item = keyLineItems.get(i);
            bl =  item.vol * 1f / maxValue;
            p = getPointItem(rectF, index, bl);
            //收盘价小于开盘价，跌，用绿色柱状
            if (item.close < item.open)
                paint = Constants.fallPaint;
            else if (item.close > item.open)
                paint = Constants.raisePaint;
            else
                paint = Constants.NomalPaint;
            canvas.drawRect(p.x, p.y, p.x + itemw, rectF.bottom, paint);
            index++;
        }
    }

    /***************************2、MACD指数平滑移动平均线*******************************/
    /**2.1绘制MACD指数平滑移动平均线刻度*/
    private void drawMACDLable(Canvas canvas) {
        PointF pointF = null;
        Constants.labelPaint.setColor(Color.DKGRAY);
        if (minMACD < 0) {
            pointF = getPointFY(Constants.labelPaint, 0, KLineType.MACD);
            canvas.drawText(Constants.twoPointFormat.format(minMACD), pointF.x, pointF.y, Constants.labelPaint);
        }
        pointF = getPointFY(Constants.labelPaint, 2, KLineType.MACD);
        canvas.drawText("0", pointF.x, pointF.y, Constants.labelPaint);
        if (maxMACD > 0) {
            pointF = getPointFY(Constants.labelPaint, 1, KLineType.MACD);
            canvas.drawText(Constants.twoPointFormat.format(maxMACD), pointF.x, pointF.y, Constants.labelPaint);
        }
    }
    /**2.2绘制当前最后一个MACD数据*/
    private void drawMACDDes(Canvas canvas,KLineTechItem klineTechItem) {
        float y = GlFontUtil.getFontLeading(techDesPaint)+4;
        techDesPaint.setColor(Constants.COLOR_A20);
        //差离值
        String diftext = "DIF:"+ Constants.twoPointFormat.format(klineTechItem.dif);
        canvas.drawText(diftext, start+4, y, techDesPaint);
        techDesPaint.setColor(Constants.COLOR_A10);
        //离差平均值
        String deatext = "DEA:"+ Constants.twoPointFormat.format(klineTechItem.dea);
        canvas.drawText(deatext, start+8+ GlFontUtil.getFontlength(techDesPaint,diftext),
                y, techDesPaint);
    }
    /**2.3绘制MACD数据*/
    private void drawMACDData(Canvas canvas) {
        if (isStop) return;
        int index = 0;
        Paint paint;
        PointF point;
        KLineTechItem item;

        Path difpath = new Path();
        Path deapath = new Path();

        point = getPointMACD(index,kLineTechParam.getTechItem(indexStart).dif);
        difpath.moveTo(point.x,point.y);
        point = getPointMACD(index,kLineTechParam.getTechItem(indexStart).dea);
        deapath.moveTo(point.x,point.y);

        for (int i = indexStart; i <= indexEnd; i++) {
            item = kLineTechParam.getTechItem(i);
            point = getPointMACD(index,item.macd);
            //绘制MACD
            if (item.macd < 0) {
                paint = Constants.fallPaint;
                canvas.drawRect(point.x, rectF.bottom / 2, point.x + Constants.gridlinewidth, point.y, paint);
            } else {
                paint = Constants.raisePaint;
                canvas.drawRect(point.x, point.y, point.x + Constants.gridlinewidth, rectF.bottom / 2, paint);
            }
            if(index>0) {
                //绘制DIF、DEA线
                point = getPointMACD(index, item.dif);
                difpath.lineTo(point.x, point.y);
                point = getPointMACD(index, item.dea);
                deapath.lineTo(point.x, point.y);
            }
            index++;
        }
        if(datasize>1) {
            techPaint.setColor(Constants.COLOR_A20);
            canvas.drawPath(difpath, techPaint);
            techPaint.setColor(Constants.COLOR_A10);
            canvas.drawPath(deapath, techPaint);
        }
    }

    private float getMaxMACD(KLineTechItem tech) {
        float max = tech.macd;
        max = Math.max(max, tech.dea);
        max = Math.max(max, tech.dif);
        return Math.max(maxMACD, max);
    }
    private float getMinMACD(KLineTechItem tech) {
        float min = tech.macd;
        min = Math.min(min, tech.dea);
        min = Math.min(min, tech.dif);
        return Math.min(minMACD, min);
    }

    private PointF getPointMACD(int index,float value) {
        float h;
        float x = start + index * ONEL+ itemw / 2;
        if (value > 0) {
            h = (maxMACD - value) * rectF.bottom / (maxMACD * 2);
//            h = -Math.abs(tech.macd * rectF.height()/(2*maxMACD));
        } else {
            h = rectF.bottom / 2 + Math.abs(value * rectF.bottom / (minMACD * 2));
//            h = Math.abs(tech.macd * rectF.height()/(2*minMACD));
        }
        return new PointF(x, h);
    }

    /***************************3、KDJ指标*******************************/
    /**3.1绘制KDJ指标刻度*/
    private void drawKDJLabel(Canvas canvas) {
        PointF pointF = null;
        Constants.labelPaint.setColor(Color.DKGRAY);
        pointF = getPointFY(Constants.labelPaint, 0, KLineType.KDJ);
        canvas.drawText(Constants.twoPointFormat.format(minKDJ), pointF.x, pointF.y, Constants.labelPaint);
        pointF = getPointFY(Constants.labelPaint, 1, KLineType.KDJ);
        canvas.drawText(Constants.twoPointFormat.format(maxKDJ), pointF.x, pointF.y, Constants.labelPaint);
    }
    /**3.1绘制KDJ指标最新数据*/
    private void drawKDJDes(Canvas canvas,KLineTechItem klineTechItem) {
        float y = GlFontUtil.getFontLeading(techDesPaint)+4;
        techDesPaint.setColor(Constants.COLOR_A5);
        String ktext = "K:"+ Constants.twoPointFormat.format(klineTechItem.k);
        float w= start+4;
        canvas.drawText(ktext,w,y,techDesPaint);

        w = w+ GlFontUtil.getFontlength(techDesPaint,ktext)+4;
        techDesPaint.setColor(Constants.COLOR_A10);
        String dtext = "D:"+ Constants.twoPointFormat.format(klineTechItem.d);
        canvas.drawText(dtext,w,y,techDesPaint);

        w = w+ GlFontUtil.getFontlength(techDesPaint,dtext)+4;
        techDesPaint.setColor(Constants.COLOR_A20);
        String jtext = "J:"+ Constants.twoPointFormat.format(klineTechItem.j);
        canvas.drawText(jtext,w,y,techDesPaint);
    }
    /**3.1绘制KDJ指标线*/
    private void drawKDJData(Canvas canvas) {
        if (isStop||datasize==1) return;
        int index = 1;
        KLineTechItem item;
        float bk =( kLineTechParam.getTechItem(indexStart).k - minKDJ) * 1.0f
                / (maxKDJ-minKDJ);
        float bd = (kLineTechParam.getTechItem(indexStart).d - minKDJ) * 1.0f
                / (maxKDJ-minKDJ);
        float bj = (kLineTechParam.getTechItem(indexStart).j - minKDJ) * 1.0f
                / (maxKDJ-minKDJ);

        Path kpath = new Path();
        Path dpath = new Path();
        Path jpath = new Path();

        PointF p = getPointItem(rectF, 0, bk);
        kpath.moveTo(p.x+itemw/2,p.y);
        p = getPointItem(rectF,0,bd);
        dpath.moveTo(p.x+itemw/2,p.y);
        p = getPointItem(rectF,0,bj);
        jpath.moveTo(p.x+itemw/2,p.y);
        for (int i = indexStart+1; i <= indexEnd; i++) {
            item = kLineTechParam.getTechItem(i);
            bk = (item.k -minKDJ) * 1.0f / (maxKDJ-minKDJ);
            bd = (item.d -minKDJ) * 1.0f / (maxKDJ-minKDJ);
            bj = (item.j -minKDJ) * 1.0f / (maxKDJ-minKDJ);

            p = getPointItem(rectF, index, bk);
            kpath.lineTo(p.x+itemw/2,p.y);
            p = getPointItem(rectF,index,bd);
            dpath.lineTo(p.x+itemw/2,p.y);
            p = getPointItem(rectF,index,bj);
            jpath.lineTo(p.x+itemw/2,p.y);

            index++;
        }
        techPaint.setColor(Constants.COLOR_A5);
        canvas.drawPath(kpath,techPaint);
        techPaint.setColor(Constants.COLOR_A10);
        canvas.drawPath(dpath,techPaint);
        techPaint.setColor(Constants.COLOR_A20);
        canvas.drawPath(jpath,techPaint);
    }

    private float getMaxKDJ(KLineTechItem tech) {
        float max = tech.k;
        max = Math.max(max, tech.d);
        max = Math.max(max, tech.j);
        return Math.max(maxKDJ, max);
    }
    private float getMinKDJ(KLineTechItem tech) {
        float min = tech.k;
        min = Math.min(min, tech.d);
        min = Math.min(min, tech.j);
        return Math.min(minKDJ, min);
    }

    /***************************4、RSI指标*******************************/
    /**4.1绘制RSI指标Y刻度*/
    private void drawRSILabel(Canvas canvas) {
        PointF pointF;
        Constants.labelPaint.setColor(Constants.C_LABLE_TEXT);
        pointF = getPointFY(Constants.labelPaint, 0, KLineType.RSI);
        canvas.drawText("0", pointF.x, pointF.y, Constants.labelPaint);
        pointF = getPointFY(Constants.labelPaint, 1, KLineType.RSI);
        canvas.drawText("100", pointF.x, pointF.y, Constants.labelPaint);
    }
    /**4.2绘制RSI指数*/
    private void drawRSIDes(Canvas canvas, KLineTechItem klineTechItem) {
        techDesPaint.setColor(Constants.COLOR_A5);
        String ktext = "6:"+ Constants.twoPointFormat.format(klineTechItem.rsi1);
        float y = GlFontUtil.getFontLeading(techDesPaint)+4;
        float w = start+4;
        canvas.drawText(ktext, w, y, techDesPaint);

        w = w+ GlFontUtil.getFontlength(techDesPaint,ktext)+4;

        techDesPaint.setColor(Constants.COLOR_A10);
        String dtext = "12:"+ Constants.twoPointFormat.format(klineTechItem.rsi2);
        canvas.drawText(dtext, w, y, techDesPaint);

        w = w + GlFontUtil.getFontlength(techDesPaint,dtext)+4;
        techDesPaint.setColor(Constants.COLOR_A20);
        String jtext = "24:"+ Constants.twoPointFormat.format(klineTechItem.rsi3);
        canvas.drawText(jtext, w, y,techDesPaint);
    }
    /**4.3绘制RSI*/
    private void drawRSIData(Canvas canvas) {
        if (isStop)
            return;
        KLineTechItem item;
        //第一个数据
        float bk =kLineTechParam.getTechItem(indexStart).rsi1  / 100;
        float bd =kLineTechParam.getTechItem(indexStart).rsi2  / 100;
        float bj =kLineTechParam.getTechItem(indexStart).rsi3  / 100;

        Path kpath = new Path();
        Path dpath = new Path();
        Path jpath = new Path();

        PointF p = getPointItem(rectF, 0, bk);
        kpath.moveTo(p.x+itemw/2,p.y);
        p = getPointItem(rectF,0,bd);
        dpath.moveTo(p.x+itemw/2,p.y);
        p = getPointItem(rectF,0,bj);
        jpath.moveTo(p.x+itemw/2,p.y);

        int index = 1;

        for (int i = indexStart+1; i <= indexEnd; i++) {
            item = kLineTechParam.getTechItem(i);
            bk = item.rsi1  / 100;
            bd = item.rsi2  / 100;
            bj = item.rsi3  / 100;

            p = getPointItem(rectF, index, bk);
            kpath.lineTo(p.x+itemw/2,p.y);
            p = getPointItem(rectF,index,bd);
            dpath.lineTo(p.x+itemw/2,p.y);
            p = getPointItem(rectF,index,bj);
            jpath.lineTo(p.x+itemw/2,p.y);

            index++;
        }
        techPaint.setColor(Constants.COLOR_A5);
        canvas.drawPath(kpath,techPaint);
        techPaint.setColor(Constants.COLOR_A10);
        canvas.drawPath(dpath,techPaint);
        techPaint.setColor(Constants.COLOR_A20);
        canvas.drawPath(jpath,techPaint);
    }

    /**计算lable的Y刻度*/
    private PointF getPointFY(Paint paint, int p, KLineType type) {
        float leading = GlFontUtil.getFontLeading(paint);
        float h = GlFontUtil.getFontHeight(paint);
        if (p == 0) {//下边
            float w = 0;
            if (type == KLineType.VOL||type==KLineType.RSI)
                w = GlFontUtil.getFontlength(paint, "0");
            else if (type == KLineType.MACD) {
                w = GlFontUtil.getFontlength(paint, Constants.twoPointFormat.format(minMACD));
            } else if(type == kLineType.KDJ){
                w = GlFontUtil.getFontlength(paint, Constants.twoPointFormat.format(minKDJ));
            }
            return new PointF(rectF.left - w - Constants.S_LABLE_CHART_DIS,
                    getHeight() - h + leading);
        } else if (p == 1) {//上边
            float w = 0;
            if (type == KLineType.VOL)
                w = GlFontUtil.getFontlength(paint, leftLable[0] + leftLable[1]);
            else if (type == KLineType.MACD) {
                w = GlFontUtil.getFontlength(paint, Constants.twoPointFormat.format(maxMACD));
            } else if(type == KLineType.KDJ){
                w = GlFontUtil.getFontlength(paint, Constants.twoPointFormat.format(maxKDJ));
            }else if(type == KLineType.RSI){
                w = GlFontUtil.getFontlength(paint, "100");
            }
            return new PointF(rectF.left - w - Constants.S_LABLE_CHART_DIS, leading);
        } else if (p == 2) {//中间
            float w = 0;

            if (type == KLineType.MACD) {
                w = GlFontUtil.getFontlength(paint, "0");
            }
            return new PointF(rectF.left - w - Constants.S_LABLE_CHART_DIS, (getHeight() - h) / 2 + leading);
        }

        return new PointF();
    }

    /**获取某个数据在图表上的坐标*/
    private PointF getPointItem(RectF rectF, int index, float scaleY) {
        PointF point = new PointF();
        point.x = start + index * ONEL;
        //注意：Y轴坐标上面位0，向下为正值，此处获取坐标 = 总高度 - 总高度*比重 = 总高度*（1-比重）
        point.y = rectF.top + rectF.height() * (1 - scaleY);
        return point;
    }



    public void setOnFocusChangeListener(TouchEventUtil.OnFoucsChangedListener listener) {
        if (touchEventUtil == null) touchEventUtil = new TouchEventUtil(context, this);
        touchEventUtil.setFoucsChangedListener(listener);
    }

    public void updateDes(int index){
        desIndex= index;
        invalidate();
    }
    public int getLastItemIndex() {
        return indexEnd;
    }

    public void setOnDragListener(TouchEventUtil.OnDragChangedListener listener) {
        if (touchEventUtil == null)
            touchEventUtil = new TouchEventUtil(context, this);
        touchEventUtil.setDragChangeListener(listener);
    }

    public void setIsStop(boolean isStop) {
        this.isStop = isStop;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean b = super.dispatchTouchEvent(event);
        if (!isDrawFinished) return true;
        if (!isEnable) return b;
        if (touchEventUtil == null) touchEventUtil = new TouchEventUtil(context, this);
        return touchEventUtil.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrawFinished) return true;
        if (!isEnable) return super.onTouchEvent(event);
        return touchEventUtil.onTouchEvent(event);
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public KLineType getkLineType(){
        return this.kLineType;
    }
}
