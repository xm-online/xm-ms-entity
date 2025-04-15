package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST controller for managing Storage objects.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/storage")
public class StorageResource {

    private final StorageService storageService;
    private final ApplicationProperties applicationProperties;


    /**
     * Api for uploading data to object storage.
     * @param size used to resize avatar (legacy)
     */
    @PostMapping("/objects")
    @Timed
    @PreAuthorize("hasPermission({'size': #size, 'multipartFile': #multipartFile}, 'STORAGE.OBJECT.CREATE')")
    @PrivilegeDescription("Privilege to create object on S3 or other supported storage")
    public ResponseEntity<String> createContent(
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false, defaultValue = "true") boolean isAvatar,
        @RequestParam("file") MultipartFile multipartFile)
        throws IOException {

        if (multipartFile.getSize() > applicationProperties.getMaxAvatarSize()) {
            throw new BusinessException(ERR_VALIDATION,
                "Avatar file must not exceed " + applicationProperties.getMaxAvatarSize() + " bytes");
        }

        final String contentType = multipartFile.getContentType();
        if (isAvatar && contentType != null && "image".equals(contentType.substring(0, 5))) {
            String result = storageService.storeAvatar(multipartFile, size);
            return ResponseEntity.ok(result);

        }

        String result = storageService.store(multipartFile, null);
        return ResponseEntity.ok(result);
    }

}
