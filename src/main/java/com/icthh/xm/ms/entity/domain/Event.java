package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.CalendarObjectIdResolver;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A Event.
 */
@Entity
@Table(name = "event")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "#{@indexName.prefix}event")
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
    @Field(index = FieldIndex.not_analyzed, type = FieldType.String)
    private String typeKey;

    /**
     * Configuration for event repetition
     */
    @ApiModelProperty(value = "Configuration for event repetition")
    @Column(name = "repeat_rule_key")
    private String repeatRuleKey;

    /**
     * Event title
     */
    @NotNull
    @ApiModelProperty(value = "Event title", required = true)
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Event description
     */
    @ApiModelProperty(value = "Event description")
    @Column(name = "description")
    private String description;

    /**
     * Start date
     */
    @ApiModelProperty(value = "Start date")
    @Column(name = "start_date")
    private Instant startDate;

    /**
     * End date
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public Event typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getRepeatRuleKey() {
        return repeatRuleKey;
    }

    public Event repeatRuleKey(String repeatRuleKey) {
        this.repeatRuleKey = repeatRuleKey;
        return this;
    }

    public void setRepeatRuleKey(String repeatRuleKey) {
        this.repeatRuleKey = repeatRuleKey;
    }

    public String getTitle() {
        return title;
    }

    public Event title(String title) {
        this.title = title;
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public Event description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Event startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public Event endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public Event calendar(Calendar calendar) {
        this.calendar = calendar;
        return this;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public XmEntity getAssigned() {
        return assigned;
    }

    public Event assigned(XmEntity xmEntity) {
        this.assigned = xmEntity;
        return this;
    }

    public void setAssigned(XmEntity xmEntity) {
        this.assigned = xmEntity;
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
        return "Event{" +
            "id=" + getId() +
            ", typeKey='" + getTypeKey() + "'" +
            ", repeatRuleKey='" + getRepeatRuleKey() + "'" +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            "}";
    }
}
