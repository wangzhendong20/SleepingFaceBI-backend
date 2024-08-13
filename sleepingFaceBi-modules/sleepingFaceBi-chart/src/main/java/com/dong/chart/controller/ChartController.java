package com.dong.chart.controller;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dong.chart.api.constant.ChartConstant;
import com.dong.chart.api.model.dto.*;
import com.dong.chart.api.model.entity.Chart;
import com.dong.chart.api.model.vo.ChartVO;
import com.dong.chart.service.ChartService;
import com.dong.common.ai.config.QianWenChart;
import com.dong.common.annotation.AuthCheck;
import com.dong.common.common.BaseResponse;
import com.dong.common.common.DeleteRequest;
import com.dong.common.common.ErrorCode;
import com.dong.common.common.ResultUtils;
import com.dong.common.configs.config.ThreadPoolExecutorConfig;
import com.dong.common.configs.manager.RedisLimiterManager;
import com.dong.common.configs.manager.ThreadPoolExecutorManager;
import com.dong.common.constant.CommonConstant;
import com.dong.common.constant.LimitConstant;
import com.dong.common.constant.MqConstant;
import com.dong.common.excption.BusinessException;
import com.dong.common.excption.ThrowUtils;
import com.dong.common.model.vo.AiResponse;
import com.dong.common.mq.config.MqMessageProducer;
import com.dong.common.utils.SqlUtils;
import com.dong.user.api.InnerUserService;
import com.dong.user.api.constant.UserConstant;
import com.dong.user.api.model.entity.User;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.dong.common.constant.RedisConstant.CACHE_CHARTS_USER;
import static com.dong.common.constant.RedisConstant.MUTEX_CHARTS_KEY;

/**
 * 图表分析接口
 *
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @DubboReference
    private InnerUserService userService;


    @Resource
    private QianWenChart qianWenChart;


    @Resource
    private MqMessageProducer mqMessageProducer;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private Gson gson;

    @Resource
    private RedissonClient redissonClient;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);

        User loginUser = userService.getLoginUser();
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser();
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }
    /**
     * 根据 id 获取 图表脱敏
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<ChartVO> getChartVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        ChartVO chartVO = new ChartVO();
        BeanUtils.copyProperties(chart,chartVO);
        return ResultUtils.success(chartVO);
    }
    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser();
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        //添加redis作为缓存，提高速度
        String key = CACHE_CHARTS_USER + chartQueryRequest.getUserId();
        String cacheChartList = stringRedisTemplate.opsForValue().get(key);

        if (!StringUtils.isEmpty(cacheChartList)) {
            try {
                List<Chart> chartList = gson.fromJson(cacheChartList,new TypeToken<List<Chart>>(){}.getType());
                Page<Chart> chartPage = new Page<>();
                if(StringUtils.isEmpty(chartQueryRequest.getName())){
                    //返回全部
                    chartPage.setRecords(chartList);
                    chartPage.setTotal(chartList.size());
                }else{
                    //返回指定的图表
                    List<Chart> myChartList = chartList.stream().filter((chart)->{
                        //用contains代替模糊查询
                        return chart.getName().contains(chartQueryRequest.getName());
                    }).collect(Collectors.toList());

                    chartPage.setRecords(myChartList);
                    chartPage.setTotal(myChartList.size());
                }

                return ResultUtils.success(chartPage);

            } catch (Exception e) {
                e.printStackTrace();
                //如果一旦发生错误，那么就使用数据库查询
                Page<Chart> chartPage = chartService.page(new Page<>(current, size), getQueryWrapper(chartQueryRequest));
                return ResultUtils.success(chartPage);
            }
        }

        //防止缓存穿透,返回一个空值
        if(cacheChartList != null){
            return ResultUtils.success(new Page<Chart>());
        }

        //防止缓存击穿，使用互斥锁让线程排队
        RLock lock = redissonClient.getLock(MUTEX_CHARTS_KEY + chartQueryRequest.getUserId());
        Page<Chart> chartPage = null;

        try {
            //尝试获得锁
            boolean b = lock.tryLock();
            //没有获得锁，重试
            if (!b){
                return listMyChartByPage(chartQueryRequest);
            }
            //否则，就直接去数据库查询
            chartPage = chartService.page(new Page<>(current, size),
                    getQueryWrapper(chartQueryRequest));
            if(chartPage.getRecords().isEmpty()){
                //并在查询后，向缓存添加信息
                String myChartListJson = gson.toJson(chartPage.getRecords());
                //设置较短的TTL
                stringRedisTemplate.opsForValue().set(key,myChartListJson,5, TimeUnit.MINUTES);
                return ResultUtils.success(chartPage);
            }
            //并在查询后，向缓存添加信息
            String myChartListJson = gson.toJson(chartPage.getRecords());
            //必须添加过期时间，因为Redis的内存并不能扩充
            stringRedisTemplate.opsForValue().set(key,myChartListJson,12, TimeUnit.HOURS);
        }catch (Exception e){
            ThrowUtils.throwIf(true, ErrorCode.SYSTEM_ERROR,"系统错误，请稍后重试");
        }finally {
            lock.unlock();
        }


//        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
//                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion


    /**
     * 编辑（图表）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser();
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();

        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        String chatType = chartQueryRequest.getChatType();
        String goal = chartQueryRequest.getGoal();
        String name = chartQueryRequest.getName();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        Long id = chartQueryRequest.getId();
        Long userId = chartQueryRequest.getUserId();


        queryWrapper.eq(id!=null &&id>0,"id",id);
        queryWrapper.like(StringUtils.isNotEmpty(name),"name",name);
        queryWrapper.eq(StringUtils.isNoneBlank(goal),"goal",goal);
        queryWrapper.eq(StringUtils.isNoneBlank(chatType),"chartType",chatType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId),"userId",userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
    /**
     * 图表数据上传(同步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<AiResponse> genChartAi(@RequestPart("file") MultipartFile multipartFile,
                                               GenChartByAiRequest genChartByAiRequest) throws NoApiKeyException, InputRequiredException {

        User loginUser = userService.getLoginUser();
        //获取任务表数据
        Chart chartTask = chartService.getChartTask(multipartFile, genChartByAiRequest, loginUser);

        String result = qianWenChart.callWithMessage(chartService.buildUserInput(chartTask));
        //处理返回的数据
        boolean saveResult = chartService.saveChartAiResult(result, chartTask.getId());
        if (!saveResult){
            chartService.handleChartUpdateError(chartTask.getId(), "图表数据保存失败");
        }
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(chartTask.getId());
        return ResultUtils.success(aiResponse);

    }


    @Resource
    private ThreadPoolExecutorManager threadPoolExecutorManager;
    /**
     * 图表数据上传(异步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @return
     */
    @PostMapping("/gen/async/threadpool")
    public BaseResponse<AiResponse> genChartAsyncAi(@RequestPart("file") MultipartFile multipartFile,
                                                    GenChartByAiRequest genChartByAiRequest) {

        User loginUser = userService.getLoginUser();
        //获取任务表数据
        Chart chartTask = chartService.getChartTask(multipartFile, genChartByAiRequest, loginUser);

        threadPoolExecutorManager.dynamicModify(20, 40, 200, true);

        //todo 需要处理队列满后的异常
        try {
            CompletableFuture.runAsync(()->{
                //更改图片状态为 running
                Chart updateChart = new Chart();
                updateChart.setId(chartTask.getId());
                updateChart.setStatus(ChartConstant.RUNNING);
                boolean updateResult = chartService.updateById(updateChart);
                if (!updateResult){
                    chartService.handleChartUpdateError(chartTask.getId(),"更新图表执行状态失败");
                    return;
                }
                //调用AI
                String result = null;
                try {
                    result = qianWenChart.callWithMessage(chartService.buildUserInput(updateChart).toString());
                } catch (Exception e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 服务错误");
                }
                //处理返回的数据
                boolean saveResult = chartService.saveChartAiResult(result, chartTask.getId());
                if (!saveResult){
                    chartService.handleChartUpdateError(chartTask.getId(), "图表数据保存失败");
                }
            },threadPoolExecutor);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"系统繁忙，请稍后重试");
        }
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(chartTask.getId());
        return ResultUtils.success(aiResponse);

    }

    /**
     * 图表数据上传(mq)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<AiResponse> genChartAsyncAiMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest) {
        User loginUser = userService.getLoginUser();
        redisLimiterManager.doRateLimit(LimitConstant.GEN_CHART_LIMIT + loginUser.getId());

        //获取任务表数据
        Chart chartTask = chartService.getChartTask(multipartFile, genChartByAiRequest, loginUser);

        Long chartId = chartTask.getId();
        log.warn("准备发送信息给队列，Message={}=======================================",chartId);
        mqMessageProducer.sendMessage(MqConstant.BI_EXCHANGE_NAME,MqConstant.BI_ROUTING_KEY,String.valueOf(chartId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(chartTask.getId());
        return ResultUtils.success(aiResponse);

    }


    /**
     * 图表重新生成(mq)
     *
     * @param chartRebuildRequest
     * @return
     */
    @PostMapping("/gen/async/rebuild")
    public BaseResponse<AiResponse> genChartAsyncAiRebuild(ChartRebuildRequest chartRebuildRequest) {
        Long chartId = chartRebuildRequest.getId();
        Chart genChartByAiRequest = chartService.getById(chartId);
        String chartType = genChartByAiRequest.getChatType();
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartData = genChartByAiRequest.getChartData();

        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length()>=100,ErrorCode.PARAMS_ERROR,"名称过长");
        ThrowUtils.throwIf(StringUtils.isBlank(chartData),ErrorCode.PARAMS_ERROR,"表格数据为空");
        ThrowUtils.throwIf(StringUtils.isBlank(chartType),ErrorCode.PARAMS_ERROR,"生成表格类型为空");

        User loginUser = userService.getLoginUser();

        //保存数据库 wait
        Chart chart = new Chart();
        chart.setStatus(ChartConstant.WAIT);
        chart.setId(chartId);
        boolean saveResult = chartService.updateById(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");
        log.warn("准备发送信息给队列，Message={}=======================================",chartId);
        mqMessageProducer.sendMessage(MqConstant.BI_EXCHANGE_NAME,MqConstant.BI_ROUTING_KEY,String.valueOf(chartId));
        //返回数据参数
        AiResponse aiResponse = new AiResponse();
        aiResponse.setResultId(chart.getId());
        return ResultUtils.success(aiResponse);

    }




}
