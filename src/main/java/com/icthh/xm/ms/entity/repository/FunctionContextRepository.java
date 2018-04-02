package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the FunctionContext entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FunctionContextRepository extends JpaRepository<FunctionContext,Long> {
    
}
