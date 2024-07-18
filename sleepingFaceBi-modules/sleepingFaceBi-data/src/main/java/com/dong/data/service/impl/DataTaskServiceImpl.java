package com.dong.data.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.common.common.ErrorCode;
import com.dong.common.excption.ThrowUtils;
import com.dong.common.utils.ExcelUtils;
import com.dong.common.utils.TxtUtils;
import com.dong.data.api.constant.DataConstant;
import com.dong.data.api.model.dto.GenDataTaskByAiRequest;
import com.dong.data.api.model.entity.DataRecord;
import com.dong.data.api.model.entity.DataTask;
import com.dong.data.mapper.DataTaskMapper;
import com.dong.data.service.DataRecordService;
import com.dong.data.service.DataTaskService;
import com.dong.user.api.InnerCreditService;
import com.dong.user.api.constant.CreditConstant;
import com.dong.user.api.model.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* @author dong
* @description 针对表【Data_task(文本任务表)】的数据库操作Service实现
* @createDate 2023-07-12 20:32:15
*/
@Service
public class DataTaskServiceImpl extends ServiceImpl<DataTaskMapper, DataTask>
    implements DataTaskService {

    @DubboReference
    private InnerCreditService creditService;

    @Resource
    private DataRecordService dataRecordService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DataTask getDataTask(MultipartFile multipartFile, GenDataTaskByAiRequest genDataTaskByAiRequest, User loginUser) {
        String DataTaskType = genDataTaskByAiRequest.getTextType();
        String name = genDataTaskByAiRequest.getName();
        String aim = genDataTaskByAiRequest.getAim();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(DataTaskType), ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length()>=100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024*1024;
        ThrowUtils.throwIf(size>ONE_MB,ErrorCode.PARAMS_ERROR,"文件超过1MB");
        ThrowUtils.throwIf(size==0,ErrorCode.PARAMS_ERROR,"文件为空");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("xlsx","csv");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀名非法");

        //消耗积分
        Boolean creditResult = creditService.updateCredits(loginUser.getId(), CreditConstant.CREDIT_CHART_SUCCESS);
        ThrowUtils.throwIf(!creditResult,ErrorCode.OPERATION_ERROR,"你的积分不足");

        //保存数据库 wait
        //保存任务进数据库
        DataTask DataTask = new DataTask();
        DataTask.setTextType(DataTaskType);
        DataTask.setName(name);
        DataTask.setUserId(loginUser.getId());
        DataTask.setStatus(DataConstant.WAIT);
        DataTask.setAim(aim);
        boolean saveResult = this.save(DataTask);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"文本任务保存失败");

        Long taskId = DataTask.getId();

        ArrayList<String> DataContentList = new ArrayList<>();
        // 压缩后的数据

        // 增加txt,doc,docx,md文件类型判断
        if (suffix.equals("xlsx")) {
            DataContentList.add(ExcelUtils.excelToCsv(multipartFile));
        }

        ThrowUtils.throwIf(DataContentList.size() ==0,ErrorCode.PARAMS_ERROR,"文件为空");

        //将分割的内容保存入记录表
        ArrayList<DataRecord> taskArrayList = new ArrayList<>();
        DataContentList.forEach(DataContent ->{
            DataRecord DataRecord = new DataRecord();
            DataRecord.setTextTaskId(taskId);
            DataRecord.setTextContent(DataContent);
            DataRecord.setStatus(DataConstant.WAIT);
            taskArrayList.add(DataRecord);
        });

        boolean batchResult = dataRecordService.saveBatch(taskArrayList);
        ThrowUtils.throwIf(!batchResult,ErrorCode.SYSTEM_ERROR,"文本记录保存失败");

        return DataTask;
    }

    @Override
    public void handleDataTaskUpdateError(Long DataTaskId, String execMessage) {
        DataTask updateDataTaskResult = new DataTask();
        updateDataTaskResult.setStatus(DataConstant.FAILED);
        updateDataTaskResult.setId(DataTaskId);
        updateDataTaskResult.setExecMessage(execMessage);
        boolean updateResult = this.updateById(updateDataTaskResult);
        if (!updateResult){
            log.error("更新文本失败状态失败"+DataTaskId+","+execMessage);
        }
    }


}




