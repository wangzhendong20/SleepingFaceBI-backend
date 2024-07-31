package com.dong.chart.controller;

import com.dong.chart.api.service.Chart1Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
@Validated
@RequestMapping("/chart1")
public class Chart1Controller {

    @Resource
    Chart1Service chart1Service;

    @GetMapping("/get")
    public String getString(String str){
        return chart1Service.getChart(str);
    }
}
