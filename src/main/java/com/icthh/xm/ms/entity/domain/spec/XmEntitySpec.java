package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.FormSpec;
import com.icthh.xm.commons.domain.SpecWithDefinitionAndForm;
import lombok.Data;

import java.util.Collection;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"types", "definitions", "forms"})
@Data
public class XmEntitySpec implements SpecWithDefinitionAndForm {

    @JsonProperty("types")
    private List<TypeSpec> types = null;

    @JsonProperty("definitions")
    private List<DefinitionSpec> definitions = null;

    @JsonProperty("forms")
    private List<FormSpec> forms = null;

    @Override
    public Collection<TypeSpec> getSpecifications() {
        return this.types;
    }
}
