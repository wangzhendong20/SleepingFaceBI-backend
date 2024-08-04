package com.dong.user.dubbo;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dong.user.api.InnerKeyRecordService;
import com.dong.user.api.model.entity.KeyRecord;
import com.dong.user.api.model.vo.KeyRecordVO;
import com.dong.user.mapper.KeyRecordMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


@DubboService
@Service
@RequiredArgsConstructor
public class InnerKeyRecordServiceImpl implements InnerKeyRecordService {

    private final KeyRecordMapper keyRecordMapper;

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
    public KeyRecord getKeyRecord(String ak) {
        LambdaQueryWrapper<KeyRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KeyRecord::getAccessKey, ak);
        return keyRecordMapper.selectOne(queryWrapper);
    }
}
