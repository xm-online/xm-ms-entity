package com.icthh.xm.ms.entity.service.search.page;

import org.springframework.data.domain.Page;

public interface ScoredPage<T> extends Page<T> {

    float getMaxScore();
}

