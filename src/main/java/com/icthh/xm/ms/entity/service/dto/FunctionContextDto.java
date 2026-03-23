package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityDtoObjectIdResolver;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ApiModel(description = "Represents function context with result of function execution.")
@Getter
@Setter
public class FunctionContextDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @ApiModelProperty(value = "Function context alphanumeric alias.", required = true)
    private String key;

    @NotNull
    @ApiModelProperty(value = "String typeKey with tree-like structure.", required = true)
    private String typeKey;

    @ApiModelProperty(value = "Function context description.")
    private String description;

    @ApiModelProperty(value = "Start date.")
    private Instant startDate;

    @ApiModelProperty(value = "Update date.")
    private Instant updateDate;

    @ApiModelProperty(value = "End date.")
    private Instant endDate;

    @ApiModelProperty(value = "Function context execution related data in JSON format. On DTO level data type should be Map<String,Object>")
    private Map<String, Object> data = new HashMap<>();

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto xmEntity;

    @JsonIgnore
    private boolean onlyData;

    @JsonIgnore
    private String binaryDataField;

    @Getter
    private String binaryDataType;

    public Object functionResult() {
        if (isBinaryData()) {
            return getBinaryData();
        }
        if (onlyData) {
            return data.get("data");
        }
        return this;
    }

    public boolean isBinaryData() {
        return binaryDataField != null;
    }

    public Object getBinaryData() {
        if (data == null) {
            return null;
        }
        return data.get(binaryDataField);
    }
}
