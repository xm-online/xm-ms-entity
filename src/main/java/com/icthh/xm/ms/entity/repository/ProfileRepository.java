package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;




/**
 * Spring Data JPA repository for the Profile entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Profile findOneByUserKey(String userKey);

    Profile findOneByXmentityId(@Param("entityId")Long entityId);
}
