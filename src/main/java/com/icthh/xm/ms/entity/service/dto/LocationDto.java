package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityDtoObjectIdResolver;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Additional lateral identification for this location, could be used as reference to the external system")
    private String key;

    @NotNull
    @Schema(description = "String typeKey with tree-like structure.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String typeKey;

    @Schema(description = "Country code.")
    private String countryKey;

    @Schema(description = "GPS longityde in decimal format")
    private Double longitude;

    @Schema(description = "GPS latitude in decimal format")
    private Double latitude;

    @Schema(description = "Full address name")
    private String name;

    @Schema(description = "Street and number, P.O. box, c/o")
    private String addressLine1;

    @Schema(description = "Apartment, suite, unit, building, floor, etc.")
    private String addressLine2;

    @Schema(description = "City name")
    private String city;

    @Schema(description = "State, Province, Region")
    private String region;

    @Schema(description = "ZIP code")
    private String zip;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto xmEntity;
}
