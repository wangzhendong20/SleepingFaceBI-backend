package com.dong.apiclientsdk.readerStrategy;

import com.dong.apiclientsdk.utils.TxtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class DocFileReaderStrategy implements FileReaderStrategy {
    @Override
    public ArrayList<String> readFile(MultipartFile file) throws IOException {
        return TxtUtils.readerDocFile(file);
    }
}
