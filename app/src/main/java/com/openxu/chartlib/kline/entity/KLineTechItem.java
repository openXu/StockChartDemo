package com.openxu.chartlib.kline.entity;


import java.io.Serializable;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KLineTech
 * version : 1.0
 * class describe：K线指标数据类，每个K线实体对应一个技术指标数据
 */
public class KLineTechItem implements Serializable{

    //MACD:
    public float dif = .0f;   //差离值
    public float dea = .0f;   //离差平均值
    public float macd = .0f;

    //KDJ:
    public float k = .0f;
    public float d = .0f;
    public float j = .0f;


    //RSI:
    public float rsi1 = .0f; //6
    public float rsi2 = .0f; //12
    public float rsi3 = .0f; //24


/*    *//**取参数中的最大值、最小值*//*
    public void getBundle(KLineType paramsType, KLineTechParam.ParamsLimit limit) {
        switch(paramsType) {
            case KDJ:
                if(d > j) {
                    limit._max = d;
                    limit._min = j;
                } else {
                    limit._max = j;
                    limit._min = d;
                }
                break;
            case MACD:
                limit._max = dif; //suppose dif > dea
                limit._min = dea;
                if(dif < dea) {
                    limit._max = dea;
                    limit._min = dif;
                }
                if(limit._min > macd) {
                    limit._min = macd;
                }
                if(limit._max < macd) {
                    limit._max = macd;
                }
                break;
            case RSI:
                limit._max = rsi1; //suppose rsi1 > rsi3
                limit._min = rsi3;
                if(rsi1 < rsi3) {
                    limit._max = rsi3;
                    limit._min = rsi1;
                }
                if(limit._min > rsi2) {
                    limit._min = rsi2;
                }
                if(limit._max < rsi2) {
                    limit._max = rsi2;
                }
                break;
            default:
                break;
        }
    }

    *//**取参数值*//*
    public float getTech(KLineType paramsType, int pos) {
        float techVal = .0f;
        switch(paramsType) {
            case KDJ:
                switch(pos) {
                    case Constants.TECHLINELEFT:
                        techVal = k;
                        break;
                    case Constants.TECHLINEMID:
                        techVal = d;
                        break;
                    case Constants.TECHLINERIGHT:
                        techVal = j;
                        break;
                }
                break;
            case MACD:
                switch(pos) {
                    case Constants.TECHLINELEFT:
                        techVal = dif;
                        break;
                    case Constants.TECHLINEMID:
                        techVal = dea;
                        break;
                    case Constants.TECHLINERIGHT:
                        techVal = macd;
                        break;
                }
                break;
            case RSI:
                switch(pos) {
                    case Constants.TECHLINELEFT:
                        techVal = rsi1;
                        break;
                    case Constants.TECHLINEMID:
                        techVal = rsi2;
                        break;
                    case Constants.TECHLINERIGHT:
                        techVal = rsi3;
                        break;
                }
                break;
            default:
                break;
        }

        return techVal;
    }*/

}
