package com.icthh.xm.ms.entity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LifecycleLepStrategyFactory {

    private final LifecycleLepStrategy lifecycleLepStrategy;

    public LifecycleLepStrategy getLifecycleLepStrategy() {
        return lifecycleLepStrategy;
    }
}
