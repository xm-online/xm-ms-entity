package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.CalendarObjectIdResolver;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import com.icthh.xm.ms.entity.validator.EventDataTypeKey;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import static jakarta.persistence.CascadeType.REMOVE;

/**
 * A Event.
 */
@Entity
@Table(name = "event")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
@EventDataTypeKey
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /**
     * String typeKey with tree-like structure.
     */
    @NotNull
    @Column(name = "type_key")
    private String typeKey;

    /**
     * Configuration for event repetition.
     */
    @Column(name = "repeat_rule_key")
    private String repeatRuleKey;

    /**
     * Event title.
     */
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Event description.
     */
    @Column(name = "description")
    private String description;

    /**
     * Start date.
     */
    @Column(name = "start_date")
    private Instant startDate;

    /**
     * End date.
     */
    @Column(name = "end_date")
    private Instant endDate;

    @ManyToOne
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver =
        CalendarObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private Calendar calendar;

    @ManyToOne
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver =
        XmEntityObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntity assigned;

    /**
     * Reference to {@link XmEntity} which stores extra data regarding to this {@link Event}.
     */
    @OneToOne(cascade = REMOVE)
    @JoinColumn(name = "event_data_ref_id", unique = true)
    private XmEntity eventDataRef;

    /**
     * Event color. Override default color from {@link com.icthh.xm.ms.entity.domain.spec.EventSpec}
     */
    private String color;

    public Event typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public Event repeatRuleKey(String repeatRuleKey) {
        this.repeatRuleKey = repeatRuleKey;
        return this;
    }

    public Event title(String title) {
        this.title = title;
        return this;
    }

    public Event description(String description) {
        this.description = description;
        return this;
    }

    public Event startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public Event endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public Event calendar(Calendar calendar) {
        this.calendar = calendar;
        return this;
    }

    public Event assigned(XmEntity xmEntity) {
        this.assigned = xmEntity;
        return this;
    }

    public Event eventDataRef(XmEntity xmEntity) {
        this.eventDataRef = xmEntity;
        return this;
    }

    public Event color(String color) {
        this.color = color;
        return this;
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
        Event event = (Event) o;
        if (event.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), event.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Event{"
            + "id=" + getId()
            + ", typeKey='" + getTypeKey() + "'"
            + ", repeatRuleKey='" + getRepeatRuleKey() + "'"
            + ", title='" + getTitle() + "'"
            + ", description='" + getDescription() + "'"
            + ", startDate='" + getStartDate() + "'"
            + ", endDate='" + getEndDate() + "'"
            + ", color='" + getColor() + "'"
            + "}";
    }
}
