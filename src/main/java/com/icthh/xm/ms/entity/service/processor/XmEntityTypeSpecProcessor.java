package com.icthh.xm.ms.entity.service.processor;

import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.processor.DataSpecProcessor;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class XmEntityTypeSpecProcessor extends DataSpecProcessor<TypeSpec> {

    public static final String XM_ENTITY_DATA_SPEC = "xmEntityDataSpec";

    // tenantName -> specKey -> definitionKey -> DefinitionsSpec
    private final Map<String, Map<String, Map<String, TypeSpec>>> originalTypeSpecs = new ConcurrentHashMap<>();

    public XmEntityTypeSpecProcessor(JsonListenerService jsonListenerService) {
        super(jsonListenerService);
    }

    @Override
    public String getSectionName() {
        return XM_ENTITY_DATA_SPEC;
    }

    @Override
    public String getReferencePattern() {
        return "#/" + getSectionName() + "/*";
    }

    @Override
    public String getKeyTemplate() {
        return "#/" + getSectionName() + "/{key}";
    }

    @Override
    public void fullUpdateStateByTenant(String tenant, String dataSpecKey, Collection<TypeSpec> typeSpecs) {
        Map<String, TypeSpec> allTypeSpecs = toKeyMapOverrideDuplicates(typeSpecs);
        if (!allTypeSpecs.isEmpty()) {
            log.info("added {} type specs to tenant: {}", allTypeSpecs.size(), tenant);
            originalTypeSpecs
                .computeIfAbsent(tenant, s -> new HashMap<>())
                .put(dataSpecKey, allTypeSpecs);
        }
    }

    @Override
    public void processDataSpecReferences(String tenant, String dataSpecKey, String spec, Map<String, Map<String, Object>> tenantDataSpecs) {
        findDataSpecReferencesByPattern(spec, getReferencePattern()).forEach(ref ->
            processDefinition(
                tenant, dataSpecKey, tenantDataSpecs, ref,
                originalTypeSpecs,
                TypeSpec::getDataSpec
            )
        );
    }
}
