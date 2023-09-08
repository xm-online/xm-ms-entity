package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.lep.api.LepManagementService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.FutureTask;

@Slf4j
@Service
public class InternalTransactionService {

    @Autowired
    @Lazy
    private InternalTransactionService self;

    @SneakyThrows
    public <T> T inNestedTransaction(Task<T> task, Runnable setupMethod) {
        FutureTask<T> futureTask = new FutureTask<T>(() -> {
            try {
                setupMethod.run();
                return self.inTransaction(task);
            } catch (Exception e) {
                log.error("Error during nested transaction", e);
                throw e;
            }
        });
        Thread t = new Thread(futureTask);
        t.start();
        t.join();
        return futureTask.get();
    }

    @Transactional
    @SneakyThrows
    public <T> T inTransaction(Task<T> task) {
        return task.doWork();
    }

    public interface Task<T> {
        T doWork() throws Exception;
    }

}
