package com.icthh.xm.ms.entity.service.patch.model;

import lombok.Data;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

import static com.icthh.xm.ms.entity.service.patch.TenantDbPatchService.XM_ENTITY_TABLE_NAME;
import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang.text.StrSubstitutor.replace;
import static org.apache.commons.lang3.StringUtils.isBlank;


@Data
public abstract class CreateIndexPatch extends XmTenantChangeSet {
    @NotNull
    @NotBlank
    private String indexName;
    @Nullable
    private Boolean unique;
    @Nullable
    private String condition;
    @Nullable
    private String indexType;
    private String tableName = XM_ENTITY_TABLE_NAME;

    @Override
    protected String changeSetBody(String tenantName) {
        String sql = "CREATE ${unique} INDEX IF NOT EXISTS ${indexName} ON ${tableName} ${indexType} (${indexExpression}) ${condition}";
        String tableName = isBlank(this.tableName) ? XM_ENTITY_TABLE_NAME : this.tableName;
        String schemaName = tenantName.toLowerCase();
        sql = replace(sql, Map.of(
                "unique", TRUE.equals(unique) ? "UNIQUE" : "",
                "indexName", indexName,
                "tableName", schemaName + "." + tableName,
                "indexType", isBlank(indexType) ? "" : "USING " + indexType,
                "indexExpression", getIndexExpression(),
                "condition", isBlank(condition) ? "" : "WHERE " + condition
        ));
        return "<sql>" + sql + "</sql>";
    }

    public abstract String getIndexExpression();
}
