package com.icthh.xm.ms.entity.domain;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.Instant;

/**
 * Represents simplified XM entity model for converting data to csv, xlsx, etc.
 */
@Getter
@Setter
public class SimpleExportXmEntityDto {
    private Long id;
    private String key;
    private String typeKey;
    private String stateKey;
    private String name;
    private Instant startDate;
    private Instant updateDate;
    private Instant endDate;
    private String avatarUrl;
    private String description;
    private Boolean removed;
    private String createdBy;

    public SimpleExportXmEntityDto() {
        // Empty public constructor used by Jackson.
    }

    public Long getOrElseId(Long value) {
        return id == null ? value : id;
    }

    public String getOrElseKey(String value) {
        return key == null ? value : key;
    }

    public String getOrElseTypeKey(String value) {
        return typeKey == null ? value : typeKey;
    }

    public String getOrElseStateKey(String value) {
        return stateKey == null ? value : stateKey;
    }

    public String getOrElseName(String value) {
        return name == null ? value : name;
    }

    public Instant getOrElseStartDate(Instant value) {
        return startDate == null ? value : startDate;
    }

    public Instant getOrElseUpdateDate(Instant value) {
        return updateDate == null ? value : updateDate;
    }

    public Instant getOrElseEndDate(Instant value) {
        return endDate == null ? value : endDate;
    }

    public String getOrElseAvatarUrl(String value) {
        return avatarUrl == null ? value : avatarUrl;
    }

    public String getOrElseDescription(String value) {
        return description == null ? value : description;
    }

    public Boolean isOrElseRemoved(Boolean value) {
        return removed == null ? value : removed;
    }

    public String getOrElseCreatedBy(String value) {
        return createdBy == null ? value : createdBy;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("SimpleExportXmEntityDto{ id", id)
            .append(" ,key", key)
            .append(" ,typeKey", typeKey)
            .append(" ,stateKey", stateKey)
            .append(" ,name", name)
            .append(" ,startDate", startDate)
            .append(" ,updateDate", updateDate)
            .append(" ,endDate", endDate)
            .append(" ,avatarUrl", avatarUrl)
            .append(" ,description", description)
            .append(" ,removed", removed)
            .append(" ,createdBy}", createdBy)
            .toString();
    }
}
