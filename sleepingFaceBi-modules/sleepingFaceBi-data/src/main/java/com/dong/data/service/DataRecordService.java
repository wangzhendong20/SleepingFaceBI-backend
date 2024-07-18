package com.dong.data.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dong.data.api.model.entity.DataRecord;

/**
* @author dong
* @description 针对表【Data_record(文本记录表)】的数据库操作Service
* @createDate 2023-07-12 20:32:09
*/
public interface DataRecordService extends IService<DataRecord> {
    /**
     * 文本用户输入构造
     * @param dataRecord
     * @param DataTaskType
     * @return
     */
    String buildUserInput(DataRecord dataRecord, String DataTaskType);


    /**
     * 文本用户输入及目标构造
     * @param dataRecord
     * @param DataTaskType
     * @return
     */
    String buildUserInput(DataRecord dataRecord, String DataTaskType, String aim);
}
