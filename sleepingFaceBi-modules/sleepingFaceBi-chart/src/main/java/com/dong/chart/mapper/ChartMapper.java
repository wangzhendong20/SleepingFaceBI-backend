package com.dong.chart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dong.chart.api.model.entity.Chart;

import java.util.List;
import java.util.Map;


public interface ChartMapper extends BaseMapper<Chart> {
    List<Map<String,Object>> queryChartData(String querySql);
}




