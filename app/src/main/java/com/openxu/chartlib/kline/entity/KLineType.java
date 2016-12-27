package com.openxu.chartlib.kline.entity;

/**
 * author : openXu
 * create at : 2016/11/24 16:07
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KLineType
 * version : 1.0
 * class describe：技术指标类型
 */
public enum  KLineType {

    /**
     * 成交量指标
     */
    VOL,


    /**
     * 指数平滑移动平均线
     */
    MACD,


    /**
     * 随机指标,KDJ指标是三条曲线
     */
    KDJ,


    /**
     * 相对强弱指标（ Relative Strength Index ）
     * 由 Wells Wider 创制的一种通过特定时期内股价的变动情况计算市场买卖力量对比,
     * 来判断股价内部本质强弱、推测价格未来的变动方向的技术指标。
     * 公式的推导过程如下所示：
     * 相对强弱指标（ＲＳＩ）=（N日内上涨总幅度平均值/ N日内上涨总幅度和下跌总幅度平均值）X 100%
     　　                 一般短期ＲＳＩ设Ｎ＝6,长期ＲＳＩ设Ｎ＝12。ＲＳＩ值永远在0-100之内变动。
     *
     */
    RSI






}
