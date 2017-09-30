package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.errors.ErrorConstants.ERR_VALIDATION;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.errors.exception.BusinessException;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.repository.backend.StorageRepository;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for managing Storage objects.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/storage")
public class StorageResource {

    private final StorageRepository storageRepository;
    private final ApplicationProperties applicationProperties;

    @PostMapping("/objects")
    @Timed
    public ResponseEntity<String> createContent(@RequestParam(required = false) Integer size,
        @RequestParam("file") MultipartFile multipartFile)
        throws URISyntaxException {
        if (multipartFile.getSize() > applicationProperties.getMaxAvatarSize()) {
            throw new BusinessException(ERR_VALIDATION,
                "Avatar file must not exceed " + applicationProperties.getMaxAvatarSize() + " bytes");
        }
        String result = storageRepository.store(multipartFile, size);
        return ResponseEntity.ok(result);
    }
}
