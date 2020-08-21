package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Attachment;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Attachment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long>, ResourceRepository {

    @Override
    Attachment findResourceById(Object id);

    int countByXmEntityIdAndTypeKey(long xmEntityId, String typeKey);

    static Attachment enrich(Attachment att) {
        Hibernate.initialize(att.getContent());
        return att;
    }

    List<Attachment> findByXmEntityTypeKeyAndTypeKeyIn(String entityTypeKey, List<String> typeKeys);
}
