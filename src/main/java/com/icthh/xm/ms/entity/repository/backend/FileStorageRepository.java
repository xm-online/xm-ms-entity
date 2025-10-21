package com.icthh.xm.ms.entity.repository.backend;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileStorageRepository implements StorageRepository {

    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;

    @Override
    public String store(MultipartFile file, Integer size) {
        return "";
    }

    @Override
    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        return "";
    }

    @Override
    public UploadResultDto store(Content content, String folderName, String fileName) {
        return null;
    }
}
