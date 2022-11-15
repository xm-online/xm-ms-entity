package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.XmEntityProjectionRepository;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.XmEntityProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class XmEntityProjectionServiceImpl implements XmEntityProjectionService {

    private final XmEntityProjectionRepository xmEntityProjectionRepository;
    private final ProfileService profileService;

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
        if (idOrKey.isSelf()) {
            XmEntity profile = profileService.getSelfProfile().getXmentity();
            T projection = byId.apply(profile.getId());
            if (projection == null) {
                throw new EntityNotFoundException("XmEntity with key [" + profile.getId() + "] not found");
            }
            return of(projection);
        }
        return ofNullable(byKey.apply(idOrKey.getKey()));
    }

}
