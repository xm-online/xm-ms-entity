package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the FunctionContext entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FunctionContextRepository extends JpaRepository<FunctionContext, Long> {

}
