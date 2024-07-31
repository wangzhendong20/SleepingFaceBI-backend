package com.dong.user.api;


public interface InnerCreditService{

    /**
     * 更新积分（内部方法） 正数为增加积分，负数为消耗积分
     * @param userId
     * @param credits
     * @return
     */
    Boolean updateCredits(Long userId,long credits);

}
