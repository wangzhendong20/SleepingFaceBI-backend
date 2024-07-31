package com.dong.text.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.text.api.model.dto.GenTextTaskByAiRequest;
import com.dong.text.api.model.entity.TextTask;
import com.dong.user.api.model.entity.User;
import org.springframework.web.multipart.MultipartFile;


public interface TextTaskService extends IService<TextTask> {

    /**
     * 获取准备分析的表数据(事务回滚)
     * @param multipartFile
     * @param genTextTaskByAiRequest
     * @param loginUser
     * @return
     */
    TextTask getTextTask(MultipartFile multipartFile, GenTextTaskByAiRequest genTextTaskByAiRequest, User loginUser);

    /**
     * 文本更新失败
     * @param textTaskId
     * @param execMessage
     */
    void handleTextTaskUpdateError(Long textTaskId, String execMessage);
}
