package com.icthh.xm.ms.entity.config;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.FutureTask;

@Service
public class InternalTransactionService {

    @Autowired
    @Lazy
    private InternalTransactionService self;

    @SneakyThrows
    public <T> T inNestedTransaction(Task<T> task, Runnable setupMethod) {
        FutureTask<T> futureTask = new FutureTask<T>(() -> {
            setupMethod.run();
            return self.inTransaction(task);
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
