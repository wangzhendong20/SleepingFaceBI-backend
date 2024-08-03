package com.dong.user.api.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ak,sk视图（脱敏）
 *
 *
 *
 */
@Data
public class KeyRecordVO implements Serializable {

    /**
     * id
     */
    private Long id;


    private String accessKey;


    private String secretKey;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
