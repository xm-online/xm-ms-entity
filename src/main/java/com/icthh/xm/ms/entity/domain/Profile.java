package com.icthh.xm.ms.entity.domain;

import com.icthh.xm.ms.entity.domain.listener.ProfileElasticSearchListener;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents User's profile on Entity microservice.
 * Contains relation to XmEntity by foreign key. Profile will be creatid in LEP code as a reaction to event for new user registration.
 */
@ApiModel(description = "Represents User's profile on Entity microservice. Contains relation to XmEntity by foreign key. Profile will be creatid in LEP code as a reaction to event for new user registration.")
@Entity
@Table(name = "profile")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@EntityListeners(ProfileElasticSearchListener.class)
public class Profile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /**
     * User identifier from UAA.
     */
    @NotNull
    @ApiModelProperty(value = "User identifier from UAA.", required = true)
    @Column(name = "user_key", nullable = false)
    private String userKey;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @NotNull
    @JoinColumn(name = "xmentity_id", unique = true)
    private XmEntity xmentity;

    // jhipster-needle-entity-add-field - Jhipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserKey() {
        return userKey;
    }

    public Profile userKey(String userKey) {
        this.userKey = userKey;
        return this;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public XmEntity getXmentity() {
        return xmentity;
    }

    public Profile xmentity(XmEntity xmEntity) {
        this.xmentity = xmEntity;
        return this;
    }

    public void setXmentity(XmEntity xmEntity) {
        this.xmentity = xmEntity;
    }
    // jhipster-needle-entity-add-getters-setters - Jhipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Profile profile = (Profile) o;
        if (profile.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), profile.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Profile{" +
            "id=" + getId() +
            ", userKey='" + getUserKey() + "'" +
            "}";
    }
}
