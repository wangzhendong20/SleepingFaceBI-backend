package com.dong.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.user.api.model.dto.KeyRecord.KeyRecordQueryRequest;
import com.dong.user.api.model.dto.user.UserQueryRequest;
import com.dong.user.api.model.entity.KeyRecord;
import com.dong.user.api.model.entity.User;
import com.dong.user.api.model.vo.KeyRecordVO;
import com.dong.user.api.model.vo.UserVO;

import java.util.List;

/**
* @author 39112
* @description 针对表【key_record(API签名认证表)】的数据库操作Service
* @createDate 2024-08-03 16:56:27
*/
public interface KeyRecordService extends IService<KeyRecord> {

    KeyRecordVO getKeyRecordVO(KeyRecord keyRecord);

    List<KeyRecordVO> getKeyRecordVO(List<KeyRecord> keysList);

    /**
     * 获取查询条件
     *
     * @return
     */
    QueryWrapper<KeyRecord> getQueryWrapper(KeyRecordQueryRequest keyRecordQueryRequest);

    KeyRecord getKeyRecord(String ak);
}
