package com.dong.user.dubbo;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.dong.common.common.ErrorCode;
import com.dong.common.excption.BusinessException;
import com.dong.user.api.InnerUserService;
import com.dong.user.api.constant.UserConstant;
import com.dong.user.api.model.entity.User;
import com.dong.user.api.model.enums.UserRoleEnum;
import com.dong.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

/**
 * @author dong
 * @version 1.0
 * @project sleepingFaceBi-cloud
 * @description 远程user业务服务
 * @date 2023/7/27 16:12:24
 */
@DubboService
@Service
@RequiredArgsConstructor
public class InnerUserServiceImpl implements InnerUserService {

    private final UserMapper userMapper;

    @Override
    public User getLoginUser() {
        // 先判断是否已登录
        SaSession saSession = StpUtil.getTokenSession();
        // 先判断是否已登录
        Object userObj = saSession.get(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean isAdmin() {
        // 仅管理员可查询
        SaSession saSession = StpUtil.getTokenSession();
        // 先判断是否已登录
        Object userObj = saSession.get(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}
