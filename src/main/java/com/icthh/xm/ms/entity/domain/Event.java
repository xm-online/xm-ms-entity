package com.icthh.xm.ms.entity.domain;

import static javax.persistence.CascadeType.REMOVE;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.CalendarObjectIdResolver;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import com.icthh.xm.ms.entity.validator.EventDataTypeKey;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
    @ApiModelProperty(value = "String typeKey with tree-like structure.")
    @Column(name = "type_key")
    private String typeKey;

    /**
     * Configuration for event repetition.
     */
    @ApiModelProperty(value = "Configuration for event repetition")
    @Column(name = "repeat_rule_key")
    private String repeatRuleKey;

    /**
     * Event title.
     */
    @NotNull
    @ApiModelProperty(value = "Event title", required = true)
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Event description.
     */
    @ApiModelProperty(value = "Event description")
    @Column(name = "description")
    private String description;

    /**
     * Start date.
     */
    @ApiModelProperty(value = "Start date")
    @Column(name = "start_date")
    private Instant startDate;

    @ApiModelProperty(value = "Timezone id")
    @Column(name = "time_zone_id")
    private String timeZoneId;

    /**
     * End date.
     */
    @ApiModelProperty(value = "End date")
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
    @ApiModelProperty(value = "Reference to event's extra data")
    @OneToOne(cascade = REMOVE)
    @JoinColumn(name = "event_data_ref_id", unique = true)
    private XmEntity eventDataRef;

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
            + "}";
    }
}
