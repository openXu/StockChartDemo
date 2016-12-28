package com.openxu.chart;

import android.app.Activity;
import android.os.Bundle;

import com.openxu.chartlib.manager.MinuteManager;

public class KlineActivity extends Activity {
    private MinuteManager chartManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minute);
        //初始化股票图表管理器，参数为当前activity对象、股票代码
        chartManager=new MinuteManager(this, "sz000002");
        chartManager.show();
    }
}
