package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.XmFunction;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.*;


/**
 * Spring Data JPA repository for the XmFunction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface XmFunctionRepository extends JpaRepository<XmFunction,Long> {
    
}
