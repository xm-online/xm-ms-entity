package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@TypeKey
public class LocationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @ApiModelProperty(value = "Additional lateral identification for this location, could be used as reference to the external system")
    private String key;

    @NotNull
    @ApiModelProperty(value = "String typeKey with tree-like structure.", required = true)
    private String typeKey;

    @ApiModelProperty(value = "Country code.")
    private String countryKey;

    @ApiModelProperty(value = "GPS longityde in decimal format")
    private Double longitude;

    @ApiModelProperty(value = "GPS latitude in decimal format")
    private Double latitude;

    @ApiModelProperty(value = "Full address name")
    private String name;

    @ApiModelProperty(value = "Street and number, P.O. box, c/o")
    private String addressLine1;

    @ApiModelProperty(value = "Apartment, suite, unit, building, floor, etc.")
    private String addressLine2;

    @ApiModelProperty(value = "City name")
    private String city;

    @ApiModelProperty(value = "State, Province, Region")
    private String region;

    @ApiModelProperty(value = "ZIP code")
    private String zip;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto xmEntity;
}
