package com.icthh.xm.ms.entity.repository;

// TODO: 14-Jan-19 Use commons ResourceRepository after fix
//import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Spring Data JPA repository for the Vote entity.
 */
@SuppressWarnings("unused")
@Repository
public interface VoteRepository extends JpaRepository<Vote,Long>, ResourceRepository {

    @Override
    Vote findResourceById(Object id);
}
