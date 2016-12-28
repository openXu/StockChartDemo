package com.openxu.chartlib.bean;

import java.io.Serializable;
import java.util.List;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : KLineTechParam
 * version : 1.0
 * class describe：技术指标类
 */
public class KLineTechParam implements Serializable{


    public List<KLineTechItem> listParams;

    public KLineTechItem getTechItem(int index) {
        int size = listParams.size();
        KLineTechItem itemT = null;
        if(index < size) {
            itemT = listParams.get(index);
        } else {
            while (index >= size) {
                itemT = new KLineTechItem();
                listParams.add(itemT);
                size++;
            }
        }
        return itemT;
    }

    public class RSIMaData {
        public float ma1 = .0f;
        public float ma2 = .0f;
        public float ma3 = .0f;
        public float ma4 = .0f;
        public float ma5 = .0f;
        public float ma6 = .0f;
    }


    private boolean isKDJOL=false;
    private boolean isMACDOL=false;
    private boolean isRSIOL=false;


    public void resetTecnology(){
        isKDJOL=false;
        isMACDOL=false;
        isRSIOL=false;
    }
    public boolean isKDJOL() {
        return isKDJOL;
    }

    public void setKDJOL(boolean KDJOL) {
        isKDJOL = KDJOL;
    }

    public boolean isMACDOL() {
        return isMACDOL;
    }

    public void setMACDOL(boolean MACDOL) {
        isMACDOL = MACDOL;
    }

    public boolean isRSIOL() {
        return isRSIOL;
    }

    public void setRSIOL(boolean RSIOL) {
        isRSIOL = RSIOL;
    }
}
