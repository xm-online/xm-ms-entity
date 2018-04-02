package com.icthh.xm.ms.entity.domain;

import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A Vote.
 */
@Entity
@Table(name = "vote")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "#{@indexName.prefix}vote")
public class Vote implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /**
     * Vote author user key.
     */
    @NotNull
    @ApiModelProperty(value = "Vote author user key", required = true)
    @Column(name = "user_key", nullable = false)
    private String userKey;

    /**
     * Vote value
     */
    @NotNull
    @ApiModelProperty(value = "Vote value", required = true)
    @Column(name = "jhi_value", nullable = false)
    private Double value;

    /**
     * Vote message
     */
    @ApiModelProperty(value = "Vote message")
    @Column(name = "message")
    private String message;

    /**
     * Entry date
     */
    @ApiModelProperty(value = "Entry date")
    @Column(name = "entry_date", nullable = false)
    private Instant entryDate;

    @ManyToOne
    private Rating rating;

    @ManyToOne(optional = false)
    @NotNull
    private XmEntity xmEntity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserKey() {
        return userKey;
    }

    public Vote userKey(String userKey) {
        this.userKey = userKey;
        return this;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public Double getValue() {
        return value;
    }

    public Vote value(Double value) {
        this.value = value;
        return this;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public Vote message(String message) {
        this.message = message;
        return this;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getEntryDate() {
        return entryDate;
    }

    public Vote entryDate(Instant entryDate) {
        this.entryDate = entryDate;
        return this;
    }

    public void setEntryDate(Instant entryDate) {
        this.entryDate = entryDate;
    }

    public Rating getRating() {
        return rating;
    }

    public Vote rating(Rating rating) {
        this.rating = rating;
        return this;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    public XmEntity getXmEntity() {
        return xmEntity;
    }

    public Vote xmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
        return this;
    }

    public void setXmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
    }

    @PrePersist
    private void prePersist() {
        if (id == null && entryDate == null) {
            entryDate = Instant.now();
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
        Vote vote = (Vote) o;
        if (vote.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), vote.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Vote{" +
            "id=" + getId() +
            ", userKey='" + getUserKey() + "'" +
            ", value='" + getValue() + "'" +
            ", message='" + getMessage() + "'" +
            ", entryDate='" + getEntryDate() + "'" +
            "}";
    }
}
