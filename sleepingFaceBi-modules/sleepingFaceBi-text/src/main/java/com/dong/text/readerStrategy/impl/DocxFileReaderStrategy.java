package com.dong.text.readerStrategy.impl;

import com.dong.common.utils.TxtUtils;
import com.dong.text.readerStrategy.FileReaderStrategy;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DocxFileReaderStrategy implements FileReaderStrategy {
    @Override
    public ArrayList<String> readFile(MultipartFile file) throws IOException {
        return TxtUtils.readerDocxFile(file);
    }
}
