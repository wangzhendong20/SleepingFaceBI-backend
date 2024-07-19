package com.dong.data.mq.DataMq;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dong.common.ai.config.QianWenData;
import com.dong.common.common.ErrorCode;
import com.dong.common.configs.config.GuavaRetryConfig;
import com.dong.common.constant.MqConstant;
import com.dong.common.excption.BusinessException;
import com.dong.data.api.constant.DataConstant;
import com.dong.data.api.model.entity.DataRecord;
import com.dong.data.api.model.entity.DataTask;
import com.dong.data.service.DataRecordService;
import com.dong.data.service.DataTaskService;
import com.github.rholder.retry.Retryer;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 文本转换消费者队列
 */
@Component
@Slf4j
public class DataMessageConsumerForm {

    @Resource
    private DataTaskService DataTaskService;

    @Resource
    private DataRecordService DataRecordService;

    @Resource
    private QianWenData qianWenData;
    @Resource
    private GuavaRetryConfig guavaRetryConfig;

    @SneakyThrows
    @RabbitListener(queues = {MqConstant.DATA_FORM_QUEUE_NAME},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        log.warn("接收到队列信息，receiveMessage={}=======================================",message);
        if (StringUtils.isBlank(message)){
            //消息为空，消息拒绝，不重复发送，不重新放入队列
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }

        long DataTaskId = Long.parseLong(message);
        List<DataRecord> DataRecordList = DataRecordService.list(new QueryWrapper<DataRecord>().eq("DataTaskId", DataTaskId));
        //todo 可以将记录表字段增加一个type 减少一次查询表
        DataTask DataTask = DataTaskService.getById(DataTaskId);
        if (DataRecordList == null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"文本为空");
        }
        //修改表状态为执行中，执行成功修改为“已完成”；执行失败修改为“失败”
        DataTask updateTask = new DataTask();
        updateTask.setId(DataTaskId);
        updateTask.setStatus(DataConstant.RUNNING);
        boolean updateResult = DataTaskService.updateById(updateTask);
        if (!updateResult){
            DataTaskService.handleDataTaskUpdateError(DataTaskId,"更新图表执行状态失败");
            return;
        }
        //调用AI

        for (DataRecord DataRecord : DataRecordList) {
            String result = null;
            //队列重新消费时，不在重新生成已经生成过的数据
            if (DataRecord.getGenTextContent() != null) continue;
            // Guava Retry
            Callable<String> callable = () -> {
                return qianWenData.callWithMessageForm(DataRecordService.buildUserInput(DataRecord,DataTask.getTextType(),DataTask.getAim()).toString());
            };
            Retryer<String> retryer = guavaRetryConfig.retryer();
            try {
                result = retryer.call(callable); // 执行
            } catch (Exception e) { // 重试次数超过阈值或被强制中断
                channel.basicNack(deliveryTag,false,true);
                log.warn("信息放入队列{}", DateTime.now());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 服务错误");
            }
//            try {
//                result = qianWenData.callWithMessageForm(DataRecordService.buildUserInput(DataRecord,DataTask.getTextType(),DataTask.getAim()).toString());
//            } catch (Exception e) {
//                channel.basicNack(deliveryTag,false,true);
//                log.warn("信息放入队列{}", DateTime.now());
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 服务错误");
//            }
            DataRecord.setGenTextContent(result);
            DataRecord.setStatus(DataConstant.SUCCEED);
            boolean updateById = DataRecordService.updateById(DataRecord);
            if (!updateById){
                log.warn("AI生成错误，重新放入队列");
                channel.basicNack(deliveryTag,false,true);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"保存失败");
            }
        }
        //将记录表中已经生成好的内容合并存入任务表
        StringBuilder stringBuilder = new StringBuilder();
        DataRecordList.forEach(DataRecord1 -> {
            stringBuilder.append(DataRecord1.getGenTextContent()).append('\n');
        });
        DataTask DataTask1 = new DataTask();
        DataTask1.setId(DataTaskId);
        DataTask1.setGenTextContent(stringBuilder.toString());
        DataTask1.setStatus(DataConstant.SUCCEED);
        boolean save = DataTaskService.updateById(DataTask1);
        if (!save){
            channel.basicNack(deliveryTag,false,true);
            DataTaskService.handleDataTaskUpdateError(DataTask.getId(), "ai返回文本任务保存失败");
        }

        //消息确认
        channel.basicAck(deliveryTag,false);
    }
}
