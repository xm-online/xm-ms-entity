package com.icthh.xm.ms.entity.domain;

import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.domain.XmEntity_
import lombok.EqualsAndHashCode
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

import static java.time.Instant.now
import static java.util.UUID.randomUUID
import static org.springframework.data.domain.Sort.Direction.ASC

def TYPE_KEY1 = 'TYPE-KEY1'
def TYPE_KEY2 = 'TYPE-KEY1'
def TYPE_KEY3 = 'TYPE-KEY1'
def NAME1 = 'Name1'
def NAME2 = 'Name2'
def NAME3 = 'Name3'


def entityRepository = lepContext.repositories.xmEntity


def entities = []
entities << createEntity(TYPE_KEY1, NAME1)
entities << createEntity(TYPE_KEY2, NAME2)
entities << createEntity(TYPE_KEY3, NAME3)
entityRepository.saveAll(entities)

List<ProjectionClass> results = entityRepository.findAll(Specification.where({ root, query, cb ->
    return cb.and(
        cb.equal(root.get('typeKey'), TYPE_KEY1),
        cb.equal(root.get('name'), NAME1),
    )
}), {root, cb, pc ->
    return cb.construct(pc, root.get(XmEntity_.typeKey), root.get(XmEntity_.name))
}, Sort.by(new Sort.Order(ASC, 'name')), ProjectionClass.class)

return [foundEntities: results.collect { ['name': it.name, 'typeKey': it.typeKey] }]

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

