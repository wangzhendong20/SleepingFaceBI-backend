package com.dong.user.api;

import com.dong.user.api.model.entity.User;


/**
 * 远程用户服务 user
 *
 */
public interface InnerUserService {

    /**
     * 获取当前登录用户
     *
     * @return
     */
    User getLoginUser();


    /**
     * 是否为管理员
     *
     * @return
     */
    boolean isAdmin();

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);



}
