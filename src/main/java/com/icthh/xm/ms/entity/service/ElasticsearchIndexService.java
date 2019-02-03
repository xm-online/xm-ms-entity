package com.icthh.xm.ms.entity.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.persistence.OneToMany;

@Slf4j
@Service
public class ElasticsearchIndexService {

    private static final Lock reindexLock = new ReentrantLock();

    private final XmEntityRepository xmEntityRepository;
    private final XmEntitySearchRepository xmEntitySearchRepository;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final MappingConfiguration mappingConfiguration;
    private final Executor executor;

    @Setter(AccessLevel.PACKAGE)
    @Resource
    @Lazy
    private ElasticsearchIndexService selfReference;

    public ElasticsearchIndexService(XmEntityRepository xmEntityRepository,
                                     XmEntitySearchRepository xmEntitySearchRepository,
                                     ElasticsearchTemplate elasticsearchTemplate,
                                     TenantContextHolder tenantContextHolder,
                                     MappingConfiguration mappingConfiguration,
                                     @Qualifier("taskExecutor") Executor executor) {
        this.xmEntityRepository = xmEntityRepository;
        this.xmEntitySearchRepository = xmEntitySearchRepository;
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.tenantContextHolder = tenantContextHolder;
        this.mappingConfiguration = mappingConfiguration;
        this.executor = executor;
    }

    @Timed
    public void reindexAllAsync() {
        TenantKey tenantKey = TenantContextUtils.getRequiredTenantKey(tenantContextHolder);
        String rid = MdcUtils.getRid();
        executor.execute(() -> execForCustomContext(tenantKey, rid, () -> selfReference.reindexAll()));
    }

    @Timed
    @Transactional(readOnly = true)
    public void reindexAll() {
        if (reindexLock.tryLock()) {
            try {
                reindexForClass(xmEntityRepository, xmEntitySearchRepository);

                log.info("Elasticsearch: Successfully performed reindexing");
            } finally {
                reindexLock.unlock();
            }
        } else {
            log.info("Elasticsearch: concurrent reindexing attempt");
        }
    }

    private <T, ID extends Serializable> void reindexForClass(JpaRepository<T, ID> jpaRepository,
                                                              ElasticsearchRepository<T, ID> elasticsearchRepository) {
        elasticsearchTemplate.deleteIndex(XmEntity.class);
        try {
            elasticsearchTemplate.createIndex(XmEntity.class);
        } catch (IndexAlreadyExistsException e) {
            // Do nothing. Index was already concurrently recreated by some other service.
        }
        elasticsearchTemplate.putMapping(XmEntity.class);
        if (mappingConfiguration.isMappingExists()) {
            elasticsearchTemplate.putMapping(XmEntity.class, mappingConfiguration.getMapping());
        }
        if (jpaRepository.count() > 0) {
            List<Method> relationshipGetters = Arrays.stream(XmEntity.class.getDeclaredFields())
                .filter(field -> field.getType().equals(Set.class))
                .filter(field -> field.getAnnotation(OneToMany.class) != null)
                .filter(field -> field.getAnnotation(JsonIgnore.class) == null)
                .map(field -> {
                    try {
                        return new PropertyDescriptor(field.getName(), XmEntity.class).getReadMethod();
                    } catch (IntrospectionException e) {
                        log.error("Error retrieving getter for class {}, field {}. Field will NOT be indexed",
                                  XmEntity.class.getSimpleName(), field.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            int size = 100;
            for (int i = 0; i <= jpaRepository.count() / size; i++) {
                Pageable page = new PageRequest(i, size);
                log.info("Indexing page {} of {}, size {}", i, jpaRepository.count() / size, size);
                Page<T> results = jpaRepository.findAll(page);
                results.map(result -> {
                    // if there are any relationships to load, do it now
                    relationshipGetters.forEach(method -> {
                        try {
                            // eagerly load the relationship set
                            ((Set) method.invoke(result)).size();
                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                        }
                    });
                    return result;
                });
                elasticsearchRepository.save(results.getContent());
            }
        }
        log.info("Elasticsearch: Indexed all rows for {}", XmEntity.class.getSimpleName());
    }

    private void execForCustomContext(TenantKey tenantKey, String rid, Runnable runnable) {
        final String oldRid = MdcUtils.getRid();
        try {
            TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
            MdcUtils.putRid(rid);
            runnable.run();
        } finally {
            if (oldRid != null) {
                MdcUtils.putRid(oldRid);
            } else {
                MdcUtils.removeRid();
            }
        }
    }
}
