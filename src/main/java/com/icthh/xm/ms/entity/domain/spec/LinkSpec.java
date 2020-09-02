package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "key", "builderType", "name", "backName", "icon", "typeKey", "max", "isUnique" })
@Data
public class LinkSpec {

    public static final String NEW_BUILDER_TYPE = "NEW";
    public static final String SEARCH_BUILDER_TYPE = "SEARCH";

    @JsonProperty("key")
    private String key;
    /**
     * Link constructor on interface. It could be: NEW - add new xmEntity dialog will be shown, SEARCH - xmEntity
     * search dialog will be show.
     * */
    @JsonProperty("builderType")
    private String builderType;
    @JsonProperty("name")
    private Map<String, String> name;
    /** Name of Google material system icon https://material.io/icons/ */
    @JsonProperty("icon")
    private String icon;
    @JsonProperty("typeKey")
    private String typeKey;
    @JsonProperty("max")
    private Integer max;

    @JsonProperty("backName")
    private Map<String, String> backName;

    /**
     * Flag that disallow create two same links.
     * WARNING: only affects search links, checking for unique is not implemented now
     * */
    @JsonProperty("isUnique")
    private Boolean isUnique;

}
