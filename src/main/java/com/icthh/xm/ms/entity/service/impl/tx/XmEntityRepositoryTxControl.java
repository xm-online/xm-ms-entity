package com.icthh.xm.ms.entity.service.impl.tx;

import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.function.Supplier;
import org.springframework.data.domain.Page;

public interface XmEntityRepositoryTxControl {

    Page<XmEntity> executeInTransaction(Supplier<Page<XmEntity>> executor);

    Page<XmEntity> executeInReadOnlyTransaction(Supplier<Page<XmEntity>> executor);

    Page<XmEntity> executeWithNoTransaction(Supplier<Page<XmEntity>> executor);
}
