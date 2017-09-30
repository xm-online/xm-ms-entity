package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;
import lombok.Data;

/**
 * The {@link FunctionSpec} class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"key", "name", "isShowFormWithoutData", "dataSpec", "dataForm"})
@Data
public class FunctionSpec {

    @JsonProperty("key")
    private String key;

    /**
     * Localized action name via map where key is ISO 639-1 code.
     */
    @JsonProperty("name")
    private Map<String, String> name;

    @JsonProperty("isShowFormWithoutData")
    private Boolean showFormWithoutData;

    /**
     * Function input context specification.
     */
    @JsonProperty("contextSpec")
    private String contextSpec = null;

    @JsonProperty("dataSpec")
    private String dataSpec = null;

    @JsonProperty("dataForm")
    private String dataForm = null;

}
