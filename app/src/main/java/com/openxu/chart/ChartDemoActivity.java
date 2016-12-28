package com.openxu.chart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.openxu.chartlib.manager.BaseManager;

public class ChartDemoActivity extends Activity {
    private BaseManager chartManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_demo);
        findViewById(R.id.btn_minute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChartDemoActivity.this, MinuteActivity.class));
            }
        });
        findViewById(R.id.btn_kline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChartDemoActivity.this, KlineActivity.class));
            }
        });
    }
}
