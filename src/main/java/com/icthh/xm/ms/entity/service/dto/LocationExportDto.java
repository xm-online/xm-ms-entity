package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class LocationExportDto {

    private Long id;
    private String typeKey;
    private String countryKey;
    private Double longitude;
    private Double latitude;
    private String name;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;
    private String zip;
    private Long entityId;

    public LocationExportDto(Location location) {

        if (location != null) {
            this.id = location.getId();
            this.typeKey = location.getTypeKey();
            this.countryKey = location.getCountryKey();
            this.longitude = location.getLongitude();
            this.latitude = location.getLatitude();
            this.name = location.getName();
            this.addressLine1 = location.getAddressLine1();
            this.addressLine2 = location.getAddressLine2();
            this.city = location.getCity();
            this.region = location.getRegion();
            this.zip = location.getZip();
            this.entityId = Optional.ofNullable(location.getXmEntity()).map(XmEntity::getId).orElse(null);
        }
    }

    public Location toLocation(XmEntity entity) {
        Location location = new Location();
        location.setTypeKey(this.getTypeKey());
        location.setCountryKey(this.getCountryKey());
        location.setLongitude(this.getLongitude());
        location.setLatitude(this.getLatitude());
        location.setName(this.getName());
        location.setAddressLine1(this.getAddressLine1());
        location.setAddressLine2(this.getAddressLine2());
        location.setCity(this.getCity());
        location.setRegion(this.getRegion());
        location.setZip(this.getZip());
        location.setXmEntity(entity);
        return location;
    }
}
