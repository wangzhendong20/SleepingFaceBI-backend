package com.dong.data.api.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文本生成请求
 *
 */
@Data
public class GenDataTaskByAiRequest implements Serializable {

    /**
     * 数据名称
     */
    private String name;

    /**
     * 目标
     */
    private String aim;

    /**
     * 文本类型
     */
    private String textType;



    private static final long serialVersionUID = 1L;
}
