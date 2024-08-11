package com.dong.common.configs.manager;

import com.dong.common.configs.config.ResizableCapacityLinkedBlockingQueue;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 线程池动态调整参数
 * 使用自定义的ResizableCapacityLinkedBlockingQueue队列，可以动态调整队列大小
 */

@Service
public class ThreadPoolExecutorManager {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    public void dynamicModify(int corePoolSize, int maximumPoolSize, int queueCapacity, boolean allowCoreThreadTimeout) {
        ThreadPoolExecutor executor = threadPoolExecutor;

        executor.setCorePoolSize(corePoolSize);
        executor.setMaximumPoolSize(maximumPoolSize);

        ResizableCapacityLinkedBlockingQueue<Runnable> queue = (ResizableCapacityLinkedBlockingQueue<Runnable>) executor.getQueue();
        queue.setCapacity(queueCapacity);

        executor.allowCoreThreadTimeOut(allowCoreThreadTimeout);
//        executor.prestartAllCoreThreads();
    }

}
