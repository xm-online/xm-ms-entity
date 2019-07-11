package com.icthh.xm.ms.entity.domain;

import java.util.Objects;

public class LinkDetails {

    private Long count;

    private String typeKey;

    private String sourceTypeKey;

    public LinkDetails(String typeKey, String sourceTypeKey, Long count) {
        this.typeKey = typeKey;
        this.sourceTypeKey = sourceTypeKey;
        this.count = count;
    }

    public LinkDetails count(Long count) {
        this.count = count;
        return this;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getSourceTypeKey() {
        return sourceTypeKey;
    }

    public void setSourceTypeKey(String sourceTypeKey) {
        this.sourceTypeKey = sourceTypeKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkDetails that = (LinkDetails) o;
        return Objects.equals(typeKey, that.typeKey) &&
            Objects.equals(sourceTypeKey, that.sourceTypeKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeKey, sourceTypeKey);
    }
}
