package com.openxu.chartlib.bean;

import java.io.Serializable;

/**
 * author : openXu
 * create at : 2016/07/8 13:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : ChartParame
 * version : 1.0
 * class describe：图标需要的各种参数，方便类之间的传递
 */
public class KLineParame implements Serializable{

    public float maxPrice=0,minPrice=Float.MAX_VALUE;  //最高价、最低价
    public float maxpriceLenth;
    public int maxVolume;              //最大成交量
    public String[] leftVolume;        //最大成交量（数量+单位数组）
    public int pricedivider;
    public float volumeper;
    public int number = 0;               //K线图当前展示的实体个数
}
