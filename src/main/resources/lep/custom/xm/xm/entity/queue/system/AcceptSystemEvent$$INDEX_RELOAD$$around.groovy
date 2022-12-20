package lep.custom.xm.xm.entity.queue.system

import com.fasterxml.jackson.databind.ObjectMapper
import com.icthh.xm.commons.domainevent.domain.DomainEvent
import com.icthh.xm.commons.domainevent.domain.DomainEventPayload
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation
import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventSource
import com.icthh.xm.commons.tenant.TenantContext
import com.icthh.xm.commons.tenant.TenantContextUtils
import com.icthh.xm.commons.topic.service.KafkaTemplateService
import com.icthh.xm.ms.entity.domain.XmEntity
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal
import groovy.transform.Field
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification

final String DB_TOPIC_FORMAT = "event.%s.db";
final int PAGE_SIZE = 1000;

@Field ObjectMapper objectMapper = new ObjectMapper()

Logger log = LoggerFactory.getLogger(getClass())
Map<String, Object> eventData = lepContext.inArgs.event?.data as Map<String, Object>
TenantContext tenantContext = lepContext.tenantContext
KafkaTemplateService kafkaTemplateService = lepContext.templates.kafka
XmEntityRepositoryInternal xmEntityRepositoryInternal = lepContext.lepServices.getInstance(XmEntityRepositoryInternal.class)

String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContext)
Specification<XmEntity> spec = buildSpecification(eventData)

long count = xmEntityRepositoryInternal.count(spec)
if (count <= 0) {
    log.info("Finish reload data, cause count <= 0!")
    return
}

String dbTopic = String.format(DB_TOPIC_FORMAT, tenantKey)
for (int i = 0; i <= count / PAGE_SIZE; i++) {
    Pageable page = PageRequest.of(i, PAGE_SIZE);
    Page<XmEntity> all = xmEntityRepositoryInternal.findAll(spec, page);
    all.getContent()
        .collect { it -> toEvent(it, tenantKey) }
        .each { domainEvent -> kafkaTemplateService.send(dbTopic, toJson(domainEvent)) }
}

return

private static Specification<XmEntity> buildSpecification(Map<String, Object> params) {
    List<Specification<XmEntity>> specifications = params.entrySet()
        .collect { entry -> buildForEquals(entry.getKey(), entry.getValue()) }
    return composeSpecifications(specifications);
}

private static Specification<XmEntity> buildForEquals(String key, Object value) {
    return Specification.where({
        root, query, cb -> cb.and(cb.equal(root.get(key), value))
    });
}

private static Specification<XmEntity> composeSpecifications(List<Specification<XmEntity>> specifications) {
    Specification<XmEntity> specification = Specification.where(
        { root, query, cb -> return cb.conjunction() }
    )
    specifications.each {
        specification = specification.and(it)
    }
    return specification;
}

private static DomainEvent toEvent(XmEntity entity, String tenantKey) {
    return DomainEvent.builder()
        .id(UUID.randomUUID()) // TODO ?
        .txId(String.valueOf(entity.id)) // TODO ?
        .aggregateId(XmEntity.class.getSimpleName())
        .aggregateType(entity.typeKey)
        .operation(DefaultDomainEventOperation.READ.name())
        .msName("entity") // TODO ?
        .source(DefaultDomainEventSource.DB.name())
        .userKey(null)
        .clientId(null)
        .tenant(tenantKey)
        .validFor(null)
        .meta(null)
        .payload(new DomainEventPayload(entity.data))
        .build()
}

private String toJson(DomainEvent domainEvent) {
    return objectMapper.writeValueAsString(domainEvent);
}
