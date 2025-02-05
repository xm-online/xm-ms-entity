package com.icthh.xm.ms.entity.service.impl.tx;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.function.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XmEntityRepositoryTxControlImpl implements XmEntityRepositoryTxControl {

    @Override
    @Transactional
    @IgnoreLogginAspect
    public Page<XmEntity> executeInTransaction(Supplier<Page<XmEntity>> executor) {
        return executor.get();
    }

    @Override
    @IgnoreLogginAspect
    @Transactional(readOnly = true)
    public Page<XmEntity> executeInReadOnlyTransaction(Supplier<Page<XmEntity>> executor) {
        return executor.get();
    }

    @Override
    @IgnoreLogginAspect
    public Page<XmEntity> executeWithNoTransaction(Supplier<Page<XmEntity>> executor) {
        return executor.get();
    }
}
