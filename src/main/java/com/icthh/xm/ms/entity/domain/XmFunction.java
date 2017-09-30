package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.converter.MapToStringConverter;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents function that can be executed in the system.
 */
@ApiModel(description = "Represents function that can be executed in the system.")
@Entity
@Table(name = "xm_function")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "#{@indexName.prefix}xmfunction")
public class XmFunction implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /**
     * Function alfanumeric alias.
     */
    @NotNull
    @ApiModelProperty(value = "Function alfanumeric alias.", required = true)
    @Column(name = "jhi_key", nullable = false)
    private String key;

    /**
     * String typeKey with tree-like structure.
     */
    @NotNull
    @ApiModelProperty(value = "String typeKey with tree-like structure.", required = true)
    @Column(name = "type_key", nullable = false)
    private String typeKey;

    /**
     * Functio description.
     */
    @ApiModelProperty(value = "Functio description.")
    @Column(name = "description")
    private String description;

    /**
     * Start date.
     */
    @NotNull
    @ApiModelProperty(value = "Start date.", required = true)
    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    /**
     * Update date.
     */
    @ApiModelProperty(value = "Update date.")
    @Column(name = "update_date")
    private Instant updateDate;

    /**
     * End date.
     */
    @ApiModelProperty(value = "End date.")
    @Column(name = "end_date")
    private Instant endDate;

    /**
     * Function related data in JSON format.
     * On DTO level data type should be Map<String,Object>
     */
    @ApiModelProperty(value = "Function related data in JSON format. On DTO level data type should be Map<String,Object>")
    @Convert(converter = MapToStringConverter.class)
    @Column(name = "data")
    private Map<String, Object> data = new HashMap<>();

    @ManyToOne(optional = false)
    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver =
        XmEntityObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true) // otherwise first ref as POJO, others as id
    private XmEntity xmEntity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public XmFunction key(String key) {
        this.key = key;
        return this;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public XmFunction typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getDescription() {
        return description;
    }

    public XmFunction description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public XmFunction startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getUpdateDate() {
        return updateDate;
    }

    public XmFunction updateDate(Instant updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public void setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public XmFunction endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public XmFunction data(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public XmEntity getXmEntity() {
        return xmEntity;
    }

    public XmFunction xmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
        return this;
    }

    public void setXmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        XmFunction xmFunction = (XmFunction) o;
        if (xmFunction.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), xmFunction.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "XmFunction{" +
            "id=" + getId() +
            ", key='" + getKey() + "'" +
            ", typeKey='" + getTypeKey() + "'" +
            ", description='" + getDescription() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", updateDate='" + getUpdateDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            ", data='" + getData() + "'" +
            "}";
    }
}
