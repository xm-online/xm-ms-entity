package com.icthh.xm.ms.entity.domain;

import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.domain.XmEntity_
import lombok.EqualsAndHashCode
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

import javax.persistence.criteria.Root

import static java.time.Instant.now
import static java.util.UUID.randomUUID
import static org.springframework.data.domain.Sort.Direction.ASC

def NAME1 = 'Name1'
def NAME2 = 'Name2'
def NAME3 = 'Name3'


def entityRepository = lepContext.repositories.xmEntity


def entities = []
entities << createEntity('TEST_EXPORT_2', NAME1)
entities << createEntity('TEST_EXPORT_2', NAME2)
entities << createEntity('TEST_EXPORT_2', NAME3)
entityRepository.saveAll(entities)

List<?> results = entityRepository.findAll(Specification.where({ root, query, cb ->
    return cb.and(
        cb.equal(root.get('name'), NAME1)
    )
}), { Root<XmEntity> root ->
    return [root.get(XmEntity_.typeKey), root.get(XmEntity_.id)]
}, PageRequest.of(0, 10))

return [foundEntities: results.collect { ['id': it.id, 'typeKey': it.typeKey] }]

@EqualsAndHashCode
class ProjectionClass {
    String typeKey;
    String name;

    ProjectionClass(String typeKey, String name) {
        this.typeKey = typeKey
        this.name - name
    }
}

def createEntity(String typeKey, String name) {
    return new XmEntity()
        .typeKey(typeKey)
        .key(randomUUID())
        .name(name)
        .startDate(now())
        .updateDate(now())
        .data([:]);
}

