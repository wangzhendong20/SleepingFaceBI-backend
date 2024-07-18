package com.dong.data.api.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *
 */
@Data
public class DataAddRequest implements Serializable {

    /**
     * 笔记名称
     */
    private String name;

    private String aim;

    /**
     * 文本类型
     */
    private String textType;



    private static final long serialVersionUID = 1L;
}
