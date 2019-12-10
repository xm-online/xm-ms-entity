package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Optional;

/**
 * REST controller for managing Profile.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileResource {

    private final ProfileService profileService;

    /**
     * GET  /profile : get the profile.
     *
     * @return the ResponseEntity with status 200 (OK) and with body the profile, or with status 404 (Not Found)
     */
    @GetMapping("/profile")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ENTITY.PROFILE.SELF.GET')")
    public ResponseEntity<XmEntity> getProfile() {
        Profile profile = profileService.getSelfProfile();
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(profile).map(Profile::getXmentity));
    }

    /**
     * GET  /profile : update the profile.
     *
     * @return the ResponseEntity with status 200 (OK) and with body the updated xmEntity, or with
     * status 400 (Bad Request) if the xmEntity is not valid, or with status 500 (Internal
     * Server Error) if the xmEntity couldn't be updated
     */
    @PutMapping("/profile")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ENTITY.PROFILE.SELF.UPDATE')")
    public ResponseEntity<XmEntity> updateProfile(@Valid @RequestBody XmEntity xmEntity) {
        XmEntity result = profileService.updateProfile(xmEntity);

        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/profile/{userKey}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'ENTITY.PROFILE.GET_LIST.ITEM')")
    public ResponseEntity<XmEntity> getProfile(@PathVariable("userKey") String userKey) {
        Profile profile = profileService.getProfile(userKey);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(profile).map(Profile::getXmentity));
    }

}
