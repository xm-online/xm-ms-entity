package com.icthh.xm.ms.entity.service.processor;

import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.processor.impl.DefinitionSpecProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * This class extends exiting DefinitionSpecProcessor to support inverse relationship
 */
@Slf4j
@Component
public class XmEntityDefinitionSpecProcessor extends DefinitionSpecProcessor {

    public static final String XM_ENTITY_DEFINITION = "xmEntityDefinition";

    private final XmEntityTypeSpecProcessor typeSpecProcessor;

    public XmEntityDefinitionSpecProcessor(JsonListenerService jsonListenerService,
                                           XmEntityTypeSpecProcessor typeSpecProcessor) {
        super(jsonListenerService);
        this.typeSpecProcessor = typeSpecProcessor;
    }

    @Override
    public void processDataSpecReferences(String tenant, String dataSpecKey, String spec, Map<String, Map<String, Object>> tenantDataSpecs) {
        super.processDataSpecReferences(tenant, dataSpecKey, spec, tenantDataSpecs);
        typeSpecProcessor.processDataSpecReferences(tenant, dataSpecKey, spec, tenantDataSpecs);
    }

    @Override
    public String getSectionName() {
        return XM_ENTITY_DEFINITION;
    }
}
