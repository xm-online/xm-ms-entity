package com.icthh.xm.ms.entity.domain.spec;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.icthh.xm.ms.entity.service.ISpecVisitor;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "key", "name" })
@Data
public class TagSpec implements IUiEvaluatedSpec {

    @JsonProperty("key")
    private String key;
    @JsonProperty("name")
    private Map<String, String> name;
    @JsonProperty("uiActionSpec")
    private Set<UiActionSpec> uiActionSpec;

    @Override
    public void accept(ISpecVisitor visitor) {
        setUiActionSpec(visitor.visit(this));
    }
}
