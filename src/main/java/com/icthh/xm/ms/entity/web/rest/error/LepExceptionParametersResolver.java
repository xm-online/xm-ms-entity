package com.icthh.xm.ms.entity.web.rest.error;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Transactional(readOnly = true)
@LepService(group = "service.exceptiontranslator")
public class LepExceptionParametersResolver {

    @LogicExtensionPoint("ExtractParameters")
    public Map<String, String> extractParameters(Throwable throwable) {
        log.info("No handlers for extractParameters");
        return new HashMap<>();
    }
}
