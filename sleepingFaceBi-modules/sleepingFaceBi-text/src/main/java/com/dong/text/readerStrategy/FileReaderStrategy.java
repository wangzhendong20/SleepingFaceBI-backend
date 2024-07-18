package com.dong.text.readerStrategy;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface FileReaderStrategy {
    ArrayList<String> readFile(MultipartFile file) throws IOException;
}
