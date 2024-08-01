package com.dong.text.readerStrategy;

import com.dong.text.readerStrategy.DocFileReaderStrategy;
import com.dong.text.readerStrategy.DocxFileReaderStrategy;
import com.dong.text.readerStrategy.MdFileReaderStrategy;
import com.dong.text.readerStrategy.TxtFileReaderStrategy;

import java.util.HashMap;
import java.util.Map;

public class FileReaderFactory2 {
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
