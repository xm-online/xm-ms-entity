package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.projection.XmEntityVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Internal repository interface which never be published to LEP context.
 */
public interface XmEntityRepositoryInternal extends XmEntityRepository {

    Optional<XmEntityVersion> findVersionById(Long id);

    boolean existsByTypeKeyAndNameIgnoreCase(String typeKey, String name);

    List<XmEntity> findAll();

    void flush();

    Page<XmEntity> findAll(Pageable pageable);

    long count();

    void deleteAll();
}
