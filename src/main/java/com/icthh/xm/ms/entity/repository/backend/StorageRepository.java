package com.icthh.xm.ms.entity.repository.backend;

import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

public interface StorageRepository {

    String store(MultipartFile file, Integer size);

    String store(HttpEntity<Resource> httpEntity, Integer size);

    UploadResultDto store(Content content, String folderName, String fileName);

}
