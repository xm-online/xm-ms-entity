package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import com.icthh.xm.ms.entity.domain.serializer.SimpleLinkSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Bidirectional link between two XmEntites.
 */
@ApiModel(description = "Bidirectional link between two XmEntites.")
@Entity
@Table(name = "link")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JsonSerialize(using = SimpleLinkSerializer.class)
public class Link implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /**
     * String typeKey with tree-like structure.
     */
    @NotNull
    @ApiModelProperty(value = "String typeKey with tree-like structure.", required = true)
    @Column(name = "type_key", nullable = false)
    private String typeKey;

    /**
     * Link name
     */
    @ApiModelProperty(value = "Link name")
    @Column(name = "name")
    private String name;

    /**
     * Link description
     */
    @ApiModelProperty(value = "Link description")
    @Column(name = "description")
    private String description;

    /**
     * Start date
     */
    @ApiModelProperty(value = "Start date")
    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    /**
     * End date
     */
    @ApiModelProperty(value = "End date")
    @Column(name = "end_date")
    private Instant endDate;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "target_id", nullable = false)
    private XmEntity target;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver =
        XmEntityObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true) // otherwise first ref as POJO, others as id
    @JoinColumn(name = "source_id", nullable = false)
    private XmEntity source;

    @ApiModelProperty(value = "Order")
    @Column(name = "link_order")
    private Integer order;

    public Link() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public Link typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getName() {
        return name;
    }

    public Link name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public Link description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Link startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public Link endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public XmEntity getTarget() {
        return target;
    }

    public Link target(XmEntity xmEntity) {
        this.target = xmEntity;
        return this;
    }

    public void setTarget(XmEntity xmEntity) {
        this.target = xmEntity;
    }

    public XmEntity getSource() {
        return source;
    }

    public Link source(XmEntity xmEntity) {
        this.source = xmEntity;
        return this;
    }

    public void setSource(XmEntity xmEntity) {
        this.source = xmEntity;
    }

    public boolean linkFromSameEntity(Link link) {
        return this.getSource().getId().equals(link.getSource().getId());
    }

    @PrePersist
    private void prePersist() {
        if (startDate == null) {
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
        Link link = (Link) o;
        if (link.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), link.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Link{" +
            "id=" + getId() +
            ", typeKey='" + getTypeKey() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            "}";
    }
}
