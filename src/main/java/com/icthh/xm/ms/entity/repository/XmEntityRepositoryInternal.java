package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.projection.XmEntityVersion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Internal repository interface which never be published to LEP context.
 */
public interface XmEntityRepositoryInternal extends XmEntityRepository {

    Optional<XmEntityVersion> findVersionById(Long id);

    boolean existsByTypeKeyAndNameIgnoreCase(String typeKey, String name);

    List<XmEntity> findAll();

    Page<XmEntity> findAll(Pageable pageable);

    void flush();

    long count();

    void deleteAll();
}
