package com.openxu.chartlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.utils.LogUtil;


/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : ChartContainerView
 * version : 1.0
 * class describe：K线图的容器View，监听手指的缩放事件
 */
public class ChartContainerView extends FrameLayout {

    private String TAG = "ChartContainerView";

    boolean isPointerDown=false;
    private float scale = 1;
    PointF start=new PointF();
    PointF pointer= new PointF();
    private float distance;


    public interface OnScalListener{
        void scaleChart(float scale, boolean isEnd);
    }
    public OnScalListener scalListener=null;

    public void setScalListener(OnScalListener scalListener) {
        this.scalListener = scalListener;
    }

    public ChartContainerView(Context context) {
        this(context,null);
    }

    public ChartContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /***/
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if(isPointerDown){
            LogUtil.e(TAG, "多点缩放状态，需要拦截事件");
            return true;
        }else{
            LogUtil.i(TAG, "不是多点缩放状态，不需要拦截事件");
            return super.onInterceptTouchEvent(event);
        }
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:   //非第一个触摸点按下（第二个以上手指），多点缩放
                isPointerDown=true;
                start.set(event.getX(0), event.getY(0));
                pointer.set(event.getX(1), event.getY(1));
                distance = getDistance(pointer.x - start.x, pointer.y - start.y);
                break;
            case MotionEvent.ACTION_POINTER_UP:      //非第一个触摸点抬起
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isPointerDown=false;
                scalListener.scaleChart(scale,true);
                break;
        }
        return super.dispatchTouchEvent(event);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        if(action == MotionEvent.ACTION_MOVE){
            if(isPointerDown){
                //如果是两个手指在移动，控制缩放
                toZoom(event);
                return true;
            }
        }
        return super.onTouchEvent(event);

    }


    private float getDistance(float dx, float dy) {
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    float newDistance=0;
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private void toZoom(MotionEvent event) {

        PointF curr = new PointF();
        PointF currP = new PointF();
        curr.set(event.getX(0), event.getY(0));
        currP.set(event.getX(1), event.getY(1));

        newDistance = getDistance(currP.x - curr.x, currP.y - curr.y);
        if(newDistance == distance)
            return;
        if (newDistance > Constants.EPSILON) {
            scale = newDistance / distance;
            scalListener.scaleChart(scale,false);
        }
    }
}
