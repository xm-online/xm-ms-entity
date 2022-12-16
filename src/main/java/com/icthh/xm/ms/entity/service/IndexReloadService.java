package com.icthh.xm.ms.entity.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.kafka.SystemEvent;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.service.mapper.XmEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexReloadService {

    private static final String DB_TOPIC_FORMAT = "event.%s.db";
    private static final int PAGE_SIZE = 1000;
    private static final String XM_ENTITY_TYPE_KEY = "typeKey";

    private final ConsulClient consulClient;
    private final KafkaTemplateService kafkaTemplateService;
    private final XmEntityRepositoryInternal xmEntityRepositoryInternal;
    private final ObjectMapper objectMapper;
    private final XmEntityMapper xmEntityMapper;

    @Transactional(readOnly = true)
    public void reloadData(SystemEvent event) {

        Map<String, Object> params = fromJson(event.getData());

        String tenantKey = event.getTenantKey();
        String lockKey = tenantKey + params.getOrDefault(XM_ENTITY_TYPE_KEY, "");

        if (!lock(lockKey)) {
            log.warn("Ignore event, is lockKey = {} is locked!", lockKey);
            return;
        }

        StopWatch stopWatch = StopWatch.createStarted();
        try {
            log.info("Start reload data to tenant = {}", tenantKey);

            Specification<XmEntity> spec = buildSpecification(params);
            long count = xmEntityRepositoryInternal.count(spec);
            if (count <= 0) {
                log.info("Finish reload data, cause count <= 0!");
                return;
            }

            String dbTopic = String.format(DB_TOPIC_FORMAT, tenantKey);
            for (int i = 0; i <= count / PAGE_SIZE; i++) {
                Pageable page = PageRequest.of(i, PAGE_SIZE);
                Page<XmEntity> all = xmEntityRepositoryInternal.findAll(spec, page);
                all.getContent().stream()
                    .map(xmEntityMapper::toEvent)
                    .forEach(domainEvent -> kafkaTemplateService.send(dbTopic, toJson(domainEvent)));
            }

            log.info("Finish reload data to consumer = {}, count = {}, wTime = {}ms", dbTopic, count, stopWatch.getTime());
        } finally {
            unlock(tenantKey);
        }
    }

    private Specification<XmEntity> buildSpecification(Map<String, Object> params) {
        List<Specification<XmEntity>> specifications = params.entrySet().stream()
            .map(entry -> buildForEquals(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        return composeSpecifications(specifications);
    }

    private Specification<XmEntity> buildForEquals(String key, Object value) {
        return Specification.where((root, query, cb) -> cb.and(cb.equal(root.get(key), value)));
    }

    private Specification<XmEntity> composeSpecifications(List<Specification<XmEntity>> specifications) {
        Specification<XmEntity> result = Specification.where((root, query, cb) -> cb.conjunction());
        for (Specification<XmEntity> specification : specifications) {
            result = result.and(specification);
        }
        return result;
    }

    private boolean lock(String lockKey) {
        PutParams putParams = new PutParams();
        putParams.setCas(0L);
        Response<Boolean> booleanResponse = consulClient.setKVValue(lockKey, lockKey, putParams);
        return booleanResponse.getValue();
    }

    private void unlock(String lockKey) {
        consulClient.deleteKVValue(lockKey);
    }

    @SneakyThrows
    private String toJson(Object domainEvent) {
        return objectMapper.writeValueAsString(domainEvent);
    }

    private Map<String, Object> fromJson(Object object) {
        return objectMapper.convertValue(object, new TypeReference<Map<String, Object>>() {
        });
    }
}
