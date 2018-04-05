package com.icthh.xm.ms.entity.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents Attachment content. Content can be extracted separately from the attachment.
 */
@ApiModel(description = "Represents Attachment content. Content can be extracted separately from the attachment.")
@Entity
@Table(name = "content")
@Document(indexName = "#{@indexName.prefix}content") // TODO - do we really need to add this entity to ES?
public class Content implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    /**
     * Content value as byte array
     */
    @NotNull
    @ApiModelProperty(value = "Content value as byte array", required = true)
    // @Lob // Do not use this annotation as it has different behaviour in Postgres and H2.
    @Column(name = "jhi_value", nullable = false, columnDefinition="BLOB")
    private byte[] value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getValue() {
        return value;
    }

    public Content value(byte[] value) {
        this.value = value;
        return this;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Content content = (Content) o;
        if (content.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), content.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Content{" +
            "id=" + getId() +
            ", value='" + getValue() + "'" +
            "}";
    }
}
