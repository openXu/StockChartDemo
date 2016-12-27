package com.openxu.chartlib.utils;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.graphics.PointF;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.view.Chart;


/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : TouchEventUtil
 * version : 1.0
 * class describe： 行情图的手指触摸事件处理类
 */
public class TouchEventUtil {

    private String TAG = "TouchEventUtil";

    private Context mcontext;
    private Chart chartView;

    //是否支持长按事件（K线图长按展示焦点）
    private boolean longPresEnable = false;
    //是否处于长按滑动状态
    private boolean longMove = false;
    //是否移动了
    private boolean isMoved;
    //是否是分时图
    private boolean isminute = false;
    //是否可拖拽
    private boolean canDrag = false;
    //移动的阈值
    private int mTouchSlop;
    private float mLastMotionX, mLastMotionY;

    private HandlerThread handlerThread = null;
    private Handler handler = null;

    private OnFoucsChangedListener foucsChangedListener = null;
    private OnDragChangedListener dragChangeListener = null;

    private VelocityTracker mVelocityTracker;
    private  float mDeceleration;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private SmoothScrollRunnable currentSmoothScrollRunnable;

    //焦点坐标
    private PointF focusPoint = new PointF();
    //结束坐标
    private PointF endPoint = new PointF();
    //手指按下的坐标
    private PointF startPoint = new PointF();
    private float offset = 0;

    private EventMode mode = EventMode.NONE;   //模式，当前是焦点模式还是拖拽模式
    /**模式（无、焦点、拖拽）*/
    private enum EventMode {
        NONE,
        FOCUS,  //分时图焦点、K线图长按焦点
        DRAG   //K线图拖拽
    }

    public interface OnFoucsChangedListener {
        void foucsChanged(int tag, int index);
    }

    public interface OnDragChangedListener {
        void dragChanged(boolean flag, float offset);
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    public TouchEventUtil(Context context, Chart view) {
        this.mcontext = context;
        this.chartView = view;
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        initHandler();
        initSmoothScroll(context);
    }

    public void setFoucsChangedListener(OnFoucsChangedListener foucsChangedListener) {
        this.foucsChangedListener = foucsChangedListener;
    }
    public void setDragChangeListener(OnDragChangedListener dragChangeListener) {
        this.dragChangeListener = dragChangeListener;
    }
    public void setLongMoveEnable(boolean enable) {
        this.longPresEnable = enable;
    }
    public void setIsminute(boolean enable) {
        this.isminute = enable;
    }
    public void setCanDrag(boolean canDrag) {
        this.canDrag = canDrag;
    }

    /**长按事件后执行*/
    private Runnable  mLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            longMove = true;
            PointF focusPoint = new PointF();
            focusPoint.set(mLastMotionX, mLastMotionY);
            LogUtil.v(TAG, "长按震动,模式置为焦点");
            Vibrator vibrator = (Vibrator) mcontext.getSystemService(Service.VIBRATOR_SERVICE);
            vibrator.vibrate(20);
            mode= EventMode.FOCUS;
            touchFocusMove(focusPoint, false);
        }
    };

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void initHandler() {
        handlerThread = new HandlerThread("focus");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper(),new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case Constants.FocusCancelFlag:
                    case Constants.FocusChangeFlag:
                        if (foucsChangedListener != null)
                            foucsChangedListener.foucsChanged(msg.what, (Integer) msg.obj);
                        break;
                    case Constants.DragRecetFlag:
                        if (offset ==(Float)msg.obj)
                            return true;
                        if (dragChangeListener != null) {
                            dragChangeListener.dragChanged(false,(Float) msg.obj);
                        }
                        break;
                    case Constants.DragChangeFlag:
                        if (dragChangeListener != null) {
                            dragChangeListener.dragChanged(true, (Float) msg.obj);
                        }
                        break;
                }
                return true;
            }
        });

    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        LogUtil.w(TAG, "事件分发dispatchTouchEvent==========");
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = x;
                mLastMotionY = y;
                isMoved = false;
                //如果支持长按事件，在超过长按时间后执行mLongPressRunnable
                if (longPresEnable && chartView != null) {
                    chartView.postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (chartView == null)
                    break;
                if (isMoved || longMove)
                    break;
                if (Math.abs(mLastMotionX - x) > mTouchSlop
                        || Math.abs(mLastMotionY - y) > mTouchSlop) {
                    //移动超过阈值，则表示移动了
                    isMoved = true;
                    chartView.removeCallbacks(mLongPressRunnable);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
                if (chartView != null) {
                    chartView.removeCallbacks(mLongPressRunnable);
                    //释放了
                    longMove = false;
                }
                break;
        }
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        LogUtil.w(TAG, "事件onTouchEvent============");
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                stopFling();
                Constants.isEnd = false;
                if(isminute) {
                    //如果是分时图，置为焦点模式
                    mode = EventMode.FOCUS;
                    LogUtil.d(TAG, "按下事件，分时图模式置为焦点");
                }else {
                    //如果是K线图，置为拖拽模式（长按时在mLongPressRunnable中置为焦点模式）
                    mode = EventMode.DRAG;    //K线图可以拖拽
                    LogUtil.d(TAG, "按下事件，K线图模式置为拖拽");
                }
                startPoint.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode== EventMode.FOCUS) {
                    LogUtil.d(TAG, "滑动事件，模式为焦点");
                    focusPoint.set(event.getX(), event.getY());
                    touchFocusMove(focusPoint, false);
                } else if (canDrag && mode == EventMode.DRAG) {
                    LogUtil.d(TAG, "滑动事件，模式为拖拽");
                    if ((int) event.getX() == (int) endPoint.x)
                        return true;
                    endPoint.set(event.getX(), event.getY());
                    touchDrag(endPoint, false);
                }
                break;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mode == EventMode.FOCUS) {
                    longMove = false;
                    touchFocusMove(null, true);
                } else if (mode == EventMode.DRAG) {
                    touchDrag(null, true);
                }
                recycleVelocityTracker();
                mode = EventMode.NONE;
                break;
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.DONUT)
    private void initSmoothScroll(Context context){
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        float ppi = context.getResources().getDisplayMetrics().density * 160.0f;
        mDeceleration = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi // pixels per inch
                * ViewConfiguration.getScrollFriction() * 4;
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void fling(int velocity) {
        if (null != currentSmoothScrollRunnable) {
            currentSmoothScrollRunnable.stop();
        }
        if (Math.abs(mDeceleration) > Constants.EPSILON) {
            int duration = (int) (1000 * velocity / mDeceleration);
            int totalDistance = (int) ((velocity * velocity) / (mDeceleration + mDeceleration));
            int startX = (int) offset, toX = 0;
            if (velocity < 0) {
                toX = startX - totalDistance;
            } else {
                toX = startX + totalDistance;
            }
            this.currentSmoothScrollRunnable = new SmoothScrollRunnable(
                    handler, startX, toX, duration);
            handler.post(currentSmoothScrollRunnable);
        }
    }
    public void stopFling() {
        if (null != currentSmoothScrollRunnable) {
            currentSmoothScrollRunnable.stop();
        }
    }

    /**缓慢滑动*/
    final class SmoothScrollRunnable implements Runnable {
        private final int ANIMATION_FPS =  1000 / 60;
        private final int duration;

        private final Interpolator interpolator;
        private final int scrollFromX;
        private final int scrollToX;
        private final Handler handler;

        public volatile boolean continueRunning = true;
        private long startTime = -1;

        public SmoothScrollRunnable(Handler handler, int fromX, int toX,
                                    int duration) {
            this.handler = handler;
            this.scrollFromX = fromX;
            this.scrollToX = toX;
            this.duration = Math.abs(duration);
            this.interpolator = new DecelerateInterpolator();
        }

        @Override
        public void run() {
            long normalizedTime = 0L;
            if (this.startTime == -1) {
                this.startTime = System.currentTimeMillis();
            } else if (duration > 0) {
                normalizedTime = (1000 * (System.currentTimeMillis() - startTime))
                        / duration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);
            }
            final int deltaX = Math
                    .round((scrollFromX - scrollToX)
                            * interpolator
                            .getInterpolation(normalizedTime / 1000f));
            offset = scrollFromX - deltaX;

            if (continueRunning) {
                if(offset!=0) {
//                    Message msg = handler.obtainMessage(Constants.DragChangeFlag);
//                    msg.obj = offset;
//                    handler.sendMessage(msg);
                    toDrag(offset, Constants.DragChangeFlag);
                    if (offset == scrollToX) {
                        continueRunning = false;
                    }
                }
                handler.post(this);
            } else {
                offset = 0;
                toDrag(offset, Constants.DragRecetFlag);
            }

        }

        public void stop() {
            if(this.continueRunning) {
                this.continueRunning = false;
                this.handler.removeCallbacks(this);
                toDrag(Constants.EPSILON, Constants.DragRecetFlag);
            }
        }
    }

    /**焦点滑动*/
    public void touchFocusMove(final PointF point, final boolean outable) {
        if(chartView == null||chartView.datasize==0)
            return;
        Message msg;
        if (outable) {
            //焦点结束
            msg = handler.obtainMessage(Constants.FocusCancelFlag);
            msg.obj = 0;
            msg.sendToTarget();
        } else if (point != null) {
            //焦点位置变化
            int index = chartView.getMoveIndex(point);
            msg = handler.obtainMessage(Constants.FocusChangeFlag);
            msg.obj = index;
            handler.sendMessage(msg);
        }
    }

    /** 拖拽 */
    private void touchDrag(PointF endPoint, boolean outabale) {
        LogUtil.i(TAG, "---------------------拖拽了touchDrag()。。。。。。。。。。。。。。");
        if(chartView==null)return;
        if (outabale) {   //如果手指已经抬起，或者滑出，缓冲
            smoothScroll();
        } else {
            offset = startPoint.x - endPoint.x;
            toDrag(offset, Constants.DragChangeFlag);
        }
    }

    @TargetApi(Build.VERSION_CODES.DONUT)
    private void smoothScroll(){
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        int initialVelocity = (int) velocityTracker.getXVelocity();

        if (Math.abs(initialVelocity) > mMinimumVelocity) {
//            System.out.println("initialVelocity:"+initialVelocity);
            fling(-initialVelocity);
        } else {
            offset = 0;
            toDrag(offset, Constants.DragRecetFlag);
        }
    }
    private void toDrag(float offset,int flag){
        Message msg= handler.obtainMessage(flag);
        msg.obj = offset;
        handler.sendMessage(msg);
    }






    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public void destroy() {
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (handlerThread != null) {
            handlerThread.quit();
            handlerThread = null;
        }
    }


}
