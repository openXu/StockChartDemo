package com.openxu.chartlib.bean;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KlineResult
 * version : 1.0
 * class describe：K线返回数据的结构类
 */
public class KlineResult implements Serializable{
    private int code;
    private String msg;

    private ArrayList<KeyLineItem> data=null;

    public ArrayList<KeyLineItem> getData() {
        return data;
    }

    public void setData(ArrayList<KeyLineItem> data) {
        this.data = data;
    }

}
