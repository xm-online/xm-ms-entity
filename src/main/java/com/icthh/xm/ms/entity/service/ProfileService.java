package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.ProfileRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@LepService(group = "service.profile")
@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    private final XmEntitySearchRepository entitySearchRepository;

    private final XmAuthenticationContextHolder authContextHolder;

    /**
     * Save a profile.
     *
     * @param profile the entity to save
     * @return the persisted entity
     */
    public Profile save(Profile profile) {
        return profileRepository.save(profile);
    }

    /**
     * Get profile by entity id.
     *
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

    @LogicExtensionPoint("GetSelfProfile")
    @Transactional(readOnly = true)
    public Profile getSelfProfile() {
        XmAuthenticationContext context = authContextHolder.getContext();
        if (!context.isFullyAuthenticated()) {
            throw new EntityNotFoundException("Can't get profile for not fully authenticated user");
        }

        String userKey = context.getRequiredUserKey();
        log.debug("Get profile for user key {}", userKey);
        return getProfile(userKey);
    }

    @LogicExtensionPoint("UpdateProfile")
    public XmEntity updateProfile(XmEntity entity) {
        Profile profile = getSelfProfile();

        XmEntity profileEntity = profile.getXmentity();

        //update profile data
        profileEntity.setData(entity.getData());

        save(profile);
        return profileEntity;
    }
}
