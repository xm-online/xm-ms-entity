package com.icthh.xm.ms.entity.domain;

import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "unique_field")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of={"fieldJsonPath", "fieldValue", "entityTypeKey"})
public class UniqueField implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "field_json_path", nullable = false)
    private String fieldJsonPath;

    @Column(name = "field_value", nullable = false)
    private String fieldValue;

    @Column(name = "entity_type_key", nullable = false)
    private String entityTypeKey;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "xm_entity_id", nullable = false)
    private XmEntity xmEntity;
}
