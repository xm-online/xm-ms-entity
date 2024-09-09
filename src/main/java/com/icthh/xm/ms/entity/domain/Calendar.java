package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents calendar instance related to XmEntity.
 */
@ApiModel(description = "Represents calendar instance related to XmEntity.")
@Entity
@Table(name = "calendar")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@TypeKey
public class Calendar implements Serializable {

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
     * Calendar name.
     */
    @NotNull
    @ApiModelProperty(value = "Calendar name.", required = true)
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Calendar description
     */
    @ApiModelProperty(value = "Calendar description")
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

    @OneToMany(mappedBy = "calendar", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Event> events = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver =
        XmEntityObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    @JoinColumn(name = "xm_entity_id", nullable = false)
    private XmEntity xmEntity;

    @Getter
    @Setter
    @ApiModelProperty(value = "Timezone id")
    @Column(name = "time_zone_id")
    private String timeZoneId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public Calendar typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getName() {
        return name;
    }

    public Calendar name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public Calendar description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Calendar startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public Calendar endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public Calendar events(Set<Event> events) {
        this.events = events;
        return this;
    }

    public Calendar addEvents(Event event) {
        this.events.add(event);
        event.setCalendar(this);
        return this;
    }

    public Calendar removeEvents(Event event) {
        this.events.remove(event);
        event.setCalendar(null);
        return this;
    }

    public void setEvents(Set<Event> events) {
        this.events = events;
    }

    public XmEntity getXmEntity() {
        return xmEntity;
    }

    public Calendar xmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
        return this;
    }

    public Calendar timeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
        return this;
    }

    public void setXmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
    }

    @PrePersist
    @PreUpdate
    private void prePersist() {
        if (id == null && startDate == null) {
            startDate = Instant.now();
        }
        if (xmEntity != null) {
            this.xmEntity.updateXmEntityReference(this.events, Event::setAssigned);
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
        Calendar calendar = (Calendar) o;
        if (calendar.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), calendar.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Calendar{" +
            "id=" + getId() +
            ", typeKey='" + getTypeKey() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            "}";
    }

}
