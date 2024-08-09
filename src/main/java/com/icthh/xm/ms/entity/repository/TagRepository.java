package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Tag entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TagRepository extends JpaRepository<Tag, Long>, ResourceRepository<Tag, Long> {

    @Override
    Tag findResourceById(Long id);

    List<Tag> findByXmEntityTypeKeyAndTypeKeyIn(String entityTypeKey, List<String> typeKeys);
}
