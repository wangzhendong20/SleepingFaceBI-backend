package com.dong.text.controller;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dong.apiclientsdk.client.ApiClient;
import com.dong.common.configs.manager.RedisLimiterManager;
import com.dong.common.constant.LimitConstant;
import com.dong.user.api.InnerKeyRecordService;
import com.dong.user.api.model.entity.KeyRecord;
import com.google.gson.Gson;
import com.dong.common.ai.config.QianWenText;
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
import com.dong.text.api.constant.TextConstant;
import com.dong.text.api.model.dto.*;
import com.dong.text.api.model.entity.TextRecord;
import com.dong.text.api.model.entity.TextTask;
import com.dong.text.api.model.vo.TextTaskVO;
import com.dong.text.service.TextRecordService;
import com.dong.text.service.TextTaskService;
import com.dong.user.api.InnerCreditService;
import com.dong.user.api.InnerUserService;
import com.dong.user.api.constant.UserConstant;
import com.dong.user.api.model.entity.User;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.dong.common.utils.SignUtils;

import static com.dong.common.constant.RedisConstant.*;

/**
 * 格式转换接口
 *
 */
@RestController
@RequestMapping("/text")
@Slf4j
public class TextController {

    @Resource
    private TextTaskService textTaskService;

    @Resource
    private TextRecordService textRecordService;
    @DubboReference
    private InnerUserService userService;
    @DubboReference
    private InnerKeyRecordService keyRecordService;

    @Resource
    private QianWenText qianWenText;
    @DubboReference
    private InnerCreditService creditService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    //自己的API接口
    @Resource
    private ApiClient apiClient;

    @Resource
    private MqMessageProducer mqMessageProducer;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private Gson gson;

    private final static Gson GSON = new Gson();

    // region 增删改查

    /**
     * 创建
     *
     * @param textTaskAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTextTask(@RequestBody TextAddRequest textTaskAddRequest, HttpServletRequest request) {
        if (textTaskAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        TextTask textTask = new TextTask();
        BeanUtils.copyProperties(textTaskAddRequest, textTask);

        User loginUser = userService.getLoginUser();
        textTask.setUserId(loginUser.getId());
        boolean result = textTaskService.save(textTask);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newTextTaskId = textTask.getId();
        return ResultUtils.success(newTextTaskId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTextTask(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser();
        long id = deleteRequest.getId();
        // 判断是否存在
        TextTask oldTextTask = textTaskService.getById(id);
        ThrowUtils.throwIf(oldTextTask == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldTextTask.getUserId().equals(user.getId()) && !userService.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = textTaskService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param textTaskUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateTextTask(@RequestBody TextUpdateRequest textTaskUpdateRequest) {
        if (textTaskUpdateRequest == null || textTaskUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        TextTask textTask = new TextTask();
        BeanUtils.copyProperties(textTaskUpdateRequest, textTask);
        long id = textTaskUpdateRequest.getId();
        // 判断是否存在
        TextTask oldTextTask = textTaskService.getById(id);
        ThrowUtils.throwIf(oldTextTask == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = textTaskService.updateById(textTask);
        return ResultUtils.success(result);
    }

    /**
     * 更新自己文本
     *
     * @param textTaskUpdateRequest
     * @return
     */
    @PostMapping("/my/update")
    public BaseResponse<Boolean> updateMyTextTask(@RequestBody TextUpdateRequest textTaskUpdateRequest) {
        if (textTaskUpdateRequest == null || textTaskUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser();
        TextTask textTask = new TextTask();
        BeanUtils.copyProperties(textTaskUpdateRequest, textTask);
        long id = textTaskUpdateRequest.getId();
        // 判断是否存在
        TextTask oldTextTask = textTaskService.getById(id);
        ThrowUtils.throwIf(oldTextTask == null, ErrorCode.NOT_FOUND_ERROR);

        //判断为自己的文本
        ThrowUtils.throwIf(!loginUser.getId().equals(oldTextTask.getUserId()),ErrorCode.OPERATION_ERROR);
        boolean result = textTaskService.updateById(textTask);
        return ResultUtils.success(result);
    }
    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<TextTask> getTextTaskById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        TextTask textTask = textTaskService.getById(id);
        if (textTask == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(textTask);
    }
    /**
     * 根据 id 获取 图表脱敏
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<TextTaskVO> getTextTaskVOById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        TextTask textTask = textTaskService.getById(id);
        if (textTask == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        TextTaskVO textTaskVO = new TextTaskVO();
        BeanUtils.copyProperties(textTask,textTaskVO);
        return ResultUtils.success(textTaskVO);
    }
    /**
     * 分页获取列表（封装类）
     *
     * @param textTaskQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<TextTask>> listTextTaskByPage(@RequestBody TextTaskQueryRequest textTaskQueryRequest) {
        long current = textTaskQueryRequest.getCurrent();
        long size = textTaskQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<TextTask> textTaskPage = textTaskService.page(new Page<>(current, size),
                getQueryWrapper(textTaskQueryRequest));
        return ResultUtils.success(textTaskPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param textTaskQueryRequest
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<TextTask>> listMyTextTaskByPage(@RequestBody TextTaskQueryRequest textTaskQueryRequest) {
        if (textTaskQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser();
        textTaskQueryRequest.setUserId(loginUser.getId());
        long current = textTaskQueryRequest.getCurrent();
        long size = textTaskQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        //添加redis作为缓存，提高速度
        String key = CACHE_TEXT_USER + textTaskQueryRequest.getUserId();
        String cachetextTaskList = stringRedisTemplate.opsForValue().get(key);

        if (!StringUtils.isEmpty(cachetextTaskList)) {
            try {
                List<TextTask> textTaskList = gson.fromJson(cachetextTaskList,new TypeToken<List<TextTask>>(){}.getType());
                Page<TextTask> textTaskPage = new Page<>();
                if(StringUtils.isEmpty(textTaskQueryRequest.getName())){
                    //返回全部
                    textTaskPage.setRecords(textTaskList);
                    textTaskPage.setTotal(textTaskList.size());
                }else{
                    //返回指定的图表
                    List<TextTask> mytextTaskList = textTaskList.stream().filter((TextTask)->{
                        //用contains代替模糊查询
                        return TextTask.getName().contains(textTaskQueryRequest.getName());
                    }).collect(Collectors.toList());

                    textTaskPage.setRecords(mytextTaskList);
                    textTaskPage.setTotal(mytextTaskList.size());
                }

                return ResultUtils.success(textTaskPage);

            } catch (Exception e) {
                e.printStackTrace();
                //如果一旦发生错误，那么就使用数据库查询
                Page<TextTask> textTaskPage = textTaskService.page(new Page<>(current, size), getQueryWrapper(textTaskQueryRequest));
                return ResultUtils.success(textTaskPage);
            }
        }

        //防止缓存穿透,返回一个空值
        if(cachetextTaskList != null){
            return ResultUtils.success(new Page<TextTask>());
        }

        //防止缓存击穿，使用互斥锁让线程排队
        RLock lock = redissonClient.getLock(MUTEX_TEXT_KEY + textTaskQueryRequest.getUserId());
        Page<TextTask> textTaskPage = null;

        try {
            //尝试获得锁
            boolean b = lock.tryLock();
            //没有获得锁，重试
            if (!b){
                return listMyTextTaskByPage(textTaskQueryRequest);
            }
            //否则，就直接去数据库查询
            textTaskPage = textTaskService.page(new Page<>(current, size),
                    getQueryWrapper(textTaskQueryRequest));
            if(textTaskPage.getRecords().isEmpty()){
                //并在查询后，向缓存添加信息
                String mytextTaskListJson = gson.toJson(textTaskPage.getRecords());
                //设置较短的TTL
                stringRedisTemplate.opsForValue().set(key,mytextTaskListJson,5, TimeUnit.MINUTES);
                return ResultUtils.success(textTaskPage);
            }
            //并在查询后，向缓存添加信息
            String mytextTaskListJson = gson.toJson(textTaskPage.getRecords());
            //必须添加过期时间，因为Redis的内存并不能扩充
            stringRedisTemplate.opsForValue().set(key,mytextTaskListJson,12, TimeUnit.HOURS);
        }catch (Exception e){
            ThrowUtils.throwIf(true, ErrorCode.SYSTEM_ERROR,"系统错误，请稍后重试");
        }finally {
            lock.unlock();
        }

//        Page<TextTask> textTaskPage = textTaskService.page(new Page<>(current, size),
//                getQueryWrapper(textTaskQueryRequest));
        return ResultUtils.success(textTaskPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param textTaskEditRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editTextTask(@RequestBody TextEditRequest textTaskEditRequest) {
        if (textTaskEditRequest == null || textTaskEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        TextTask textTask = new TextTask();
        BeanUtils.copyProperties(textTaskEditRequest, textTask);
        User loginUser = userService.getLoginUser();
        long id = textTaskEditRequest.getId();
        // 判断是否存在
        TextTask oldTextTask = textTaskService.getById(id);
        ThrowUtils.throwIf(oldTextTask == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldTextTask.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = textTaskService.updateById(textTask);
        return ResultUtils.success(result);
    }
    private QueryWrapper<TextTask> getQueryWrapper(TextTaskQueryRequest textTaskQueryRequest) {
        QueryWrapper<TextTask> queryWrapper = new QueryWrapper<>();

        if (textTaskQueryRequest == null) {
            return queryWrapper;
        }

        String textType = textTaskQueryRequest.getTextType();
        String name = textTaskQueryRequest.getName();
        String sortField = textTaskQueryRequest.getSortField();
        String sortOrder = textTaskQueryRequest.getSortOrder();
        Long id = textTaskQueryRequest.getId();
        Long userId = textTaskQueryRequest.getUserId();


        queryWrapper.eq(id!=null &&id>0,"id",id);
        queryWrapper.like(StringUtils.isNotEmpty(name),"name",name);
        queryWrapper.eq(StringUtils.isNoneBlank(textType),"textType",textType);
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
     * @param genTextTaskByAiRequest
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<AiResponse> genTextTaskAi(@RequestPart("file") MultipartFile multipartFile,
                                                  GenTextTaskByAiRequest genTextTaskByAiRequest) throws NoApiKeyException, InputRequiredException, NoApiKeyException, InputRequiredException, NoApiKeyException, InputRequiredException {

        User loginUser = userService.getLoginUser();
        //获取文本任务并校验
        TextTask textTask = textTaskService.getTextTask(multipartFile, genTextTaskByAiRequest, loginUser);

        //获取任务id
        Long taskId = textTask.getId();

        String textType = textTask.getTextType();
        //从根据任务id记录表中获取数据
        QueryWrapper<TextRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("textTaskId",taskId);
        List<TextRecord> textRecords = textRecordService.list(queryWrapper);

        //将文本依次交给ai处理
        for (TextRecord textRecord : textRecords) {
            String result = null;
            result = qianWenText.callWithMessage(textRecordService.buildUserInput(textRecord,textType).toString());
            textRecord.setGenTextContent(result);
            textRecord.setStatus(TextConstant.SUCCEED);
            boolean updateById = textRecordService.updateById(textRecord);
            if (!updateById){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"ai返回结果保存失败");
            }
        }


        //将记录表中已经生成好的内容合并存入任务表
        List<TextRecord> textRecord = textRecordService.list(queryWrapper);
        StringBuilder stringBuilder = new StringBuilder();
        textRecord.forEach(textRecord1 -> {
            stringBuilder.append(textRecord1.getGenTextContent()).append('\n');
        });
        TextTask textTask1 = new TextTask();
        textTask1.setId(taskId);
        textTask1.setGenTextContent(stringBuilder.toString());
        textTask1.setStatus(TextConstant.SUCCEED);
        boolean save = textTaskService.updateById(textTask1);
        ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR,"ai返回文本任务保存失败");
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(textTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本数据上传(mq)
     *
     * @param multipartFile
     * @param genTextTaskByAiRequest
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<AiResponse> genTextTaskAsyncAiMq(@RequestPart("file") MultipartFile multipartFile,
                                                              GenTextTaskByAiRequest genTextTaskByAiRequest) {


        User loginUser = userService.getLoginUser();
        redisLimiterManager.doRateLimit(LimitConstant.GEN_TEXT_LIMIT + loginUser.getId());

        //获取文本任务并校验
        TextTask textTask = textTaskService.getTextTask(multipartFile, genTextTaskByAiRequest, loginUser);

        //获取任务id
        Long taskId = textTask.getId();

        log.warn("准备发送信息给队列，Message={}=======================================",taskId);
        mqMessageProducer.sendMessage(MqConstant.TEXT_EXCHANGE_NAME,MqConstant.TEXT_ROUTING_KEY,String.valueOf(taskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(textTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本重新生成(mq)
     *
     * @param textRebuildRequest
     * @return
     */
    @PostMapping("/gen/async/rebuild")
    public BaseResponse<AiResponse> genTextTaskAsyncAiRebuild(TextRebuildRequest textRebuildRequest) {
        Long textTaskId = textRebuildRequest.getId();
        //获取记录表
        List<TextRecord> recordList = textRecordService.list(new QueryWrapper<TextRecord>().eq("textTaskId", textTaskId));
        //校验，查看原始文本是否为空
        recordList.forEach(textRecord -> {
            ThrowUtils.throwIf(StringUtils.isBlank(textRecord.getTextContent()),ErrorCode.PARAMS_ERROR,"文本为空");
        });

        User loginUser = userService.getLoginUser();

        //保存数据库 wait
        TextTask textTask = new TextTask();
        textTask.setStatus(TextConstant.WAIT);
        textTask.setId(textTaskId);
        boolean saveResult = textTaskService.updateById(textTask);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"文本保存失败");
        log.warn("准备发送信息给队列，Message={}=======================================",textTaskId);
        mqMessageProducer.sendMessage(MqConstant.TEXT_EXCHANGE_NAME,MqConstant.TEXT_ROUTING_KEY,String.valueOf(textTaskId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(textTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 文本数据API(同步)
     *
     * @return
     */
    @PostMapping("/api/gen")
    public BaseResponse<AiResponse> genTextTaskAiAPI(HttpServletRequest request){

        String accessKey = request.getHeader("accessKey");
        String nonce = request.getHeader("nonce");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        String body = request.getHeader("body");
        String name = request.getHeader("name");
        String textType = request.getHeader("textType");

        KeyRecord keyRecord = keyRecordService.getKeyRecord(accessKey);
        String secretKey = keyRecord.getSecretKey();
        Long userId = keyRecord.getUserId();

        String signStr = SignUtils.genSign(body,secretKey);
        ThrowUtils.throwIf(!signStr.equals(sign),ErrorCode.FORBIDDEN_ERROR,"签名错误");

        //保存数据库 wait
        //保存任务进数据库
        TextTask textTask = new TextTask();
        textTask.setTextType(textType);
        textTask.setName(name);
        textTask.setUserId(userId);
        textTask.setStatus(TextConstant.WAIT);
        boolean saveResult = textTaskService.save(textTask);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"文本任务保存失败");

        Long taskId = textTask.getId();

        ArrayList<String> textContentList = new ArrayList<>();

        TextRecord textRecord = new TextRecord();
        textRecord.setTextTaskId(taskId);
        textRecord.setTextContent(body);
        textRecord.setStatus(TextConstant.WAIT);


        boolean batchResult = textRecordService.save(textRecord);
        ThrowUtils.throwIf(!batchResult,ErrorCode.SYSTEM_ERROR,"文本记录保存失败");

        //从根据任务id记录表中获取数据
        QueryWrapper<TextRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("textTaskId",taskId);
        List<TextRecord> textRecords = textRecordService.list(queryWrapper);

        //将文本依次交给ai处理
        for (TextRecord textrecord : textRecords) {
            String result = null;
            try {
                result = qianWenText.callWithMessage(textRecordService.buildUserInput(textrecord,textType).toString());
            } catch (Exception e) {
                ThrowUtils.throwIf(true,ErrorCode.SYSTEM_ERROR,"ai接口调用失败");
            }
            textrecord.setGenTextContent(result);
            textrecord.setStatus(TextConstant.SUCCEED);
            boolean updateById = textRecordService.updateById(textrecord);
            if (!updateById){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"ai返回结果保存失败");
            }
        }


        //将记录表中已经生成好的内容合并存入任务表
        List<TextRecord> textRecord2 = textRecordService.list(queryWrapper);
        StringBuilder stringBuilder = new StringBuilder();
        textRecord2.forEach(textRecord1 -> {
            stringBuilder.append(textRecord1.getGenTextContent()).append('\n');
        });
        TextTask textTask1 = new TextTask();
        textTask1.setId(taskId);
        textTask1.setGenTextContent(stringBuilder.toString());
        textTask1.setStatus(TextConstant.SUCCEED);
        boolean save = textTaskService.updateById(textTask1);
        ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR,"ai返回文本任务保存失败");
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(textTask.getId());
        return ResultUtils.success(aiResponse);

    }
}
