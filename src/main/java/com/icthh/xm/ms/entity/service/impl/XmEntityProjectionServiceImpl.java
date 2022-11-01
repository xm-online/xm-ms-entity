package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.XmEntityProjectionRepository;
import com.icthh.xm.ms.entity.service.XmEntityProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class XmEntityProjectionServiceImpl implements XmEntityProjectionService {

    private final XmEntityProjectionRepository xmEntityProjectionRepository;

    @Transactional(readOnly = true)
    public Optional<XmEntityStateProjection> findStateProjection(IdOrKey idOrKey) {
        return findByIdOrKey(idOrKey,
            xmEntityProjectionRepository::findStateProjectionById,
            xmEntityProjectionRepository::findStateProjectionByKey);
    }

    @Transactional(readOnly = true)
    public Optional<XmEntityIdKeyTypeKey> findXmEntityIdKeyTypeKey(IdOrKey idOrKey) {
        return findByIdOrKey(idOrKey,
            xmEntityProjectionRepository::findOneIdKeyTypeKeyById,
            xmEntityProjectionRepository::findOneIdKeyTypeKeyByKey);
    }

    private <T> Optional<T> findByIdOrKey(IdOrKey idOrKey, Function<Long, T> byId, Function<String, T> byKey) {
        if (idOrKey == null) {
            return Optional.empty();
        }
        if (idOrKey.isId()) {
            return ofNullable(byId.apply(idOrKey.getId()));
        }
        return ofNullable(byKey.apply(idOrKey.getKey()));
    }

}
