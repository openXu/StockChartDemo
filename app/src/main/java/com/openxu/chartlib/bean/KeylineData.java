package com.openxu.chartlib.bean;

import com.openxu.chartlib.config.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KeylineData
 * version : 1.0
 * class describe：技术指标数据处理类
 */
public class KeylineData {
    private List<KeyLineItem> list;
    private KLineTechParam kLineTechParam;

    public KeylineData(List<KeyLineItem> list, KLineTechParam kLineTechParam){
        this.list = list;
        this.kLineTechParam = kLineTechParam;
        if(kLineTechParam.listParams == null) {
            int capacity = list.size();
            if(capacity <= 0) {
                capacity = 32;
            }
            //创建一个跟K线实体数量一样的技术指标集合
            kLineTechParam.listParams = new ArrayList<>(capacity);
        }
    }


    /**计算技术指标*/
    public void doTechData(KLineType keylineType) {
        switch (keylineType) {
            case VOL:
                break;
            case KDJ:
                if (kLineTechParam.isKDJOL())
                    return;
                kdj();
                kLineTechParam.setKDJOL(true);
                break;
            case RSI:
                if (kLineTechParam.isRSIOL())
                    return;
                rsi();
                kLineTechParam.setRSIOL(true);
                break;
            case MACD:
                if (kLineTechParam.isMACDOL())
                    return;
                macd();
                kLineTechParam.setMACDOL(true);
                break;
        }
    }

    /**
     * ①、KDJ随机指标
     *
     * KDJ的计算比较复杂，首先要计算周期（n日、n周等）的RSV值，即未成熟随机指标值，
     * 然后再计算K值、D值、J值等。以n日KDJ数值的计算为例，其计算公式为：
     *
     * n日RSV=（Cn－Ln）/（Hn－Ln）×100
     *      公式中，Cn为第n日收盘价；Ln为n日内的最低价；Hn为n日内的最高价。
     *
     * 其次，计算K值与D值：
     * 当日K值=2/3×前一日K值+1/3×当日RSV
     * 当日D值=2/3×前一日D值+1/3×当日K值
     * 若无前一日K 值与D值，则可分别用50来代替。
     * J值=3*当日K值-2*当日D值
     *
     * 以9日为周期的KD线为例，即未成熟随机值，计算公式为
     * 9日RSV=（C－L9）÷（H9－L9）×100
     * 公式中，C为第9日的收盘价；L9为9日内的最低价；H9为9日内的最高价。
     * K值=2/3×第8日K值+1/3×第9日RSV
     * D值=2/3×第8日D值+1/3×第9日K值
     * J值=3*第9日K值-2*第9日D值
     * 若无前一日K值与D值，则可以分别用50代替
     */
    public void kdj() {
        int size = list.size();
        KeyLineItem mb;
        KLineTechItem kLineTechItem;
        KLineTechItem preKLintTech = null;

        final int _EK=3, _ED=3, _EM=9, _DEFAULTK=17;
        float RSV = .0f, _nLow, _nHigh;

        for (int i = 0; i < size; i++) {
            mb = list.get(i);
            kLineTechItem = kLineTechParam.getTechItem(i);
            if(i==0){
                if(mb.high == mb.low) {
                    kLineTechItem.k =_DEFAULTK;
                } else {
                    kLineTechItem.k = (mb.close - mb.low) / (mb.high - mb.low) * 100 / _EK;//50//RSV/_EK
                }
                kLineTechItem.d = kLineTechItem.k/_ED;
                kLineTechItem.j = 3* kLineTechItem.k - 2* kLineTechItem.d;
            }  else{
                _getNLowHigh(i, list, _EM);
                _nHigh = _max;
                _nLow = _min;

                if(_nHigh == _nLow) {
                    kLineTechItem.k = _DEFAULTK;
                } else {
                    //9日RSV=（C－L9）÷（H9－L9）×100
                    RSV = (mb.close-_nLow) / (_nHigh-_nLow) * 100;
                    //K值=2/3×第8日K值+1/3×第9日RSV
                    kLineTechItem.k = (_EK-1) * preKLintTech.k/_EK + RSV/_EK;
                }
                //D值=2/3×第8日D值+1/3×第9日K值
                kLineTechItem.d = (_ED-1) * preKLintTech.d / _ED + kLineTechItem.k / _ED;
                //J值=3*第9日K值-2*第9日D值
                kLineTechItem.j = 3* kLineTechItem.k - 2 * kLineTechItem.d;
            }
            preKLintTech = kLineTechItem;
        }
    }

    public float _max = .0f;
    public float _min = .0f;
    /**获取9日内最高价和最低价的极值*/
    public void _getNLowHigh(int index, List<KeyLineItem> list, int _EM) {
        _min = Float.MAX_VALUE; //_nLow
        _max = 0.01f;           //_nHigh
        int i = 0;
        if(index >= _EM) {
            i = index-_EM+1;
        }
        KeyLineItem itemK;
        for(; i <= index; i++) {
            itemK = list.get(i);
            _min = Math.min(itemK.low, _min);
            _max = Math.max(itemK.high, _max);
        }
    }


    /**
     * ②、指数平滑移动平均线
     *
     * MACD（Moving Average Convergence and Divergence)是Geral Appel 于1970年提出的，
     * 利用收盘价的 短期（常用为12日）指数移动平均线与 长期（常用为26日）指数移动平均线之间的聚合与分离状况，
     * 对买进、卖出时机作出研判的技术指标。
     *
     * MACD在应用上应先行计算出快速（一般选12日）移动平均值与慢速（一般选26日）移动平均值。
     * 以这两个数值作为测量两者（快速与慢速线）间的“差离值”依据。
     *
     * 所谓“差离值”（DIF），即12日EMA数值减去26日EMA数值。因此，在持续的涨势中，12日EMA在26日EMA之上。
     * 其间的正差离值（+DIF）会愈来愈大。反之在跌势中，差离值可能变负（-DIF），也愈来愈大。
     * 至于行情开始回转，正或负差离值要缩小到一定的程度，才真正是行情反转的信号。MACD的反转信号界定为“差离值”的
     * 9日移动平均值（9日EMA）。 在MACD的指数平滑移动平均线计算公式中，都分别加T+1交易日的份量权值，以现在流行
     * 的参数12和26为例，其公式如下：
     *
     * 12日EMA的计算：
     * EMA（12） = 前一日EMA（12） X 11/13 + 今日收盘价 X 2/13
     * 26日EMA的计算：
     * EMA（26） = 前一日EMA（26） X 25/27 + 今日收盘价 X 2/27
     *
     * 差离值（DIF）的计算：
     * DIF = EMA（12） - EMA（26）
     *
     * 离差平均值（DEA）计算：根据差离值计算其9日的EMA，即离差平均值，是所求的DEA值。、
     * 为了不与指标原名相混淆，此值又名DEA或DEM。
     * 今日DEA = （前一日DEA X 8/10 + 今日DIF X 2/10）
     *
     * 用（DIF-DEA）*2即为MACD柱状图。
     */
    public void macd() {
        int size = list.size();
        KeyLineItem kItem;
        KLineTechItem kLineTechItem;
        KLineTechItem preKLintTech = null;

        float EMA12 = 0;
        float EMA26 = 0;
        final int _E1=12, _E2=26, _EDEA=9;
        for (int i = 0; i < size; i++) {
            kItem = list.get(i);
            kLineTechItem = kLineTechParam.getTechItem(i);
            if(i==0){
                EMA12 = kItem.close;
                EMA26 = EMA12;
            }else{
                EMA12 = _calcEMA(EMA12, kItem.close, _E1);
                EMA26 = _calcEMA(EMA26, kItem.close, _E2);
                //差离值（DIF）
                kLineTechItem.dif = EMA12 - EMA26;
                kLineTechItem.dea = _calcEMA(preKLintTech.dea, kLineTechItem.dif, _EDEA);
                kLineTechItem.macd = (kLineTechItem.dif - kLineTechItem.dea) * 2;
            }
            preKLintTech = kLineTechItem;
        }
    }

    /**
     * ema0_ X (cycle_-1)/(cycle_+1) + close_ X 2/(cycle_+1)
     * @param ema0_
     * @param close_
     * @param cycle_
     * @return
     */
    public float _calcEMA(float ema0_, float close_, int cycle_) {
        int cycleDiv = cycle_ + 1;
        return (cycle_ - 1) * ema0_ / cycleDiv + close_ * 2 / cycleDiv;
    }



    /**
     * ③、相对强弱指标（ＲＳＩ）=（N日内上涨总幅度平均值/ N日内上涨总幅度和下跌总幅度平均值）X 100%
     *                    一般短期ＲＳＩ设Ｎ＝6,长期ＲＳＩ设Ｎ＝12。ＲＳＩ值永远在0-100之内变动。
     */
    public void rsi() {
        int size = list.size();
        final int E1=6, E2=12, E3=24;
        float sum1 = .0f, sum2 = .0f, sum3 = .0f, sum4 = .0f, sum5 = .0f, sum6 = .0f;
        float c, max, abs;

        KLineTechParam.RSIMaData itemR = kLineTechParam.new RSIMaData();
        KLineTechParam.RSIMaData prevR = kLineTechParam.new RSIMaData();
        KLineTechParam.RSIMaData tempR;
        KeyLineItem kLineItem;                  //k线实体
        KLineTechItem kLineTechItem;            //技术指标
        KLineTechItem preKLintTech = null;      //上日技术指标
        for (int i = 0; i < size; i++) {
            kLineItem = list.get(i);
            kLineTechItem = kLineTechParam.getTechItem(i);

            if (i == 0 && kLineItem.close_yestoday < Constants.EPSILON) {
                c = kLineItem.close * .1f;
            } else {
                c = kLineItem.close - kLineItem.close_yestoday;
            }
            max = Math.max(c, 0);  //某项上涨幅度
            abs = Math.abs(c);     //上涨下跌幅度绝对值

            //计算6日相对强弱指标
            sum1 += max;           //上涨幅度累加
            sum4 += abs;           //上涨下跌幅度累加
            if (i >= E1) {
                //
                sum1 = max + prevR.ma1 * (E1 - 1);
                itemR.ma1 = sum1 / E1;
                sum4 = abs + prevR.ma4 * (E1 - 1);
                itemR.ma4 = sum4 / E1;
            } else {
                //不足6日
                itemR.ma1 = sum1 / (i + 1);
                itemR.ma4 = sum4 / (i + 1);
            }

            //计算12日相对强弱指标
            sum2 += max;           //上涨幅度累加
            sum5 += abs;           //上涨下跌幅度累加
            if (i >= E2) {
                sum2 = max + prevR.ma2 * (E2 - 1);
                itemR.ma2 = sum2 / E2;
                sum5 = abs + prevR.ma5 * (E2 - 1);
                itemR.ma5 = sum5 / E2;
            } else {
                itemR.ma2 = sum2 / (i + 1);
                itemR.ma5 = sum5 / (i + 1);
            }

            //计算24日相对强弱指标
            sum3 += max;
            sum6 += abs;
            if (i >= E3) {
                sum3 = max + prevR.ma3 * (E3 - 1);
                itemR.ma3 = sum3 / E3;
                sum6 = abs + prevR.ma6 * (E3 - 1);
                itemR.ma6 = sum6 / E3;
            } else {
                itemR.ma3 = sum3 / (i + 1);
                itemR.ma6 = sum6 / (i + 1);
            }

            kLineTechItem.rsi1 = itemR.ma4 > 0 ? (itemR.ma1 / itemR.ma4) * 100 :
                    (preKLintTech == null ? .0f : preKLintTech.rsi1);
            kLineTechItem.rsi2 = itemR.ma5 > 0 ? (itemR.ma2 / itemR.ma5) * 100 :
                    (preKLintTech == null ? .0f : preKLintTech.rsi2);
            kLineTechItem.rsi3 = itemR.ma6 > 0 ? (itemR.ma3 / itemR.ma6) * 100 :
                    (preKLintTech == null ? .0f : preKLintTech.rsi3);

            tempR = prevR;
            prevR = itemR;
            itemR = tempR;

            preKLintTech = kLineTechItem;
        }

    }
}
