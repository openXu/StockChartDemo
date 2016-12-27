package com.openxu.chartlib.callback;


import com.openxu.chartlib.bean.StockBaseInfo;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : BasicResultCallBack
 * version : 1.0
 * class describe：股票基本信息请求回调
 */
public interface BasicResultCallBack {
    void callback(StockBaseInfo stockBaseInfo);
}
