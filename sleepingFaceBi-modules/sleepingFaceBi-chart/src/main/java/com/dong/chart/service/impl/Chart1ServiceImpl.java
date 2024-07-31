package com.dong.chart.service.impl;

import com.dong.chart.api.service.Chart1Service;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;



@DubboService
@Service
public class Chart1ServiceImpl implements Chart1Service {
    @Override
    public String getChart(String chart) {
        return "im chart push"+chart;
    }
}
