package com.madou.common.ai.config;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageManager;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QianWenChart {
    public String callWithMessage(String message) throws NoApiKeyException, ApiException, InputRequiredException
    {
        Constants.apiKey="sk-8152bde853e3482ab1a0b5d2a02ab70f";
        Generation gen = new Generation();
        MessageManager msgManager = new MessageManager(10);
        Message systemMsg =
                Message.builder().role(Role.SYSTEM.getValue()).content("你是一个数据分析和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                        "分析需求：\n" +
                        "{数据分析的需求或者目标}\n" +
                        "原始数据：\n" +
                        "{csv格式的原始数据}\n" +
                        "请根据这两部分内容，按照以下指定格式生成内容（此处不要输出多余的开头、结尾、注释）\n" +
                        "【【【【【\n" +
                        "{前端Echarts V5的option配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
                        "【【【【【\n" +
                        "{明确的数据分析结论、越详细越好，不要生成多余的注释}").build();
        Message userMsg = Message.builder().role(Role.USER.getValue()).content(message).build();
        msgManager.add(systemMsg);
        msgManager.add(userMsg);
        QwenParam param =
                QwenParam.builder().model(Generation.Models.QWEN_TURBO).messages(msgManager.get())
                        .resultFormat(QwenParam.ResultFormat.MESSAGE)
                        .topP(0.8)
                        .enableSearch(true)
                        .build();
        GenerationResult result = gen.call(param);
//        log.info("千问api回答的话语为：{}",result);
//        System.out.println(result);
        if (result == null){
            throw new RuntimeException("AI 响应错误");
        }
        List<GenerationOutput.Choice> choices = result.getOutput().getChoices();
        String resContent = "";
        if (choices != null && !choices.isEmpty()) {
            resContent = choices.get(0).getMessage().getContent();
        } else {
            throw new RuntimeException("AI 响应错误");
        }

        return resContent;
    }
}
