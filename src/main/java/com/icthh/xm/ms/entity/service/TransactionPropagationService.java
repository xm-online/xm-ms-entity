package com.icthh.xm.ms.entity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

public class TransactionPropagationService<T> {

    protected T self;

    /**
     * A trick to generate separate transaction for lep execution.
     * @param self link for the same service instance
     */
    @Autowired
    public void setSelf(@Lazy T self) {
        if (this.self == null) {
            this.self = self;
        }
    }
}
