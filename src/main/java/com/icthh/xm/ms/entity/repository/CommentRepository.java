package com.icthh.xm.ms.entity.repository;

// TODO: 14-Jan-19 Use commons ResourceRepository after fix
//import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


/**
 * Spring Data JPA repository for the Comment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CommentRepository extends JpaRepository<Comment,Long>, ResourceRepository {

    @Override
    Comment findResourceById(Object id);
}
