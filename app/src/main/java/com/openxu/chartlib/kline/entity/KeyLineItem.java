package com.openxu.chartlib.kline.entity;


import android.graphics.PointF;

import java.io.Serializable;


/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KeyLineItem
 * version : 1.0
 * class describe：k线图实体
 */
public class KeyLineItem implements Serializable{
    public String date;   //时间
    public float open;    //开盘价
    public float close;   //收盘价
    public float high;    //最高价
    public float low;     //最低价
    public long vol;      //成交量

    public float close_yestoday= 0.0f;  //昨日收盘价（上一个数据的收盘价）

    public float turnover; //转手率
    public float zhenfu;   //振幅 （最高价-最低价）/昨日收盘价
    public boolean isFall; //跌涨  true跌  false 涨
    public boolean isFallStop=false;   //跌停
    public boolean isRaiseStop=false;  //涨停



    public float avg5=Float.NEGATIVE_INFINITY;   //5日平均收盘价
    public float avg10=Float.NEGATIVE_INFINITY;  //10日平均收盘价
    public float avg20=Float.NEGATIVE_INFINITY;  //20日平均收盘价

    public transient PointF pointF=new PointF();

    public int index;

}
