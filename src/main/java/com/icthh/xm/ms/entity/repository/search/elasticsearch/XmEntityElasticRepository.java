package com.icthh.xm.ms.entity.repository.search.elasticsearch;

import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface XmEntityElasticRepository {

    XmEntity save(XmEntity xmEntity);

    void saveAll(List<XmEntity> xmEntity);

    void delete(XmEntity xmEntity);

    long handleReindex(Function<Specification, Long> specificationFunction);

}
