package com.icthh.xm.ms.entity.domain;

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification

import javax.persistence.Tuple
import javax.persistence.criteria.Root

import static java.time.Instant.now
import static java.util.UUID.randomUUID

def TYPE_KEY = 'TEST_EXPORT_2'

def NAME1 = 'Name1'
def NAME2 = 'Name2'
def NAME3 = 'Name3'

def entityRepository = lepContext.repositories.xmEntity

def entities = []
entities << createEntity(TYPE_KEY, NAME1)
entities << createEntity(TYPE_KEY, NAME2)
entities << createEntity(TYPE_KEY, NAME3)
entityRepository.saveAll(entities)

List<Tuple> foundResults = entityRepository.findAll(Specification.where({ root, query, cb ->
    return cb.and(
        cb.equal(root.get('typeKey'), TYPE_KEY),
        cb.equal(root.get('name'), NAME2))
}), { Root<XmEntity> root ->
    return [
        root.get(XmEntity_.typeKey).alias('typeKey'),
        root.get(XmEntity_.id).alias('id')
    ]
}, PageRequest.of(0, 10))

def results = []
foundResults.each {tuple ->
    def elements = tuple.getElements()
    Map<String, Object> mappedResult = [:]
    elements.each {tupleElement ->
        String name = tupleElement.getAlias();
        mappedResult.put(name, tuple.get(name));
    }
    results << mappedResult
}

return [results: results]

def createEntity(String typeKey, String name) {
    return new XmEntity()
        .typeKey(typeKey)
        .key(randomUUID())
        .name(name)
        .startDate(now())
        .updateDate(now())
        .data([:]);
}

