package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.converter.MapToStringConverter;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

/**
 * Represents function context with result of function execution.
 */
@ApiModel(description = "Represents function context with result of function execution.")
@Entity
@Table(name = "function_context")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class FunctionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /**
     * Function context alphanumeric alias.
     */
    @NotNull
    @ApiModelProperty(value = "Function context alphanumeric alias.", required = true)
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
     * Function context description.
     */
    @ApiModelProperty(value = "Function context description.")
    @Column(name = "description")
    private String description;

    /**
     * Start date.
     */
    @ApiModelProperty(value = "Start date.")
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
     * Function context execution related data in JSON format.
     * On DTO level data type should be Map<String,Object>
     */
    @ApiModelProperty(value = "Function context execution related data in JSON format. On DTO level data type should be Map<String,Object>")
    @Convert(converter = MapToStringConverter.class)
    @Column(name = "data")
    private Map<String, Object> data = new HashMap<>();

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver =
        XmEntityObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne
    @JoinColumn(name = "xm_entity_id")
    private XmEntity xmEntity;

    @Transient
    @Setter
    private transient boolean onlyData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public FunctionContext key(String key) {
        this.key = key;
        return this;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public FunctionContext typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getDescription() {
        return description;
    }

    public FunctionContext description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public FunctionContext startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getUpdateDate() {
        return updateDate;
    }

    public FunctionContext updateDate(Instant updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public void setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public FunctionContext endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public FunctionContext data(Map<String, Object> data) {
        this.data = data;
        return this;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }


    public XmEntity getXmEntity() {
        return xmEntity;
    }

    public FunctionContext xmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
        return this;
    }

    public void setXmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
    }

    @PrePersist
    private void prePersist() {
        if (id == null && startDate == null) {
            startDate = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FunctionContext functionContext = (FunctionContext) o;
        if (functionContext.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), functionContext.getId());
    }

    public Object functionResult() {
        if (onlyData) {
            return data.get("data");
        }
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "FunctionContext{" +
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
