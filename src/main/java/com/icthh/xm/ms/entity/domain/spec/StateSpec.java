package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

import java.util.List;
import java.util.Map;
import lombok.experimental.Accessors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "key", "name", "icon", "color", "next" })
@Data
public class StateSpec {

    @JsonProperty("key")
    private String key;
    @JsonProperty("name")
    private Map<String, String> name;
    @JsonProperty("icon")
    private Object icon;
    @JsonProperty("color")
    private Object color;
    @JsonProperty("next")
    private List<NextSpec> next = null;

    public StateSpec key(String key) {
        this.key = key;
        return this;
    }

}
