package com.icthh.xm.ms.entity.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.persistence.OneToMany;
import javax.persistence.criteria.CriteriaBuilder;

@Slf4j
@Service
public class ElasticsearchIndexService {

    private static final Lock reindexLock = new ReentrantLock();
    private static final int PAGE_SIZE = 100;
    private static final String XM_ENTITY_FIELD_TYPEKEY = "typeKey";
    private static final String XM_ENTITY_FIELD_ID = "id";

    private final XmEntityRepositoryInternal xmEntityRepositoryInternal;
    private final XmEntitySearchRepository xmEntitySearchRepository;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final MappingConfiguration mappingConfiguration;
    private final Executor executor;

    @Setter(AccessLevel.PACKAGE)
    @Resource
    @Lazy
    private ElasticsearchIndexService selfReference;

    public ElasticsearchIndexService(XmEntityRepositoryInternal xmEntityRepositoryInternal,
                                     XmEntitySearchRepository xmEntitySearchRepository,
                                     ElasticsearchTemplate elasticsearchTemplate,
                                     TenantContextHolder tenantContextHolder,
                                     MappingConfiguration mappingConfiguration,
                                     @Qualifier("taskExecutor") Executor executor) {
        this.xmEntityRepositoryInternal = xmEntityRepositoryInternal;
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

    /**
     * Recreates index and then reindexes ALL entities from database.
     * @return number of reindexed entities.
     */
    @Timed
    @Transactional(readOnly = true)
    public long reindexAll() {
        long reindexed = 0L;
        if (reindexLock.tryLock()) {
            try {
                recreateIndex();
                reindexed = reindexXmEntity();
                log.info("Elasticsearch: Successfully performed full reindexing");
            } finally {
                reindexLock.unlock();
            }
        } else {
            log.info("Elasticsearch: concurrent reindexing attempt");
        }
        return reindexed;
    }

    /**
     * Refreshes entities in elasticsearch index filtered by typeKey.
     *
     * Does not recreate index.
     * @param typeKey typeKey to filter source entities.
     * @return number of reindexed entities.
     */
    @Timed
    @Transactional(readOnly = true)
    public long reindexAll(@Nonnull String typeKey){

        Objects.requireNonNull(typeKey, "typeKey should not be null");

        Specification<XmEntity> spec = Specification
            .where((root, query, cb) -> cb.equal(root.get(XM_ENTITY_FIELD_TYPEKEY), typeKey));

        return reindexXmEntity(spec);

    }

    /**
     * Refreshes entities in elasticsearch index filtered by collection of IDs.
     *
     * Does not recreate index.
     * @param ids - collection of IDs of entities to be reindexed.
     * @return number of reindexed entities.
     */
    @Timed
    @Transactional(readOnly = true)
    public long reindexAll(@Nonnull Iterable<Long> ids) {

        Objects.requireNonNull(ids, "ids should not be null");

        Specification<XmEntity> spec = Specification
            .where((root, query, cb) -> {
                CriteriaBuilder.In<Long> in = cb.in(root.get(XM_ENTITY_FIELD_ID));
                ids.forEach(in::value);
                return in;
            });

        return reindexXmEntity(spec);
    }

    private Long reindexXmEntity() {

        return reindexXmEntity(null);
    }

    private long reindexXmEntity(@Nullable Specification<XmEntity> spec) {

        StopWatch stopWatch = StopWatch.createStarted();

        final Class<XmEntity> clazz = XmEntity.class;

        long reindexed = 0L;

        if (xmEntityRepositoryInternal.count(spec) > 0) {
            List<Method> relationshipGetters = Arrays.stream(clazz.getDeclaredFields())
                                                     .filter(field -> field.getType().equals(Set.class))
                                                     .filter(field -> field.getAnnotation(OneToMany.class) != null)
                                                     .filter(field -> field.getAnnotation(JsonIgnore.class) == null)
                                                     .map(field -> extractFieldGetter(clazz, field))
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList());

            for (int i = 0; i <= xmEntityRepositoryInternal.count(spec) / PAGE_SIZE ; i++) {
                Pageable page = PageRequest.of(i, PAGE_SIZE);
                log.info("Indexing page {} of {}, pageSize {}", i, xmEntityRepositoryInternal.count(spec) / PAGE_SIZE, PAGE_SIZE);
                Page<XmEntity> results = xmEntityRepositoryInternal.findAll(spec, page);
                results.map(entity -> loadEntityRelationships(relationshipGetters, entity));
                xmEntitySearchRepository.saveAll(results.getContent());
                reindexed += results.getContent().size();
            }
        }
        log.info("Elasticsearch: Indexed [{}] rows for {} in {} ms",
                 reindexed, clazz.getSimpleName(), stopWatch.getTime());
        return reindexed;

    }

    private void recreateIndex() {

        final Class<XmEntity> clazz = XmEntity.class;

        StopWatch stopWatch = StopWatch.createStarted();

        elasticsearchTemplate.deleteIndex(clazz);
        try {
            elasticsearchTemplate.createIndex(clazz);
        } catch (ResourceAlreadyExistsException e) {
            log.info("Do nothing. Index was already concurrently recreated by some other service");
        }

        if (mappingConfiguration.isMappingExists()) {
            elasticsearchTemplate.putMapping(clazz, mappingConfiguration.getMapping());
        } else {
            elasticsearchTemplate.putMapping(clazz);
        }
        log.info("elasticsearch index was recreated for {} in {} ms",
                 XmEntity.class.getSimpleName(), stopWatch.getTime());
    }

    private Method extractFieldGetter(final Class<XmEntity> clazz, final Field field) {
        try {
            return new PropertyDescriptor(field.getName(), clazz).getReadMethod();
        } catch (IntrospectionException e) {
            log.error("Error retrieving getter for class {}, field {}. Field will NOT be indexed",
                      clazz.getSimpleName(), field.getName(), e);
            return null;
        }
    }

    private XmEntity loadEntityRelationships(final List<Method> relationshipGetters, final XmEntity entity) {
        // if there are any relationships to load, do it now
        relationshipGetters.forEach(method -> {
            try {
                // eagerly load the relationship set
                ((Set) method.invoke(entity)).size();
            } catch (Exception ex) {
                log.error("Error loading relationships for entity: {}, error: {}", entity, ex.getMessage());
            }
        });
        return entity;
    }

    private void execForCustomContext(TenantKey tenantKey, String rid, Runnable runnable) {
        try {
            MdcUtils.putRid(rid);
            tenantContextHolder.getPrivilegedContext().execute(TenantContextUtils.buildTenant(tenantKey), runnable);
        } finally {
            MdcUtils.removeRid();
        }
    }
}
