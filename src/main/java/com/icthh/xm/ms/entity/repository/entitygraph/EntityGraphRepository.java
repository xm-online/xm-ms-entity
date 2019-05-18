package com.icthh.xm.ms.entity.repository.entitygraph;

import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.List;
import java.util.Map;

public interface EntityGraphRepository {

    XmEntity findOne(Long id, List<String> embed);

    List<XmEntity> findAll(String jpql, Map<String, Object> args, List<String> embed);
}
