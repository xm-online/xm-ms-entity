package com.icthh.xm.ms.entity.service;

import java.util.Map;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeparateTransactionExecutor {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> doInSeparateTransaction(Supplier<Map<String, Object>> task) {
        return task.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @SneakyThrows
    public <T> T doInSeparateTransaction(Task<T> task) {
        return task.doWork();
    }

    public interface Task<T> {
        T doWork() throws Exception;
    }

}
