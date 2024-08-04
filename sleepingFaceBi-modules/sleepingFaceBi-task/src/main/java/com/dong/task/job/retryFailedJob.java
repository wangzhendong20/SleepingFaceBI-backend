package com.dong.task.job;

import com.dong.common.constant.MqConstant;
import com.dong.common.mq.config.MqMessageProducer;
import com.xxl.job.core.handler.annotation.XxlJob;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Component
@Slf4j
public class retryFailedJob {

    @Resource
    private MqMessageProducer mqMessageProducer;

    /**
     * 每天两点定时任务，将未能处理的任务重新放入队列
     */
    @XxlJob("retryFailedJob")
    public void handle() {
        log.info("retryFailedJob start");
        mqMessageProducer.sendMessage(MqConstant.EXCHANGE_DIRECT_TASK,MqConstant.ROUTING_TASK_8,"");
        log.info("retryFailedJob end");
    }
}
