package com.icthh.xm.ms.entity.repository.backend;


import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@LepService(group = "storage.prefix")
@Slf4j
@RequiredArgsConstructor
public class FilePrefixNameFactory {

    /**
     * Output could contain only ALPHABETICAL numbers and '/'
     * Should not end on '/'
     * @return EMPTY of '/{folder}' or '/{folder}/{folder}'
     */
    @LogicExtensionPoint("PrepareFolderName")
    public String evaluatePathSubfolder() {
        return StringUtils.EMPTY;
    }

}
