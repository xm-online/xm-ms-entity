package com.icthh.xm.ms.entity.service.patch.model;

import lombok.Data;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigInteger;


@Data
public class CreateSequencePatch extends XmTenantChangeSet {
    @NotNull
    @NotBlank
    private String sequenceName;
    @Nullable
    private BigInteger incrementBy;
    @Nullable
    private BigInteger startValue;
    @Nullable
    private BigInteger minValue;
    @Nullable
    private BigInteger maxValue;
    @Nullable
    private Boolean cycle;
    @Nullable
    private Boolean ordered;

    @Override
    protected String changeSetBody() {
        StringBuilder out = new StringBuilder();
        out.append("<createSequence ");
        appendIfNotNull(cycle, "cycle", out);
        appendIfNotNull(incrementBy, "incrementBy", out);
        appendIfNotNull(maxValue, "maxValue", out);
        appendIfNotNull(minValue, "minValue", out);
        appendIfNotNull(ordered, "ordered", out);
        appendIfNotNull(sequenceName, "sequenceName", out);
        appendIfNotNull(startValue, "startValue", out);
        out.append(" />");
        return out.toString();
    }

    private static void appendIfNotNull(Object value, String fieldName, StringBuilder out) {
        if (value != null) {
            out.append(fieldName).append("=\"").append(value).append("\"\n");
        }
    }
}
