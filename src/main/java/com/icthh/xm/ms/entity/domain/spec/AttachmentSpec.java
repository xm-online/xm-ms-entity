package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.icthh.xm.ms.entity.domain.AttachmentStoreType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "key", "name", "contentTypes", "max", "size", "storeType"})
@Data
public class AttachmentSpec {

    @JsonProperty("key")
    private String key;
    @JsonProperty("name")
    private Map<String, String> name = null;
    @JsonProperty("contentTypes")
    private List<String> contentTypes = null;
    @JsonProperty("max")
    private Integer max;
    @JsonProperty("size")
    private String size;
    @JsonProperty("storeType")
    private AttachmentStoreType storeType;
}
