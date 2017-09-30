package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Profile.
 */
@RestController
@RequestMapping("/api")
public class ProfileResource {

    private final Logger log = LoggerFactory.getLogger(ProfileResource.class);

    private final ProfileService profileService;

    public ProfileResource(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * GET  /profile : get the profile.
     *
     * @return the ResponseEntity with status 200 (OK) and with body the profile, or with status 404 (Not Found)
     */
    @GetMapping("/profile")
    @Timed
    public ResponseEntity<XmEntity> getProfile() {
        log.debug("REST request to get Profile");
        Profile profile = profileService.getProfile(TenantContext.getCurrent().getUserKey());
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(profile.getXmentity()));
    }

}
