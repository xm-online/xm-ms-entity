package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.Link;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the Link entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LinkRepository extends JpaRepository<Link,Long> {
    
}
