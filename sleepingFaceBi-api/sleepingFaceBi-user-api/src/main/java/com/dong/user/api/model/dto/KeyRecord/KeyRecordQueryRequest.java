package com.dong.user.api.model.dto.KeyRecord;

import com.dong.common.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求
 *
 *
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class KeyRecordQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;


    /**
     * 用户昵称
     */
    private Long userId;


    private static final long serialVersionUID = 1L;
}
