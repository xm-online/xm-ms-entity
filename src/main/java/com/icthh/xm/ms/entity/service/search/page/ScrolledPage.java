package com.icthh.xm.ms.entity.service.search.page;

import org.springframework.data.domain.Page;

public interface ScrolledPage<T> extends Page<T> {

    String getScrollId();

}
