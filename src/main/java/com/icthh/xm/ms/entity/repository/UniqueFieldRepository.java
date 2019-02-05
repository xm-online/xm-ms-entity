package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.UniqueField;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UniqueField entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UniqueFieldRepository extends JpaRepository<UniqueField,Long> {
    @Modifying
    @Query("DELETE FROM UniqueField u where u.xmEntity = ?1")
    void deleteByXmEntity(XmEntity xmEntity);
}
