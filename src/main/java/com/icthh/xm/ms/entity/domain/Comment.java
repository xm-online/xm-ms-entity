package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import io.swagger.annotations.ApiModelProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
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
import java.util.Set;
import java.util.Objects;

/**
 * A Comment.
 */
@Getter
@Setter
@Entity
@Table(name = "xm_comment")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /**
     * Comment author user key.
     */
    @ApiModelProperty(value = "Comment author user key")
    @Column(name = "user_key")
    private String userKey;

    @ApiModelProperty(value = "Comment author client id")
    @Column(name = "client_id")
    private String clientId;

    @ApiModelProperty(value = "Display name")
    @Column(name = "display_name")
    private String displayName;

    /**
     * Message content
     */
    @ApiModelProperty(value = "Message content")
    @Column(name = "message")
    private String message;

    /**
     * Entry date
     */
    @ApiModelProperty(value = "Entry date")
    @Column(name = "entry_date")
    private Instant entryDate;

    @ManyToOne
    private Comment comment;

    @OneToMany(mappedBy = "comment")
    @JsonIgnore
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Comment> replies = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver =
        XmEntityObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true) // otherwise first ref as POJO, others as id
    @JoinColumn(name = "xm_entity_id", nullable = false)
    private XmEntity xmEntity;

    public Comment userKey(String userKey) {
        this.userKey = userKey;
        return this;
    }

    public Comment clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public Comment displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public Comment message(String message) {
        this.message = message;
        return this;
    }

    public Comment entryDate(Instant entryDate) {
        this.entryDate = entryDate;
        return this;
    }

    public Comment comment(Comment comment) {
        this.comment = comment;
        return this;
    }

    public Comment replies(Set<Comment> comments) {
        this.replies = comments;
        return this;
    }

    public Comment addReplies(Comment comment) {
        this.replies.add(comment);
        comment.setComment(this);
        return this;
    }

    public Comment removeReplies(Comment comment) {
        this.replies.remove(comment);
        comment.setComment(null);
        return this;
    }

    public Comment xmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
        return this;
    }

    @PrePersist
    private void prePersist() {
        if (entryDate == null) {
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
        Comment comment = (Comment) o;
        if (comment.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), comment.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Comment{" +
            "id=" + getId() +
            ", userKey='" + getUserKey() + "'" +
            ", message='" + getMessage() + "'" +
            ", entryDate='" + getEntryDate() + "'" +
            "}";
    }
}
