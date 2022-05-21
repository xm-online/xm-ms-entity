package com.icthh.xm.ms.entity.service.patch.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

import static org.apache.commons.lang.text.StrSubstitutor.replace;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;


@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "operationType")
public abstract class XmTenantChangeSet {
    private String author;
    @NotNull
    @NotBlank
    private String changeSetId;

    public final String liquibaseRepresentation(String tenantName) {
        StringBuilder patch = new StringBuilder();
        String changeSet = replace("<changeSet author=\"${author}\" id=\"${changeSetId}\">", Map.of(
                "author", defaultIfBlank(author, tenantName),
                "changeSetId", changeSetId
        ));
        patch.append(changeSet);
        patch.append(changeSetBody(tenantName));
        patch.append("</changeSet>");
        return patch.toString();
    }

    protected abstract String changeSetBody(String tenantName);
}
