package com.dong.user.controller;


import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dong.common.common.BaseResponse;
import com.dong.common.common.DeleteRequest;
import com.dong.common.common.ErrorCode;
import com.dong.common.common.ResultUtils;
import com.dong.common.excption.BusinessException;
import com.dong.common.excption.ThrowUtils;
import com.dong.common.utils.KeyGenerator;
import com.dong.user.annotation.AuthCheck;
import com.dong.user.api.constant.UserConstant;
import com.dong.user.api.model.dto.KeyRecord.KeyRecordQueryRequest;
import com.dong.user.api.model.dto.user.*;
import com.dong.user.api.model.entity.KeyRecord;
import com.dong.user.api.model.entity.User;
import com.dong.user.api.model.vo.KeyRecordVO;
import com.dong.user.api.model.vo.LoginUserVO;
import com.dong.user.api.model.vo.UserVO;
import com.dong.user.service.CreditService;
import com.dong.user.service.KeyRecordService;
import com.dong.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/keyRecord")
@Slf4j
public class KeyRecordController {

    @Resource
    private UserService userService;
    @Resource
    private KeyRecordService keyRecordService;

    @GetMapping("/generateKeys")
    public BaseResponse<KeyRecordVO> generateKeys() {
        User loginUser = userService.getLoginUser();

        String ak = KeyGenerator.generateAccessKey();
        String sk = null;
        try {
            sk = KeyGenerator.generateSecretKey();
        } catch (NoSuchAlgorithmException e) {
            ThrowUtils.throwIf(true, ErrorCode.SYSTEM_ERROR);
        }
        ThrowUtils.throwIf(StringUtils.isBlank(ak) || StringUtils.isBlank(sk), ErrorCode.SYSTEM_ERROR);

        // 存储AK和SK
        KeyRecord keyRecord = new KeyRecord();
        keyRecord.setUserId(loginUser.getId());
        keyRecord.setAccessKey(ak);
        keyRecord.setSecretKey(sk);
        boolean save = keyRecordService.save(keyRecord);

        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);

        KeyRecordVO keyRecordVO = new KeyRecordVO();
        BeanUtils.copyProperties(keyRecord, keyRecordVO);

        // 返回AK和SK
        return ResultUtils.success(keyRecordVO);
    }

    // region 增删改查

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteKeys(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = keyRecordService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }


    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<KeyRecord> getkeysById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        KeyRecord keyRecord = keyRecordService.getById(id);
        ThrowUtils.throwIf(keyRecord == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(keyRecord);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<KeyRecordVO> getKeysVOById(long id) {
        BaseResponse<KeyRecord> response = getkeysById(id);
        KeyRecord keyRecord = response.getData();
        return ResultUtils.success(keyRecordService.getKeyRecordVO(keyRecord));
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<KeyRecord>> listKeyRecordByPage(@RequestBody KeyRecordQueryRequest keyRecordRequest) {
        long current = keyRecordRequest.getCurrent();
        long size = keyRecordRequest.getPageSize();
        Page<KeyRecord> keyRecordPage = keyRecordService.page(new Page<>(current, size),
                keyRecordService.getQueryWrapper(keyRecordRequest));
        return ResultUtils.success(keyRecordPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<KeyRecordVO>> listUserVOByPage(@RequestBody KeyRecordQueryRequest keyRecordRequest) {
        if (keyRecordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = keyRecordRequest.getCurrent();
        long size = keyRecordRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<KeyRecord> keyRecordPage = keyRecordService.page(new Page<>(current, size),
                keyRecordService.getQueryWrapper(keyRecordRequest));
        Page<KeyRecordVO> keyRecordVOPage = new Page<>(current, size, keyRecordPage.getTotal());
        List<KeyRecordVO> keyRecordVO = keyRecordService.getKeyRecordVO(keyRecordPage.getRecords());
        keyRecordVOPage.setRecords(keyRecordVO);
        return ResultUtils.success(keyRecordVOPage);
    }

}
