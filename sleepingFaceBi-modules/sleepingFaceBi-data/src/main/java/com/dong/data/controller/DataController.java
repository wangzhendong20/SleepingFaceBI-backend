package com.dong.data.controller;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dong.common.ai.config.QianWenData;
import com.dong.common.annotation.AuthCheck;
import com.dong.common.common.BaseResponse;
import com.dong.common.common.DeleteRequest;
import com.dong.common.common.ErrorCode;
import com.dong.common.common.ResultUtils;
import com.dong.common.constant.CommonConstant;
import com.dong.common.constant.MqConstant;
import com.dong.common.excption.BusinessException;
import com.dong.common.excption.ThrowUtils;
import com.dong.common.model.vo.AiResponse;
import com.dong.common.mq.config.MqMessageProducer;
import com.dong.common.utils.SqlUtils;
import com.dong.data.service.DataRecordService;
import com.dong.data.service.DataTaskService;
import com.dong.data.api.constant.DataConstant;
import com.dong.data.api.model.dto.*;
import com.dong.data.api.model.entity.DataRecord;
import com.dong.data.api.model.entity.DataTask;
import com.dong.data.api.model.vo.DataTaskVO;
import com.dong.user.api.InnerCreditService;
import com.dong.user.api.InnerUserService;
import com.dong.user.api.constant.UserConstant;
import com.dong.user.api.model.entity.User;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Base64;
import java.util.List;

/**
 * 格式转换接口
 *
 */
@RestController
@RequestMapping("/data")
@Slf4j
public class DataController {

    @Resource
    private DataTaskService dataTaskService;

    @Resource
    private DataRecordService dataRecordService;
    @DubboReference
    private InnerUserService userService;

    @Resource
    private QianWenData qianWenData;
    @DubboReference
    private InnerCreditService creditService;

    @Resource
    private MqMessageProducer mqMessageProducer;
    private final static Gson GSON = new Gson();

    // region 增删改查

    /**
     * 创建
     *
     * @param DataTaskAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addDataTask(@RequestBody DataAddRequest DataTaskAddRequest, HttpServletRequest request) {
        if (DataTaskAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DataTask DataTask = new DataTask();
        BeanUtils.copyProperties(DataTaskAddRequest, DataTask);

        User loginUser = userService.getLoginUser();
        DataTask.setUserId(loginUser.getId());
        boolean result = dataTaskService.save(DataTask);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newDataTaskId = DataTask.getId();
        return ResultUtils.success(newDataTaskId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteDataTask(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser();
        long id = deleteRequest.getId();
        // 判断是否存在
        DataTask oldDataTask = dataTaskService.getById(id);
        ThrowUtils.throwIf(oldDataTask == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldDataTask.getUserId().equals(user.getId()) && !userService.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = dataTaskService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param DataTaskUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateDataTask(@RequestBody DataUpdateRequest DataTaskUpdateRequest) {
        if (DataTaskUpdateRequest == null || DataTaskUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DataTask DataTask = new DataTask();
        BeanUtils.copyProperties(DataTaskUpdateRequest, DataTask);
        long id = DataTaskUpdateRequest.getId();
        // 判断是否存在
        DataTask oldDataTask = dataTaskService.getById(id);
        ThrowUtils.throwIf(oldDataTask == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = dataTaskService.updateById(DataTask);
        return ResultUtils.success(result);
    }

    /**
     * 更新自己文本
     *
     * @param DataTaskUpdateRequest
     * @return
     */
    @PostMapping("/my/update")
    public BaseResponse<Boolean> updateMyDataTask(@RequestBody DataUpdateRequest DataTaskUpdateRequest) {
        if (DataTaskUpdateRequest == null || DataTaskUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser();
        DataTask DataTask = new DataTask();
        BeanUtils.copyProperties(DataTaskUpdateRequest, DataTask);
        long id = DataTaskUpdateRequest.getId();
        // 判断是否存在
        DataTask oldDataTask = dataTaskService.getById(id);
        ThrowUtils.throwIf(oldDataTask == null, ErrorCode.NOT_FOUND_ERROR);

        //判断为自己的文本
        ThrowUtils.throwIf(!loginUser.getId().equals(oldDataTask.getUserId()),ErrorCode.OPERATION_ERROR);
        boolean result = dataTaskService.updateById(DataTask);
        return ResultUtils.success(result);
    }
    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<DataTask> getDataTaskById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DataTask DataTask = dataTaskService.getById(id);
        if (DataTask == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(DataTask);
    }
    /**
     * 根据 id 获取 图表脱敏
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<DataTaskVO> getDataTaskVOById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DataTask DataTask = dataTaskService.getById(id);
        if (DataTask == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        DataTaskVO DataTaskVO = new DataTaskVO();
        BeanUtils.copyProperties(DataTask,DataTaskVO);
        return ResultUtils.success(DataTaskVO);
    }
    /**
     * 分页获取列表（封装类）
     *
     * @param DataTaskQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<DataTask>> listDataTaskByPage(@RequestBody DataTaskQueryRequest DataTaskQueryRequest) {
        long current = DataTaskQueryRequest.getCurrent();
        long size = DataTaskQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<DataTask> DataTaskPage = dataTaskService.page(new Page<>(current, size),
                getQueryWrapper(DataTaskQueryRequest));
        return ResultUtils.success(DataTaskPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param DataTaskQueryRequest
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<DataTask>> listMyDataTaskByPage(@RequestBody DataTaskQueryRequest DataTaskQueryRequest) {
        if (DataTaskQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser();
        DataTaskQueryRequest.setUserId(loginUser.getId());
        long current = DataTaskQueryRequest.getCurrent();
        long size = DataTaskQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<DataTask> DataTaskPage = dataTaskService.page(new Page<>(current, size),
                getQueryWrapper(DataTaskQueryRequest));
        return ResultUtils.success(DataTaskPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param DataTaskEditRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editDataTask(@RequestBody DataEditRequest DataTaskEditRequest) {
        if (DataTaskEditRequest == null || DataTaskEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        DataTask DataTask = new DataTask();
        BeanUtils.copyProperties(DataTaskEditRequest, DataTask);
        User loginUser = userService.getLoginUser();
        long id = DataTaskEditRequest.getId();
        // 判断是否存在
        DataTask oldDataTask = dataTaskService.getById(id);
        ThrowUtils.throwIf(oldDataTask == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldDataTask.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = dataTaskService.updateById(DataTask);
        return ResultUtils.success(result);
    }
    private QueryWrapper<DataTask> getQueryWrapper(DataTaskQueryRequest DataTaskQueryRequest) {
        QueryWrapper<DataTask> queryWrapper = new QueryWrapper<>();

        if (DataTaskQueryRequest == null) {
            return queryWrapper;
        }

        String DataType = DataTaskQueryRequest.getTextType();
        String name = DataTaskQueryRequest.getName();
        String sortField = DataTaskQueryRequest.getSortField();
        String sortOrder = DataTaskQueryRequest.getSortOrder();
        Long id = DataTaskQueryRequest.getId();
        Long userId = DataTaskQueryRequest.getUserId();


        queryWrapper.eq(id!=null &&id>0,"id",id);
        queryWrapper.like(StringUtils.isNotEmpty(name),"name",name);
        queryWrapper.eq(StringUtils.isNoneBlank(DataType),"DataType",DataType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
    /**
     * 文本数据上传(同步)
     *
     * @param multipartFile
     * @param genDataTaskByAiRequest
     * @return
     */
//    @PostMapping("/gen")
//    public BaseResponse<AiResponse> genDataTaskAi(@RequestPart("file") MultipartFile multipartFile,
//                                                  GenDataTaskByAiRequest genDataTaskByAiRequest) throws NoApiKeyException, InputRequiredException, NoApiKeyException, InputRequiredException, NoApiKeyException, InputRequiredException {
//
//        User loginUser = userService.getLoginUser();
//        //获取文本任务并校验
//        DataTask DataTask = dataTaskService.getDataTask(multipartFile, genDataTaskByAiRequest, loginUser);
//
//        //获取任务id
//        Long taskId = DataTask.getId();
//
//        String DataType = DataTask.getTextType();
//        //从根据任务id记录表中获取数据
//        QueryWrapper<DataRecord> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("DataTaskId",taskId);
//        List<DataRecord> DataRecords = dataRecordService.list(queryWrapper);
//
//        //将文本依次交给ai处理
//        for (DataRecord DataRecord : DataRecords) {
//            String result = null;
//            result = qianWenData.callWithMessage(dataRecordService.buildUserInput(DataRecord,DataType).toString());
//            DataRecord.setGenTextContent(result);
//            DataRecord.setStatus(DataConstant.SUCCEED);
//            boolean updateById = dataRecordService.updateById(DataRecord);
//            if (!updateById){
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"ai返回结果保存失败");
//            }
//        }
//
//
//        //将记录表中已经生成好的内容合并存入任务表
//        List<DataRecord> DataRecord = dataRecordService.list(queryWrapper);
//        StringBuilder stringBuilder = new StringBuilder();
//        DataRecord.forEach(DataRecord1 -> {
//            stringBuilder.append(DataRecord1.getGenTextContent()).append('\n');
//        });
//        DataTask DataTask1 = new DataTask();
//        DataTask1.setId(taskId);
//        DataTask1.setGenTextContent(stringBuilder.toString());
//        DataTask1.setStatus(DataConstant.SUCCEED);
//        boolean save = dataTaskService.updateById(DataTask1);
//        ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR,"ai返回文本任务保存失败");
//        AiResponse aiResponse = new AiResponse();
//        aiResponse.setResultId(DataTask.getId());
//        return ResultUtils.success(aiResponse);
//
//    }

    /**
     * 文本数据上传(mq)
     *
     * @param multipartFile
     * @param genDataTaskByAiRequest
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<AiResponse> genDataTaskAsyncAiMq(@RequestPart("file") MultipartFile multipartFile,
                                                              GenDataTaskByAiRequest genDataTaskByAiRequest) {


        User loginUser = userService.getLoginUser();

        //获取文本任务并校验
        DataTask DataTask = dataTaskService.getDataTask(multipartFile, genDataTaskByAiRequest, loginUser);

        //获取任务id
        Long taskId = DataTask.getId();

        log.warn("准备发送信息给队列，Message={}=======================================",taskId);
        mqMessageProducer.sendMessage(MqConstant.DATA_EXCHANGE_NAME,MqConstant.DATA_ROUTING_KEY,String.valueOf(taskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(DataTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本重新生成(mq)
     *
     * @param DataRebuildRequest
     * @return
     */
    @PostMapping("/gen/async/rebuild")
    public BaseResponse<AiResponse> genDataTaskAsyncAiRebuild(DataRebuildRequest DataRebuildRequest) {
        Long DataTaskId = DataRebuildRequest.getId();
        //获取记录表
        List<DataRecord> recordList = dataRecordService.list(new QueryWrapper<DataRecord>().eq("DataTaskId", DataTaskId));
        //校验，查看原始文本是否为空
        recordList.forEach(DataRecord -> {
            ThrowUtils.throwIf(StringUtils.isBlank(DataRecord.getTextContent()),ErrorCode.PARAMS_ERROR,"文本为空");
        });

        User loginUser = userService.getLoginUser();

        //保存数据库 wait
        DataTask DataTask = new DataTask();
        DataTask.setStatus(DataConstant.WAIT);
        DataTask.setId(DataTaskId);
        boolean saveResult = dataTaskService.updateById(DataTask);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"文本保存失败");
        log.warn("准备发送信息给队列，Message={}=======================================",DataTaskId);
        mqMessageProducer.sendMessage(MqConstant.DATA_EXCHANGE_NAME,MqConstant.DATA_ROUTING_KEY,String.valueOf(DataTaskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(DataTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本数据上传(mq)
     *
     * @param multipartFile
     * @param genDataTaskByAiRequest
     * @return
     */
    @PostMapping("/genClean/async/mq")
    public BaseResponse<AiResponse> genDataCleanTaskAsyncAiMq(@RequestPart("file") MultipartFile multipartFile,
                                                              GenDataTaskByAiRequest genDataTaskByAiRequest) {


        User loginUser = userService.getLoginUser();

        //获取文本任务并校验
        DataTask DataTask = dataTaskService.getDataTask(multipartFile, genDataTaskByAiRequest, loginUser);

        //获取任务id
        Long taskId = DataTask.getId();

        log.warn("准备发送信息给队列，Message={}=======================================",taskId);
        mqMessageProducer.sendMessage(MqConstant.DATA_CLEAN_EXCHANGE_NAME,MqConstant.DATA_CLEAN_ROUTING_KEY,String.valueOf(taskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(DataTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本重新生成(mq)
     *
     * @param DataRebuildRequest
     * @return
     */
    @PostMapping("/genClean/async/rebuild")
    public BaseResponse<AiResponse> genDataCleanTaskAsyncAiRebuild(DataRebuildRequest DataRebuildRequest) {
        Long DataTaskId = DataRebuildRequest.getId();
        //获取记录表
        List<DataRecord> recordList = dataRecordService.list(new QueryWrapper<DataRecord>().eq("DataTaskId", DataTaskId));
        //校验，查看原始文本是否为空
        recordList.forEach(DataRecord -> {
            ThrowUtils.throwIf(StringUtils.isBlank(DataRecord.getTextContent()),ErrorCode.PARAMS_ERROR,"文本为空");
        });

        User loginUser = userService.getLoginUser();

        //保存数据库 wait
        DataTask DataTask = new DataTask();
        DataTask.setStatus(DataConstant.WAIT);
        DataTask.setId(DataTaskId);
        boolean saveResult = dataTaskService.updateById(DataTask);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"文本保存失败");
        log.warn("准备发送信息给队列，Message={}=======================================",DataTaskId);
        mqMessageProducer.sendMessage(MqConstant.DATA_CLEAN_EXCHANGE_NAME,MqConstant.DATA_CLEAN_ROUTING_KEY,String.valueOf(DataTaskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(DataTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本数据上传(mq)
     *
     * @param multipartFile
     * @param genDataTaskByAiRequest
     * @return
     */
    @PostMapping("/genChoose/async/mq")
    public BaseResponse<AiResponse> genDataChooseTaskAsyncAiMq(@RequestPart("file") MultipartFile multipartFile,
                                                               GenDataTaskByAiRequest genDataTaskByAiRequest) {


        User loginUser = userService.getLoginUser();

        //获取文本任务并校验
        DataTask DataTask = dataTaskService.getDataTask(multipartFile, genDataTaskByAiRequest, loginUser);

        //获取任务id
        Long taskId = DataTask.getId();

        log.warn("准备发送信息给队列，Message={}=======================================",taskId);
        mqMessageProducer.sendMessage(MqConstant.DATA_CHOOSE_EXCHANGE_NAME,MqConstant.DATA_CHOOSE_ROUTING_KEY,String.valueOf(taskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(DataTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本重新生成(mq)
     *
     * @param DataRebuildRequest
     * @return
     */
    @PostMapping("/genChoose/async/rebuild")
    public BaseResponse<AiResponse> genDataChooseTaskAsyncAiRebuild(DataRebuildRequest DataRebuildRequest) {
        Long DataTaskId = DataRebuildRequest.getId();
        //获取记录表
        List<DataRecord> recordList = dataRecordService.list(new QueryWrapper<DataRecord>().eq("DataTaskId", DataTaskId));
        //校验，查看原始文本是否为空
        recordList.forEach(DataRecord -> {
            ThrowUtils.throwIf(StringUtils.isBlank(DataRecord.getTextContent()),ErrorCode.PARAMS_ERROR,"文本为空");
        });

        User loginUser = userService.getLoginUser();

        //保存数据库 wait
        DataTask DataTask = new DataTask();
        DataTask.setStatus(DataConstant.WAIT);
        DataTask.setId(DataTaskId);
        boolean saveResult = dataTaskService.updateById(DataTask);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"文本保存失败");
        log.warn("准备发送信息给队列，Message={}=======================================",DataTaskId);
        mqMessageProducer.sendMessage(MqConstant.DATA_CHOOSE_EXCHANGE_NAME,MqConstant.DATA_CHOOSE_ROUTING_KEY,String.valueOf(DataTaskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(DataTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本数据上传(mq)
     *
     * @param multipartFile
     * @param genDataTaskByAiRequest
     * @return
     */
    @PostMapping("/genForm/async/mq")
    public BaseResponse<AiResponse> genDataFormTaskAsyncAiMq(@RequestPart("file") MultipartFile multipartFile,
                                                               GenDataTaskByAiRequest genDataTaskByAiRequest) {


        User loginUser = userService.getLoginUser();

        //获取文本任务并校验
        DataTask DataTask = dataTaskService.getDataTask(multipartFile, genDataTaskByAiRequest, loginUser);

        //获取任务id
        Long taskId = DataTask.getId();

        log.warn("准备发送信息给队列，Message={}=======================================",taskId);
        mqMessageProducer.sendMessage(MqConstant.DATA_FORM_EXCHANGE_NAME,MqConstant.DATA_FORM_ROUTING_KEY,String.valueOf(taskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(DataTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本重新生成(mq)
     *
     * @param DataRebuildRequest
     * @return
     */
    @PostMapping("/genForm/async/rebuild")
    public BaseResponse<AiResponse> genDataFormTaskAsyncAiRebuild(DataRebuildRequest DataRebuildRequest) {
        Long DataTaskId = DataRebuildRequest.getId();
        //获取记录表
        List<DataRecord> recordList = dataRecordService.list(new QueryWrapper<DataRecord>().eq("DataTaskId", DataTaskId));
        //校验，查看原始文本是否为空
//        recordList.forEach(DataRecord -> {
//            ThrowUtils.throwIf(StringUtils.isBlank(DataRecord.getTextContent()),ErrorCode.PARAMS_ERROR,"文本为空");
//        });

        User loginUser = userService.getLoginUser();

        //保存数据库 wait
        DataTask DataTask = new DataTask();
        DataTask.setStatus(DataConstant.WAIT);
        DataTask.setId(DataTaskId);
        boolean saveResult = dataTaskService.updateById(DataTask);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"文本保存失败");
        log.warn("准备发送信息给队列，Message={}=======================================",DataTaskId);
        mqMessageProducer.sendMessage(MqConstant.DATA_FORM_EXCHANGE_NAME,MqConstant.DATA_FORM_ROUTING_KEY,String.valueOf(DataTaskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(DataTask.getId());
        return ResultUtils.success(aiResponse);

    }

}
