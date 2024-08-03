package com.dong.apiclientsdk.readerStrategy;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 抽象策略类
 */

public interface FileReaderStrategy {
    ArrayList<String> readFile(MultipartFile file) throws IOException;
}
