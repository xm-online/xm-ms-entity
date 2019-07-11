package com.icthh.xm.ms.entity.repository;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OneTimeFunctionKeyPool {

    private final BlockingQueue<OneTimeFunctionKey> ids;

    public OneTimeFunctionKeyPool(ApplicationProperties applicationProperties) {
        Integer poolSize = applicationProperties.getOneTimeFunctionsPoolSize();
        this.ids = new ArrayBlockingQueue<>(poolSize);
        List<OneTimeFunctionKey> ids = generate(OneTimeFunctionKey::new).limit(poolSize).collect(toList());
        this.ids.addAll(ids);
    }

    @SneakyThrows
    public OneTimeFunctionKey take() {
        OneTimeFunctionKey key = ids.take();
        log.debug("Take key {}", key);
        return key;
    }

    @SneakyThrows
    public void release(OneTimeFunctionKey key) {
        log.debug("Release key {}", key);
        ids.put(key);
    }

    public class OneTimeFunctionKey implements AutoCloseable {

        @Getter
        private final String id = UUID.randomUUID().toString().replace("-", "");;

        private volatile long lastUserTimeMillis;

        @Override
        public void close() {
            release(this);
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
