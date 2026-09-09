package com.icthh.xm.ms.entity.service.search.db.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/** Body of the POST search endpoints; also built from query params by the GET variants. */
@Data
public class XmEntityDbSearchRequest {

    private String typeKey;
    private Boolean includeSubTypes = Boolean.TRUE;
    private String query;
    /** {@code <field>.<op>} → value(s); see FilterParser. */
    private Map<String, Object> filter = new HashMap<>();
    /** Set by the GET endpoints: filter values are raw strings whose types must be inferred. Not settable by clients. */
    @JsonIgnore
    private boolean rawStringValues;

    public boolean includeSubTypes() {
        return includeSubTypes == null || includeSubTypes;
    }
}
