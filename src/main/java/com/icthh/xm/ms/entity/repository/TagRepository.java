package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.Tag;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the Tag entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TagRepository extends JpaRepository<Tag,Long> {
    
}
