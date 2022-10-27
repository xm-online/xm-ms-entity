package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.XmEntityProjectionRepository;
import com.icthh.xm.ms.entity.service.XmEntityProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class XmEntityProjectionServiceImpl implements XmEntityProjectionService {

    private final XmEntityProjectionRepository xmEntityRepository;

    @Transactional(readOnly = true)
    public Optional<XmEntityStateProjection> findStateProjectionById(IdOrKey idOrKey) {
        XmEntityStateProjection projection;
        if (idOrKey.isId()) {
            // ID case
            projection = xmEntityRepository.findStateProjectionById(idOrKey.getId());
        } else {
            // KEY case
            projection = xmEntityRepository.findStateProjectionByKey(idOrKey.getKey());
        }
        return ofNullable(projection);
    }

}
