package com.dong.chart.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.chart.api.constant.ChartConstant;
import com.dong.chart.api.model.dto.GenChartByAiRequest;
import com.dong.chart.api.model.entity.Chart;
import com.dong.chart.mapper.ChartMapper;
import com.dong.chart.service.ChartService;
import com.dong.common.common.ErrorCode;
import com.dong.common.excption.BusinessException;
import com.dong.common.excption.ThrowUtils;
import com.dong.common.utils.ExcelUtils;
import com.dong.user.api.InnerCreditService;
import com.dong.user.api.constant.CreditConstant;
import com.dong.user.api.model.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService {


    @DubboReference
    InnerCreditService creditService;

    @Override
    public String buildUserInput(Chart chart){
        String goal = chart.getGoal();
        String chatType = chart.getChatType();
        String csvData = chart.getChartData();
        //构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chatType)) {
            userGoal += "，请使用" + chatType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Chart getChartTask(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        String chartType = genChartByAiRequest.getChartType();
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        //校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length()>=100,ErrorCode.PARAMS_ERROR,"名称不规范");
        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024*1024;
        ThrowUtils.throwIf(size>ONE_MB, ErrorCode.PARAMS_ERROR,"文件超过1MB");
        ThrowUtils.throwIf(size==0,ErrorCode.PARAMS_ERROR,"文件为空");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("png","xlsx","svg","webp","jpeg","csv");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀名非法");

        // 压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        //消耗积分
        Boolean creditResult = creditService.updateCredits(loginUser.getId(), CreditConstant.CREDIT_CHART_SUCCESS);
        ThrowUtils.throwIf(!creditResult,ErrorCode.OPERATION_ERROR,"你的积分不足");
        //保存数据库 wait
        Chart chart = new Chart();
        chart.setUserId(loginUser.getId());
        chart.setChartData(csvData);
        chart.setChatType(chartType);
        chart.setStatus(ChartConstant.WAIT);
        chart.setName(name);
        chart.setGoal(goal);
        boolean saveResult = this.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");
        return chart;
    }

    @Override
    public boolean saveChartAiResult(String result, long chartId) {
        String[] splits = result.split("【【【【【");

        if (splits.length < 3){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
        }
        //todo 可以使用正则表达式保证数据准确性，防止中文出现
        String genChart= splits[1].trim();
        String genResult = splits[2].trim();
        //将非js格式转化为js格式
        try {
            HashMap<String,Object> genChartJson = JSONUtil.toBean(genChart, HashMap.class);
            genChart = JSONUtil.toJsonStr(genChartJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI生成图片错误");
        }
        //保存数据库
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(ChartConstant.SUCCEED);
        updateChartResult.setGenChat(genChart);
        updateChartResult.setGenResult(genResult);
        return this.updateById(updateChartResult);

    }

    @Override
    public void handleChartUpdateError(Long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setStatus(ChartConstant.FAILED);
        updateChartResult.setId(chartId);
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = this.updateById(updateChartResult);
        if (!updateResult){
            log.error("更新图片失败状态失败"+chartId+","+execMessage);
        }
    }
}




