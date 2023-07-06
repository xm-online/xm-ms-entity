package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
public class FunctionTxControlImpl implements FunctionTxControl {
    @Override
    @Transactional
    public FunctionContext executeInTransaction(Supplier<FunctionContext> executor) {
        return executor.get();
    }

    @Override
    @Transactional(readOnly = true)
    public FunctionContext executeInTransactionWithRoMode(Supplier<FunctionContext> executor) {
        return executor.get();
    }

    @Override
    public FunctionContext executeWithNoTx(Supplier<FunctionContext> executor) {
        return executor.get();
    }

}
