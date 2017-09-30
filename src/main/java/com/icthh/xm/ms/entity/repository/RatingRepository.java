package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.Rating;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the Rating entity.
 */
@SuppressWarnings("unused")
@Repository
public interface RatingRepository extends JpaRepository<Rating,Long> {
    
}
