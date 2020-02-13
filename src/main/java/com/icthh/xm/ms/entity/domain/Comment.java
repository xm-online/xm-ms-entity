package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * A Comment.
 */
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
    @NotNull
    @ApiModelProperty(value = "Comment author user key", required = true)
    @Column(name = "user_key", nullable = false)
    private String userKey;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserKey() {
        return userKey;
    }

    public Comment userKey(String userKey) {
        this.userKey = userKey;
        return this;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public String getMessage() {
        return message;
    }

    public Comment message(String message) {
        this.message = message;
        return this;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getEntryDate() {
        return entryDate;
    }

    public Comment entryDate(Instant entryDate) {
        this.entryDate = entryDate;
        return this;
    }

    public void setEntryDate(Instant entryDate) {
        this.entryDate = entryDate;
    }

    public Comment getComment() {
        return comment;
    }

    public Comment comment(Comment comment) {
        this.comment = comment;
        return this;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public Set<Comment> getReplies() {
        return replies;
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

    public void setReplies(Set<Comment> comments) {
        this.replies = comments;
    }

    public XmEntity getXmEntity() {
        return xmEntity;
    }

    public Comment xmEntity(XmEntity xmEntity) {
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
