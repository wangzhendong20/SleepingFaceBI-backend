package com.dong.chart.api.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑请求
 *
 *  
 *   
 */
@Data
public class ChartEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;
    /**
     * 图表名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chatType;


    private static final long serialVersionUID = 1L;
}
