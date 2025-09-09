package com.icthh.xm.ms.entity.domain.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import com.icthh.xm.commons.domain.spec.IFunctionSpec;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.utils.Constants.FUNCTION_CALL_PRIVILEGE;
import static com.icthh.xm.ms.entity.service.impl.XmEntityFunctionServiceFacade.XM_ENITITY_FUNCTION_CALL_PRIV;

/**
 * The {@link FunctionSpec} class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"key", "name", "actionName", "allowedStateKeys", "withEntityId", "isShowFormWithoutData", "inputSpec", "inputForm",
    "contextDataSpec", "contextDataForm", "showResponse", "onlyData", "validateFunctionInput", "txType", "tags", "httpMethods"})
@Data
public class FunctionSpec implements IFunctionSpec {

    /**
     * Unique in tenant function key.
     */
    @JsonProperty("key")
    private String key;

    /**
     * Unique in tenant HTTP path template.
     * May contain placeholders for path parameters.
     * Example: my/rest/entity/{id}
     */
    @JsonProperty("path")
    private String path;

    /**
     * Localized action name via map where key is ISO 639-1 code.
     */
    @JsonProperty("name")
    private Map<String, String> name;

    /**
     * Function action description.
     */
    @JsonProperty("description")
    private String description;

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

    @JsonProperty("onlyData")
    private Boolean onlyData;

    @JsonProperty("binaryDataField")
    private String binaryDataField;

    @JsonProperty("binaryDataType")
    private String binaryDataType;

    @JsonProperty("validateFunctionInput")
    private Boolean validateFunctionInput;

    @JsonProperty("anonymous")
    private Boolean anonymous;

    @JsonProperty("txType")
    private FunctionTxTypes txType = FunctionTxTypes.TX;

    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    @JsonProperty("httpMethods")
    private List<String> httpMethods = new ArrayList<>();

    public Boolean getSaveFunctionContext() {
        return saveFunctionContext != null && saveFunctionContext;
    }

    public Boolean getOnlyData() {
        return onlyData != null && onlyData;
    }

    @JsonIgnore
    public String getDynamicPrivilegeKey() {
        return getWithEntityId() ?
            XM_ENITITY_FUNCTION_CALL_PRIV.concat(".").concat(getKey()) :
            FUNCTION_CALL_PRIVILEGE.concat(".").concat(getKey());
    }

    @NotNull
    public Boolean getAnonymous() {
        return anonymous != null && anonymous;
    }

    @Override
    public Boolean getWrapResult() {
        return !getOnlyData();
    }
}
