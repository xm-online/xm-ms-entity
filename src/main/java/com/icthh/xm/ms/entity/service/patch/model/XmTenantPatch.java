package com.icthh.xm.ms.entity.service.patch.model;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class XmTenantPatch {

    @Valid
    @NotEmpty
    private List<XmTenantChangeSet> databaseChangeLog = new ArrayList<>();

    public String liquibaseRepresentation(String tenantName) {
        return databaseChangeLog.stream()
                .map(it -> it.liquibaseRepresentation(tenantName))
                .collect(Collectors.joining("\n"));
    }
}
