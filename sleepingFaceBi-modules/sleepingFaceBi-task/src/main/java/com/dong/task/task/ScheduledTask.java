package com.dong.task.task;


import com.dong.common.constant.MqConstant;
import com.dong.common.mq.config.MqMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@EnableScheduling
@Slf4j
public class ScheduledTask {

    @Resource
    private MqMessageProducer mqMessageProducer;

    @Scheduled(cron = "0 0 2 * * ?")
    public void handle() {
        log.info("retryFailedJob start");
        mqMessageProducer.sendMessage(MqConstant.EXCHANGE_DIRECT_TASK,MqConstant.ROUTING_TASK_8,"");
        log.info("retryFailedJob end");
    }

}
