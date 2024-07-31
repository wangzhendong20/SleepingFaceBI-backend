package com.dong.text.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.text.api.model.entity.TextRecord;


public interface TextRecordService extends IService<TextRecord> {
    /**
     * 文本用户输入构造
     * @param textRecord
     * @param textTaskType
     * @return
     */
    String buildUserInput(TextRecord textRecord, String textTaskType);
}
