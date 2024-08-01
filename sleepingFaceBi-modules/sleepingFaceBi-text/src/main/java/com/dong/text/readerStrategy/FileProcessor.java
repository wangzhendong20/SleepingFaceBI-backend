package com.dong.text.readerStrategy;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class FileProcessor {

    @Resource
    private FileReaderFactory fileReaderFactory;

    public ArrayList<String> processFile(String suffix, MultipartFile multipartFile) throws IOException {

        FileReaderStrategy strategy = fileReaderFactory.getStrategy(suffix);

        if (strategy != null) {
            return strategy.readFile(multipartFile);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + suffix);
        }
    }
}
