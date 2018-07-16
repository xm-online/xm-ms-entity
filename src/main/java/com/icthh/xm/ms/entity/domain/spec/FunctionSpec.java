package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * The {@link FunctionSpec} class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"key", "name", "actionName", "allowedStateKeys", "withEntityId", "isShowFormWithoutData", "inputSpec", "inputForm",
    "contextDataSpec", "contextDataForm", "showResponse"})
@Data
public class FunctionSpec {

    /**
     * Unique in tenant function key.
     */
    @JsonProperty("key")
    private String key;

    /**
     * Localized action name via map where key is ISO 639-1 code.
     */
    @JsonProperty("name")
    private Map<String, String> name;

    /**
     * Localized action button name via map where key is ISO 639-1 code.
     */
    @JsonProperty("actionName")
    private Map<String, String> actionName;

    /**
     * List with allowed entity types.
     */
    @JsonProperty("allowedStateKeys")
    private List<String> allowedStateKeys;

    @JsonProperty("withEntityId")
    private Boolean withEntityId = false;

    @JsonProperty("isShowFormWithoutData")
    private Boolean showFormWithoutData;

    /**
     * Function input context specification (json-schema for {@code functionInput} arg, see FunctionExecutorService).
     */
    @JsonProperty("inputSpec")
    private String inputSpec;

    /**
     * Form layout for function input context specification (Formly layout for FunctionInput.data).
     */
    @JsonProperty("inputForm")
    private String inputForm;

    /**
     * Function context data field specification (json-schema for {@code FunctionContext.data field}).
     */
    @JsonProperty("contextDataSpec") // old: dataSpec
    private String contextDataSpec;

    /**
     * Form layout for function context data (Formly layout for FunctionContext.data).
     */
    @JsonProperty("contextDataForm") // old: dataForm
    private String contextDataForm;

    @JsonProperty("saveFunctionContext")
    private Boolean saveFunctionContext;

    @JsonProperty("showResponse")
    private Boolean showResponse;

    public Boolean getSaveFunctionContext() {
        return saveFunctionContext == null ? false : saveFunctionContext;
    }

}
