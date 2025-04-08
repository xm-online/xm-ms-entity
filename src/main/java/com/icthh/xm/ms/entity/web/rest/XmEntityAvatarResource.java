package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.impl.XmEntityAvatarService;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

import static com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class XmEntityAvatarResource {

    private final XmEntityAvatarService xmEntityAvatarService;

    /**
     * Multipart update avatar.
     *
     * @param idOrKey       id or key of XmEntity
     * @param multipartFile multipart file with avatar
     * @return HTTP response
     * @throws IOException when IO error
     */
    @Timed
    @PostMapping("/xm-entities/{idOrKey}/avatar")
    @PreAuthorize("hasPermission({'idOrKey':#idOrKey, 'multipartFile':#multipartFile}, 'XMENTITY.AVATAR.UPDATE')")
    @PrivilegeDescription("Privilege to update avatar")
    public ResponseEntity<Void> updateAvatar(@PathVariable String idOrKey,
                                             @RequestParam("file") MultipartFile multipartFile)
        throws IOException {

        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(multipartFile);
        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.of(idOrKey), avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    // TODO remove this method after deal with hasPermission check for self
    @Timed
    @PostMapping("/xm-entities/self/avatar")
    @PreAuthorize("hasPermission({'multipartFile': #multipartFile}, 'XMENTITY.SELF.AVATAR.UPDATE')")
    @PrivilegeDescription("Privilege to update avatar of current user")
    public ResponseEntity<Void> updateSelfAvatar(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(multipartFile);
        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.SELF, avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    /**
     * Update avatar with simple PUT HTTP request.
     *
     * @param idOrKey  id or key of XmEntity
     * @param fileName avatar file name
     * @param request  HTTP servlet request
     * @return HTTP response
     * @throws IOException when IO error
     */
    @Timed
    @PutMapping(path = "/xm-entities/{idOrKey}/avatar", consumes = "image/*")
    @PreAuthorize("hasPermission({'idOrKey':#idOrKey, 'fileName':#fileName, 'request':#request}, 'XMENTITY.AVATAR.UPDATE')")
    @PrivilegeDescription("Privilege to update avatar")
    public ResponseEntity<Void> updateAvatar(@PathVariable String idOrKey,
                                             @RequestHeader(value = XM_HEADER_CONTENT_NAME, required = false)
                                             String fileName,
                                             HttpServletRequest request) throws IOException {
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(request, fileName);
        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.of(idOrKey), avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    // TODO remove this method after deal with hasPermission check for self
    @Timed
    @PutMapping(path = "/xm-entities/self/avatar", consumes = "image/*")
    @PreAuthorize("hasPermission({'fileName':#fileName, 'request':#request}, 'XMENTITY.SELF.AVATAR.UPDATE')")
    @PrivilegeDescription("Privilege to update avatar of current user")
    public ResponseEntity<Void> updateSelfAvatar(@RequestHeader(value = XM_HEADER_CONTENT_NAME, required = false)
                                                 String fileName,
                                                 HttpServletRequest request) throws IOException {
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(request, fileName);
        URI uri = xmEntityAvatarService.updateAvatar(IdOrKey.SELF, avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    private static ResponseEntity<Void> buildAvatarUpdateResponse(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        if (uri != null) {
            headers.setLocation(uri);
        }
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

}
