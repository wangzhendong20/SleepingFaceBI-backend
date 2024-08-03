package com.dong.apiclientsdk.readerStrategy;

import com.dong.apiclientsdk.readerStrategy.DocFileReaderStrategy;
import com.dong.apiclientsdk.readerStrategy.DocxFileReaderStrategy;
import com.dong.apiclientsdk.readerStrategy.MdFileReaderStrategy;
import com.dong.apiclientsdk.readerStrategy.TxtFileReaderStrategy;

import java.util.HashMap;
import java.util.Map;

public class FileReaderFactory {
    private static final Map<String, FileReaderStrategy> strategyMap = new HashMap<>();

    static {
        strategyMap.put("txt", new TxtFileReaderStrategy());
        strategyMap.put("md", new MdFileReaderStrategy());
        strategyMap.put("doc", new DocFileReaderStrategy());
        strategyMap.put("docx", new DocxFileReaderStrategy());
    }

    public static FileReaderStrategy getStrategy(String suffix) {
        return strategyMap.get(suffix);
    }
}
