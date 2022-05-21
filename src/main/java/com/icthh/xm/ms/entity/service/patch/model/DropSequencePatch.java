package com.icthh.xm.ms.entity.service.patch.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

import static org.apache.commons.lang.text.StrSubstitutor.replace;


@Data
public class DropSequencePatch extends XmTenantChangeSet {
    @NotNull
    @NotBlank
    private String sequenceName;

    @Override
    protected String changeSetBody(String tenantName) {
        return replace("<dropSequence sequenceName=\"${sequenceName}\" />", Map.of(
                "sequenceName", sequenceName.toLowerCase()
        ));
    }
}
