package com.icthh.xm.ms.entity.lep;

import com.icthh.lep.api.LepMethod;

/**
 * The {@link ProceedingLep} interface.
 */
public interface ProceedingLep extends LepMethod {

    Object proceed() throws Exception;

    Object proceed(Object[] args) throws Exception;

}
