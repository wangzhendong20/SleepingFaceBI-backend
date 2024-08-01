package com.dong.text.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.common.common.ErrorCode;
import com.dong.common.excption.ThrowUtils;
import com.dong.text.api.constant.TextConstant;
import com.dong.text.api.model.dto.GenTextTaskByAiRequest;
import com.dong.text.api.model.entity.TextRecord;
import com.dong.text.api.model.entity.TextTask;
import com.dong.text.mapper.TextTaskMapper;
import com.dong.text.readerStrategy.FileProcessor;
import com.dong.text.service.TextRecordService;
import com.dong.text.service.TextTaskService;
import com.dong.user.api.InnerCreditService;
import com.dong.user.api.constant.CreditConstant;
import com.dong.user.api.model.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
public class TextTaskServiceImpl extends ServiceImpl<TextTaskMapper, TextTask>
    implements TextTaskService {

    @DubboReference
    private InnerCreditService creditService;

    @Resource
    private TextRecordService textRecordService;
    @Resource
    private FileProcessor fileProcessor;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public TextTask getTextTask(MultipartFile multipartFile, GenTextTaskByAiRequest genTextTaskByAiRequest, User loginUser) {
        String textTaskType = genTextTaskByAiRequest.getTextType();
        String name = genTextTaskByAiRequest.getName();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(textTaskType), ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length()>=100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024*1024;
        ThrowUtils.throwIf(size>ONE_MB,ErrorCode.PARAMS_ERROR,"文件超过1MB");
        ThrowUtils.throwIf(size==0,ErrorCode.PARAMS_ERROR,"文件为空");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("txt","doc","docx","md");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀名非法");

        //消耗积分
        Boolean creditResult = creditService.updateCredits(loginUser.getId(), CreditConstant.CREDIT_CHART_SUCCESS);
        ThrowUtils.throwIf(!creditResult,ErrorCode.OPERATION_ERROR,"你的积分不足");

        //保存数据库 wait
        //保存任务进数据库
        TextTask textTask = new TextTask();
        textTask.setTextType(textTaskType);
        textTask.setName(name);
        textTask.setUserId(loginUser.getId());
        textTask.setStatus(TextConstant.WAIT);
        boolean saveResult = this.save(textTask);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"文本任务保存失败");

        Long taskId = textTask.getId();

        ArrayList<String> textContentList = new ArrayList<>();
        // 压缩后的数据

        // 增加txt,doc,docx,md文件类型判断
//        if (suffix.equals("txt") || suffix.equals("md")) {
//            textContentList = TxtUtils.readerFile(multipartFile);
//        } else if (suffix.equals("doc")) {
//            textContentList = TxtUtils.readerDocxFile(multipartFile);
//        } else if (suffix.equals("docx")) {
//            textContentList = TxtUtils.readerDocxFile(multipartFile);
//        }
        try {
            textContentList = fileProcessor.processFile(suffix, multipartFile);
            // 处理读取到的文件内容
        } catch (IOException e) {
            ThrowUtils.throwIf(true,ErrorCode.SYSTEM_ERROR,"文件读取失败");
        }


        ThrowUtils.throwIf(textContentList.size() ==0,ErrorCode.PARAMS_ERROR,"文件为空");

        //将分割的内容保存入记录表
        ArrayList<TextRecord> taskArrayList = new ArrayList<>();
        textContentList.forEach(textContent ->{
            TextRecord textRecord = new TextRecord();
            textRecord.setTextTaskId(taskId);
            textRecord.setTextContent(textContent);
            textRecord.setStatus(TextConstant.WAIT);
            taskArrayList.add(textRecord);
        });

        boolean batchResult = textRecordService.saveBatch(taskArrayList);
        ThrowUtils.throwIf(!batchResult,ErrorCode.SYSTEM_ERROR,"文本记录保存失败");

        return textTask;
    }

    @Override
    public void handleTextTaskUpdateError(Long textTaskId, String execMessage) {
        TextTask updateTextTaskResult = new TextTask();
        updateTextTaskResult.setStatus(TextConstant.FAILED);
        updateTextTaskResult.setId(textTaskId);
        updateTextTaskResult.setExecMessage(execMessage);
        boolean updateResult = this.updateById(updateTextTaskResult);
        if (!updateResult){
            log.error("更新文本失败状态失败"+textTaskId+","+execMessage);
        }
    }


}




