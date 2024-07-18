package com.dong.text.readerStrategy;

import com.dong.text.readerStrategy.impl.DocFileReaderStrategy;
import com.dong.text.readerStrategy.impl.DocxFileReaderStrategy;
import com.dong.text.readerStrategy.impl.MdFileReaderStrategy;
import com.dong.text.readerStrategy.impl.TxtFileReaderStrategy;

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
