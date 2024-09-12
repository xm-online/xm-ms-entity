package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import static jakarta.persistence.CascadeType.ALL;

/**
 * Represents any XM entity file attachment. It could be image, zip archive, pdf
 * document or other file formats (List of available file formats should be
 * configured).
 * Files should be verified on:
 * - size
 * - zip bombs
 * - viruses
 */
@ApiModel(description = "Represents any XM entity file attachment. It could be image, zip archive, pdf document or other file formats (List of available file formats should be configured). Files should be verified on: - size - zip bombs - viruses")
@Entity
@Table(name = "attachment")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@TypeKey
public class Attachment implements Serializable {

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
     * Attachment name
     */
    @NotNull
    @ApiModelProperty(value = "Attachment name", required = true)
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Content retrieving URL
     */
    @ApiModelProperty(value = "Content retrieving URL")
    @Column(name = "content_url")
    private String contentUrl;

    /**
     * Content description
     */
    @ApiModelProperty(value = "Content description")
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

    /**
     * Content type
     */
    @ApiModelProperty(value = "Content type")
    @Column(name = "value_content_type")
    private String valueContentType;

    /**
     * Content size in bytes
     */
    @ApiModelProperty(value = "Content size in bytes")
    @Column(name = "value_content_size")
    private Long valueContentSize;

    /**
     * Content checksum
     */
    @ApiModelProperty(value = "Content checksum")
    @Column(name = "content_checksum")
    private String contentChecksum;

    @OneToOne(cascade = ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(unique = true)
    private Content content;

    @ManyToOne(optional = false)
    @NotNull //- can be null when create new XmEntity with attachments
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

    public String getTypeKey() {
        return typeKey;
    }

    public Attachment typeKey(String typeKey) {
        this.typeKey = typeKey;
        return this;
    }

    public void setTypeKey(String typeKey) {
        this.typeKey = typeKey;
    }

    public String getName() {
        return name;
    }

    public Attachment name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public Attachment contentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
        return this;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getDescription() {
        return description;
    }

    public Attachment description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Attachment startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public Attachment endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getValueContentType() {
        return valueContentType;
    }

    public Attachment valueContentType(String valueContentType) {
        this.valueContentType = valueContentType;
        return this;
    }

    public void setValueContentType(String valueContentType) {
        this.valueContentType = valueContentType;
    }

    public Long getValueContentSize() {
        return valueContentSize;
    }

    public Attachment valueContentSize(Long valueContentSize) {
        this.valueContentSize = valueContentSize;
        return this;
    }

    public void setValueContentSize(Long valueContentSize) {
        this.valueContentSize = valueContentSize;
    }

    public String getContentChecksum() {
        return contentChecksum;
    }

    public void setContentChecksum(String contentChecksum) {
        this.contentChecksum = contentChecksum;
    }

    public Content getContent() {
        return content;
    }

    public Attachment content(Content content) {
        setContent(content);
        return this;
    }

    public void setContent(Content content) {
        this.content = content;
        if (content != null && content.getValue() != null) {
            long length = content.getValue().length;
            this.valueContentSize = length;
        }
    }

    public XmEntity getXmEntity() {
        return xmEntity;
    }

    public Attachment xmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
        return this;
    }

    public void setXmEntity(XmEntity xmEntity) {
        this.xmEntity = xmEntity;
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
        Attachment attachment = (Attachment) o;
        if (attachment.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), attachment.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Attachment{" +
            "id=" + getId() +
            ", typeKey='" + getTypeKey() + "'" +
            ", name='" + getName() + "'" +
            ", contentUrl='" + getContentUrl() + "'" +
            ", description='" + getDescription() + "'" +
            ", startDate='" + getStartDate() + "'" +
            ", endDate='" + getEndDate() + "'" +
            ", valueContentType='" + getValueContentType() + "'" +
            ", valueContentSize='" + getValueContentSize() + "'" +
            "}";
    }
}
