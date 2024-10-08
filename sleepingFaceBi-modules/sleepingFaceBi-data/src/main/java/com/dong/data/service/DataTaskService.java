package com.dong.data.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.data.api.model.dto.GenDataTaskByAiRequest;
import com.dong.data.api.model.entity.DataTask;
import com.dong.data.api.model.dto.GenDataTaskByAiRequest;
import com.dong.data.api.model.entity.DataTask;
import com.dong.user.api.model.entity.User;
import org.springframework.web.multipart.MultipartFile;


public interface DataTaskService extends IService<DataTask> {

    /**
     * 获取准备分析的表数据(事务回滚)
     * @param multipartFile
     * @param genDataTaskByAiRequest
     * @param loginUser
     * @return
     */
    DataTask getDataTask(MultipartFile multipartFile, GenDataTaskByAiRequest genDataTaskByAiRequest, User loginUser);

    /**
     * 文本更新失败
     * @param dataTaskId
     * @param execMessage
     */
    void handleDataTaskUpdateError(Long dataTaskId, String execMessage);
}
