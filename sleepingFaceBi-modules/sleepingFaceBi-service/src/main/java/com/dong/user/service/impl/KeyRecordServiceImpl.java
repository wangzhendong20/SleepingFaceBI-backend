package com.dong.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.common.common.ErrorCode;
import com.dong.common.constant.CommonConstant;
import com.dong.common.excption.BusinessException;
import com.dong.common.utils.SqlUtils;
import com.dong.user.api.model.dto.KeyRecord.KeyRecordQueryRequest;
import com.dong.user.api.model.dto.user.UserQueryRequest;
import com.dong.user.api.model.entity.KeyRecord;
import com.dong.user.api.model.entity.User;
import com.dong.user.api.model.vo.KeyRecordVO;
import com.dong.user.api.model.vo.UserVO;
import com.dong.user.mapper.KeyRecordMapper;
import com.dong.user.service.KeyRecordService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 39112
* @description 针对表【key_record(API签名认证表)】的数据库操作Service实现
* @createDate 2024-08-03 16:56:27
*/
@Service
public class KeyRecordServiceImpl extends ServiceImpl<KeyRecordMapper, KeyRecord>
    implements KeyRecordService {

    @Override
    public KeyRecordVO getKeyRecordVO(KeyRecord keyRecord) {
        if (keyRecord == null) {
            return null;
        }
        KeyRecordVO keyRecordVO = new KeyRecordVO();
        BeanUtils.copyProperties(keyRecord, keyRecordVO);
        return keyRecordVO;
    }

    @Override
    public List<KeyRecordVO> getKeyRecordVO(List<KeyRecord> keysList) {
        if (CollectionUtils.isEmpty(keysList)) {
            return new ArrayList<>();
        }
        return keysList.stream().map(this::getKeyRecordVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<KeyRecord> getQueryWrapper(KeyRecordQueryRequest keyRecordQueryRequest) {
        if (keyRecordQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = keyRecordQueryRequest.getId();
        Long userId = keyRecordQueryRequest.getUserId();
        String sortField = keyRecordQueryRequest.getSortField();
        String sortOrder = keyRecordQueryRequest.getSortOrder();
        QueryWrapper<KeyRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public KeyRecord getKeyRecord(String ak) {
        LambdaQueryWrapper<KeyRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KeyRecord::getAccessKey, ak);
        return this.getOne(queryWrapper);
    }
}




