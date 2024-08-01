package com.dong.text.readerStrategy;

import com.dong.text.config.ReadFileTypeConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FileReaderFactory implements ApplicationContextAware {

    @Resource
    private ReadFileTypeConfig readFileTypeConfig;

    private static Map<String, FileReaderStrategy> strategyMap = new ConcurrentHashMap<>();


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        readFileTypeConfig.getTypes().forEach((k, y) -> {
            strategyMap.put(k, (FileReaderStrategy) applicationContext.getBean(y));
        });
    }

    /**
     * 对外提供获取具体策略
     */
    public FileReaderStrategy getStrategy(String type) {
        FileReaderStrategy fileReaderStrategy = strategyMap.get(type);
        return fileReaderStrategy;
    }
}
