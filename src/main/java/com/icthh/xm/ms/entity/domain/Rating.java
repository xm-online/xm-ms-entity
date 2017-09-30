package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A Rating.
 */
@Entity
@Table(name = "rating")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "#{@indexName.prefix}rating")
@TypeKey
public class Rating implements Serializable {

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
     * Rating value
     */
    @ApiModelProperty(value = "Rating value")
    @Column(name = "jhi_value")
    private Double value;

    /**
     * Start date
     */
    @NotNull
    @ApiModelProperty(value = "Start date", required = true)
    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    /**
     * End date
     */
    @ApiModelProperty(value = "End date")
    @Column(name = "end_date")
    private Instant endDate;

    @OneToMany(mappedBy = "rating")
    @JsonIgnore
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Vote> votes = new HashSet<>();

    @ManyToOne(optional = false)
    // @NotNull - can be null when create new XmEntity with ratings
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

    public String getTypeKey() {
        return typeKey;
    }

    public Rating typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public Double getValue() {
        return value;
    }

    public Rating value(Double value) {
        this.value = value;
        return this;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Rating startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public Rating endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Set<Vote> getVotes() {
        return votes;
    }

    public Rating votes(Set<Vote> votes) {
        this.votes = votes;
        return this;
    }

    public Rating addVotes(Vote vote) {
        this.votes.add(vote);
        vote.setRating(this);
        return this;
    }

    public Rating removeVotes(Vote vote) {
        this.votes.remove(vote);
        vote.setRating(null);
        return this;
    }

    public void setVotes(Set<Vote> votes) {
        this.votes = votes;
    }

    public XmEntity getXmEntity() {
        return xmEntity;
    }

    public Rating xmEntity(XmEntity xmEntity) {
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
        Rating rating = (Rating) o;
        if (rating.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), rating.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Rating{" +
            "id=" + getId() +
            ", typeKey='" + getTypeKey() + "'" +
            ", value='" + getValue() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            "}";
    }
}
