package com.icthh.xm.ms.entity.service.patch.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

import static com.icthh.xm.ms.entity.service.patch.TenantDbPatchService.XM_ENTITY_TABLE_NAME;
import static org.apache.commons.lang.text.StrSubstitutor.replace;
import static org.apache.commons.lang3.StringUtils.isBlank;


@Data
public class DropIndexPatch extends XmTenantChangeSet {
    @NotNull
    @NotBlank
    private String indexName;
    private String tableName = XM_ENTITY_TABLE_NAME;

    @Override
    protected String changeSetBody() {
        return replace("<dropIndex indexName=\"${indexName}\" tableName=\"${tableName}\"/>", Map.of(
                "indexName", indexName,
                "tableName", isBlank(tableName) ? XM_ENTITY_TABLE_NAME : tableName
        ));
    }
}
