package com.openxu.chart;

import android.app.Activity;
import android.os.Bundle;

import com.openxu.chartlib.StockChartManager;

public class ChartDemoActivity extends Activity {
    private StockChartManager manager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_demo);
        //初始化股票图表管理器，参数为当前activity对象、股票代码
        manager=new StockChartManager(this, "sz000002");
        manager.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.destroryRequest();
    }
}
