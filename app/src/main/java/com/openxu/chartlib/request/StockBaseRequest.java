package com.openxu.chartlib.request;

import android.util.Log;

import com.openxu.chartlib.bean.StockBaseInfo;
import com.openxu.chartlib.bean.StockBaseResult;
import com.openxu.chartlib.callback.BasicResultCallBack;
import com.openxu.chartlib.config.Constants;
import com.openxu.chartlib.testdata.TestData;
import com.openxu.chartlib.utils.JSONUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.HashMap;

import okhttp3.Call;

/**
 * author : openXu
 * create at : 2016/07/8 12:40
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : StockChart
 * class name : StockBaseRequest
 * version : 1.0
 * class describe：基本面数据请求处理类
 */
public class StockBaseRequest {
    private String TAG = "BasicDataRequest";
    BasicResultCallBack callBack=null;

    private String symbol=null;
    private StockBaseInfo stockBaseInfo =null;

    public StockBaseRequest(String symbol, BasicResultCallBack callBack){
        this.symbol = symbol;
        this.callBack=callBack;
    }
    public void requestBasicData(){
        HashMap<String, String> p = new HashMap<String,String>();
        p.put("symbol", symbol);
        Log.v(TAG, "请求基本面数据："+ Constants.BasicDataRequestUrl+"?symbol="+symbol);
        OkHttpUtils.post()
                .url(Constants.BasicDataRequestUrl)
                .tag(TAG)
                //TODO 参数
//                .params(p)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        callBack.callback(null);
                        e.printStackTrace();
                    }
                    @Override
                    public void onResponse(String response) {
                        //TODO 如果使用真实接口，请注释下面一段代码
                        //使用测试数据
                        response= TestData.StockBaseData;
                        Log.i(TAG, "基本面数据："+response);
                        if (response != null) {
                            StockBaseResult result = (StockBaseResult) JSONUtil.jsonToBean(response,StockBaseResult.class);

                            if (result != null&&result.getData()!=null) {
                                stockBaseInfo = result.getData();
                                callBack.callback(stockBaseInfo);
//                        chartManager.show(basicData);
                            }else{
                                callBack.callback(null);
                            }

                        }
                    }

                });
    }
}
