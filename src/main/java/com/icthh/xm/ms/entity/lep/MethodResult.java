package com.icthh.xm.ms.entity.lep;

import com.icthh.lep.api.LepInvocationCauseException;

import javax.annotation.Nullable;
import java.util.Optional;

class MethodResult {

    @Nullable
    private final Object methodResult;

    @Nullable
    private final LepInvocationCauseException methodException;

    MethodResult(@Nullable Object methodResult, @Nullable LepInvocationCauseException methodException) {
        this.methodException = methodException;
        this.methodResult = methodResult;
    }

    static MethodResult valueOf(@Nullable Object methodResult) {
        return new MethodResult(methodResult, null);
    }

    static MethodResult valueOf(LepInvocationCauseException methodException) {
        return new MethodResult(null, methodException);
    }

    Object processResult() throws LepInvocationCauseException {
        if (getMethodException().isPresent()) {
            throw getMethodException().get();
        }
        return this.getMethodResult().orElse(null);
    }

    private Optional<LepInvocationCauseException> getMethodException() {
        return Optional.ofNullable(methodException);
    }

    private Optional<Object> getMethodResult() {
        return Optional.ofNullable(methodResult);
    }
}
