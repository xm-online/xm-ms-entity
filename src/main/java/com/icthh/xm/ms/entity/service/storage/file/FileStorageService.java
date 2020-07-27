package com.icthh.xm.ms.entity.service.storage.file;

import java.io.InputStream;

public interface FileStorageService {

    void save(String fileName, InputStream inputStream);
}
