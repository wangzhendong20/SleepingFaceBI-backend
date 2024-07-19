package com.dong.text.mq.TextMq;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dong.common.ai.config.QianWenText;
import com.dong.common.common.ErrorCode;
import com.dong.common.configs.config.GuavaRetryConfig;
import com.dong.common.configs.config.RetryConfig;
import com.dong.common.constant.MqConstant;
import com.dong.common.excption.BusinessException;
import com.dong.text.api.constant.TextConstant;
import com.dong.text.api.model.entity.TextRecord;
import com.dong.text.api.model.entity.TextTask;
import com.dong.text.service.TextRecordService;
import com.dong.text.service.TextTaskService;
import com.github.rholder.retry.*;
import com.google.common.base.Predicates;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 文本转换消费者队列
 */
@Component
@Slf4j
public class TextMessageConsumer {

    @Resource
    private TextTaskService textTaskService;

    @Resource
    private TextRecordService textRecordService;

    @Resource
    private RetryConfig retryConfig;
    @Resource
    private GuavaRetryConfig guavaRetryConfig;

    @Resource
    private QianWenText qianWenText;

    @SneakyThrows
    @RabbitListener(queues = {MqConstant.TEXT_QUEUE_NAME},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        log.warn("接收到队列信息，receiveMessage={}=======================================",message);
        if (StringUtils.isBlank(message)){
            //消息为空，消息拒绝，不重复发送，不重新放入队列
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }

        long textTaskId = Long.parseLong(message);
        List<TextRecord> textRecordList = textRecordService.list(new QueryWrapper<TextRecord>().eq("textTaskId", textTaskId));
        //todo 可以将记录表字段增加一个type 减少一次查询表
        TextTask textTask = textTaskService.getById(textTaskId);
        if (textRecordList == null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"文本为空");
        }
        //修改表状态为执行中，执行成功修改为“已完成”；执行失败修改为“失败”
        TextTask updateTask = new TextTask();
        updateTask.setId(textTaskId);
        updateTask.setStatus(TextConstant.RUNNING);
        boolean updateResult = textTaskService.updateById(updateTask);
        if (!updateResult){
            textTaskService.handleTextTaskUpdateError(textTaskId,"更新图表执行状态失败");
            return;
        }
        //调用AI

        for (TextRecord textRecord : textRecordList) {
            String result = null;
            //队列重新消费时，不在重新生成已经生成过的数据
            if (textRecord.getGenTextContent() != null) continue;

            // Guava Retry
            Callable<String> callable = () -> {
                return qianWenText.callWithMessage(textRecordService.buildUserInput(textRecord,textTask.getTextType()).toString()); // 业务逻辑
            };
            Retryer<String> retryer = guavaRetryConfig.retryer();
            try {
                result = retryer.call(callable); // 执行
            } catch (Exception e) { // 重试次数超过阈值或被强制中断
                channel.basicNack(deliveryTag,false,true);
                log.warn("信息放入队列{}", DateTime.now());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 服务错误");
            }

//            // Spring Retry
//            RetryTemplate retryTemplate = retryConfig.retryTemplate();
//            result = retryTemplate.execute(retryCallback -> {
//                return qianWenText.callWithMessage(textRecordService.buildUserInput(textRecord,textTask.getTextType()).toString()); // 业务逻辑
//            }, recoveryCallback -> {
//                channel.basicNack(deliveryTag,false,true);
//                log.warn("信息放入队列{}", DateTime.now());
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 服务错误");
//            });

//            try {
//                result = qianWenText.callWithMessage(textRecordService.buildUserInput(textRecord,textTask.getTextType()).toString());
//            } catch (Exception e) {
//                channel.basicNack(deliveryTag,false,true);
//                log.warn("信息放入队列{}", DateTime.now());
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 服务错误");
//            }
            textRecord.setGenTextContent(result);
            textRecord.setStatus(TextConstant.SUCCEED);
            boolean updateById = textRecordService.updateById(textRecord);
            if (!updateById){
                log.warn("AI生成错误，重新放入队列");
                channel.basicNack(deliveryTag,false,true);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"保存失败");
            }
        }
        //将记录表中已经生成好的内容合并存入任务表
        StringBuilder stringBuilder = new StringBuilder();
        textRecordList.forEach(textRecord1 -> {
            stringBuilder.append(textRecord1.getGenTextContent()).append('\n');
        });
        TextTask textTask1 = new TextTask();
        textTask1.setId(textTaskId);
        textTask1.setGenTextContent(stringBuilder.toString());
        textTask1.setStatus(TextConstant.SUCCEED);
        boolean save = textTaskService.updateById(textTask1);
        if (!save){
            channel.basicNack(deliveryTag,false,true);
            textTaskService.handleTextTaskUpdateError(textTask.getId(), "ai返回文本任务保存失败");
        }

        //消息确认
        channel.basicAck(deliveryTag,false);
    }


}
