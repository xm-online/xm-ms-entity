package com.icthh.xm.ms.entity.service.search.db.dto;

import static java.lang.Boolean.FALSE;

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
    /**
     * Set by the GET endpoints only, never by clients: filter values arrived as query-string text and their
     * types are inferred before comparison. Examples:
     * <ul>
     *     <li>{@code ?data.orderNo.in=1,2,3} → {@code [1L, 2L, 3L]} (comma-separated list, numbers)</li>
     *     <li>{@code ?data.price.gt=10.5} → {@code 10.5d}</li>
     *     <li>{@code ?data.active.eq=true} → {@code Boolean.TRUE}</li>
     *     <li>{@code ?stateKey.eq=ACTIVE} → {@code "ACTIVE"}</li>
     * </ul>
     * With the flag off (POST body) values keep their JSON types: {@code "filter": {"data.orderNo.in": [1, 2, 3]}}.
     */
    @JsonIgnore
    private boolean rawStringValues;
    /**
     * Skip the {@code count} query and the {@code X-Total-Count} header: body field on POST, query parameter
     * {@code skip-total-count} on GET. The count is the expensive part of a broad full text query over a large
     * table, the page itself is cheap.
     */
    private boolean skipTotalCount;

    public boolean includeSubTypes() {
        return !FALSE.equals(includeSubTypes);
    }
}
