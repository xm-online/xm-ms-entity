package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.Content;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the Content entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ContentRepository extends JpaRepository<Content,Long> {
    
}
