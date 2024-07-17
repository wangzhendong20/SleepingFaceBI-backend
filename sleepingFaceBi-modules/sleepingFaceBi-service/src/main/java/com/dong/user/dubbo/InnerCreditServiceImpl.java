package com.dong.user.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dong.common.common.ErrorCode;
import com.dong.common.excption.BusinessException;
import com.dong.common.excption.ThrowUtils;
import com.dong.user.api.InnerCreditService;
import com.dong.user.api.model.entity.Credit;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import com.dong.user.mapper.CreditMapper;

/**
 * @author dong
 * @version 1.0
 * @project sleepingFaceBi-cloud
 * @description 远程积分业务实现
 * @date 2023/7/27 16:48:13
 */

@DubboService
@Service
@RequiredArgsConstructor
public class InnerCreditServiceImpl implements InnerCreditService {

    private final CreditMapper creditMapper;

    @Override
    public Boolean updateCredits(Long userId, long credits) {
        if (userId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        QueryWrapper<Credit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        Credit credit = creditMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(credit == null, ErrorCode.NOT_FOUND_ERROR);
        Long creditTotal = credit.getCreditTotal();
        //积分不足时
        if (creditTotal+credits<0) return false;
        creditTotal =creditTotal + credits;
        credit.setCreditTotal(creditTotal);
        //保持更新时间
        credit.setUpdateTime(null);
        return creditMapper.updateById(credit) == 1;
    }
}
