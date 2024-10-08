package com.dong.user.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.common.common.ErrorCode;
import com.dong.common.excption.BusinessException;
import com.dong.common.excption.ThrowUtils;
import com.dong.user.api.constant.CreditConstant;
import com.dong.user.api.model.entity.Credit;
import com.dong.user.service.CreditService;
import org.springframework.stereotype.Service;
import com.dong.user.mapper.CreditMapper;


@Service
public class CreditServiceImpl extends ServiceImpl<CreditMapper, Credit>
    implements CreditService {

    @Override
    public Long getCreditTotal(Long userId) {
        if (userId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        QueryWrapper<Credit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        Credit credit = this.getOne(queryWrapper);
        ThrowUtils.throwIf(credit == null, ErrorCode.NOT_FOUND_ERROR);
        return credit.getCreditTotal();
    }

    @Override
    public Boolean signUser(Long userId) {
        if (userId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        synchronized (userId.toString().intern()) {
            QueryWrapper<Credit> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userId", userId);
            Credit credit = this.getOne(queryWrapper);
            ThrowUtils.throwIf(credit == null, ErrorCode.NOT_FOUND_ERROR);
            //判断今天是否已经签过
            if (DateUtil.isSameDay(credit.getUpdateTime(), new DateTime())) {
                return false;
            }
            Long creditTotal = credit.getCreditTotal() + CreditConstant.CREDIT_DAILY;
            credit.setCreditTotal(creditTotal);
            //保持更新时间
            credit.setUpdateTime(null);
            return this.updateById(credit);
        }
    }

    @Override
    public Boolean updateCredits(Long userId, long credits) {
        if (userId == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        QueryWrapper<Credit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        Credit credit = this.getOne(queryWrapper);
        ThrowUtils.throwIf(credit == null, ErrorCode.NOT_FOUND_ERROR);
        Long creditTotal = credit.getCreditTotal();
        //积分不足时
        if (creditTotal+credits<0) return false;
        creditTotal =creditTotal + credits;
        credit.setCreditTotal(creditTotal);
        //保持更新时间
        credit.setUpdateTime(null);
        return this.updateById(credit);
    }

}




