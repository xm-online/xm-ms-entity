package com.icthh.xm.ms.entity.service;

import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeparateTransactionExecutor {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> doInSeparateTransaction(Supplier<Map<String, Object>> task) {
        return task.get();
    }
}
