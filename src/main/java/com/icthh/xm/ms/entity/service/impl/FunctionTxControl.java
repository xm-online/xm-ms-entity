package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.ms.entity.domain.FunctionContext;

import java.util.function.Supplier;

public interface FunctionTxControl {

    FunctionContext executeInTransaction(Supplier<FunctionContext> executor);

    FunctionContext executeInTransactionWithRoMode(Supplier<FunctionContext> executor);

    FunctionContext executeWithNoTx(Supplier<FunctionContext> executor);

}
