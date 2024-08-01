package com.dong.text.readerStrategy;

import com.dong.common.common.ErrorCode;
import com.dong.common.excption.ThrowUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class FileProcessor {

    @Resource
    private FileReaderFactory fileReaderFactory;

    public ArrayList<String> processFile(String suffix, MultipartFile multipartFile){

        FileReaderStrategy strategy = fileReaderFactory.getStrategy(suffix);

        if (strategy != null) {
            try {
                return strategy.readFile(multipartFile);
            } catch (Exception e) {
                ThrowUtils.throwIf(true, ErrorCode.SYSTEM_ERROR, "文件读取失败");
            }
        } else {
            ThrowUtils.throwIf(true, ErrorCode.SYSTEM_ERROR, "文件类型不支持");
        }

        return null;
    }
}
