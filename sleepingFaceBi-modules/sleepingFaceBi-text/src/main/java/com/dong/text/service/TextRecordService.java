package com.dong.text.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.text.api.model.entity.TextRecord;

/**
* @author dong
* @description 针对表【text_record(文本记录表)】的数据库操作Service
* @createDate 2023-07-12 20:32:09
*/
public interface TextRecordService extends IService<TextRecord> {
    /**
     * 文本用户输入构造
     * @param textRecord
     * @param textTaskType
     * @return
     */
    String buildUserInput(TextRecord textRecord, String textTaskType);
}
