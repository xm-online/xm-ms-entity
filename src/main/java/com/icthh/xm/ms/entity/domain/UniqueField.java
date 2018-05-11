package com.icthh.xm.ms.entity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
@ToString
@AllArgsConstructor
@Builder
public class UniqueField implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    private String fieldJsonPath;

    private String fieldValue;

    private String entityTypeKey;

    @NotNull
    @ManyToOne(optional = false)
    private XmEntity xmEntity;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UniqueField that = (UniqueField) o;

        if (fieldJsonPath != null ? !fieldJsonPath.equals(that.fieldJsonPath) : that.fieldJsonPath != null)
            return false;
        if (fieldValue != null ? !fieldValue.equals(that.fieldValue) : that.fieldValue != null) return false;
        if (entityTypeKey != null ? !entityTypeKey.equals(that.entityTypeKey) : that.entityTypeKey != null)
            return false;
        return xmEntity != null ? xmEntity.equals(that.xmEntity) : that.xmEntity == null;
    }

    @Override
    public int hashCode() {
        int result = fieldJsonPath != null ? fieldJsonPath.hashCode() : 0;
        result = 31 * result + (fieldValue != null ? fieldValue.hashCode() : 0);
        result = 31 * result + (entityTypeKey != null ? entityTypeKey.hashCode() : 0);
        return result;
    }
}
