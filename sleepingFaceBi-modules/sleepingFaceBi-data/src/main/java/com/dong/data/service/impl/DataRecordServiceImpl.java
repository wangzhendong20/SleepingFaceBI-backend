package com.dong.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.data.api.model.entity.DataRecord;
import com.dong.data.mapper.DataRecordMapper;
import com.dong.data.service.DataRecordService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


@Service
public class DataRecordServiceImpl extends ServiceImpl<DataRecordMapper, DataRecord>
    implements DataRecordService {

    @Override
    public String buildUserInput(DataRecord dataRecord,String textTaskType) {
        String textContent = dataRecord.getTextContent();
        //构造用户输入
        StringBuilder userInput = new StringBuilder();
        String gold = "请使用"+textTaskType+"格式对下面文本做格式转换，只需要输出转换格式后的文本即可，不需要输出任何多余的解释、注释和格式类型等。";

        userInput.append(gold).append("\n");

        if (StringUtils.isNotBlank(textContent)) {
            textContent = textContent.trim();
            userInput.append(textContent);
        }
        return userInput.toString();
    }

    @Override
    public String buildUserInput(DataRecord dataRecord,String textTaskType, String aim) {
        String textContent = dataRecord.getTextContent();
        //构造用户输入
        StringBuilder userInput = new StringBuilder();
        String gold = "目标：" + aim + "，请使用"+textTaskType+"格式对下面文本做格式转换，只需要输出转换格式后的文本即可，不需要输出任何多余的解释、注释和格式类型等。";

        userInput.append(gold).append("\n");

        if (StringUtils.isNotBlank(textContent)) {
            textContent = textContent.trim();
            userInput.append(textContent);
        }
        return userInput.toString();
    }

}




