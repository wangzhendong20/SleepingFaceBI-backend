package com.dong.text.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.text.api.model.entity.TextRecord;
import com.dong.text.mapper.TextRecordMapper;
import com.dong.text.service.TextRecordService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


@Service
public class TextRecordServiceImpl extends ServiceImpl<TextRecordMapper, TextRecord>
    implements TextRecordService {

    @Override
    public String buildUserInput(TextRecord textRecord,String textTaskType) {
        String textContent = textRecord.getTextContent();
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

}




