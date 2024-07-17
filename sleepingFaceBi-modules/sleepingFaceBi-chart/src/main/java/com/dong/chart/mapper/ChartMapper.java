package com.dong.chart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dong.chart.api.model.entity.Chart;

import java.util.List;
import java.util.Map;

/**
* @author dong
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2023-05-26 23:18:07
* @Entity com.dong.text.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {
    List<Map<String,Object>> queryChartData(String querySql);
}




