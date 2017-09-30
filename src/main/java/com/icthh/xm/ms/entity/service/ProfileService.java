package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.repository.ProfileRepository;
import com.icthh.xm.ms.entity.repository.search.ProfileSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class ProfileService {

    private ProfileRepository profileRepository;

    private ProfileSearchRepository profileSearchRepository;

    private XmEntitySearchRepository entitySearchRepository;

    public ProfileService(
                    ProfileRepository profileRepository,
                    ProfileSearchRepository profileSearchRepository,
                    XmEntitySearchRepository entitySearchRepository) {
        this.profileRepository = profileRepository;
        this.profileSearchRepository = profileSearchRepository;
        this.entitySearchRepository = entitySearchRepository;
    }

    /**
     * Save a profile.
     *
     * @param profile the entity to save
     * @return the persisted entity
     */
    public Profile save(Profile profile) {

        Profile result = profileRepository.save(profile);
        profileSearchRepository.save(result);
        entitySearchRepository.save(result.getXmentity());
        return result;
    }

    /**
     * Get profile by entity id.
     * @param entityId entity id
     * @return profile
     */
    public Profile getByXmEntityId(Long entityId) {
        return profileRepository.findOneByXmentityId(entityId);
    }

    @Transactional(readOnly = true)
    public Profile getProfile(String userKey) {
        return profileRepository.findOneByUserKey(userKey);
    }

}
