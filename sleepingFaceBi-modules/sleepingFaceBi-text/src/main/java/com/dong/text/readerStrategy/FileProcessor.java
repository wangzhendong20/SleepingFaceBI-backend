package com.dong.text.readerStrategy;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class FileProcessor {
    public ArrayList<String> processFile(String suffix, MultipartFile multipartFile) throws IOException {

        FileReaderStrategy strategy = FileReaderFactory.getStrategy(suffix);

        if (strategy != null) {
            return strategy.readFile(multipartFile);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + suffix);
        }
    }
}
