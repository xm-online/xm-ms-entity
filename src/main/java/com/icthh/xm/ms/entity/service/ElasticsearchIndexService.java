package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.config.IndexConfiguration;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
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
    private final ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;
    private final TenantContextHolder tenantContextHolder;
    private final MappingConfiguration mappingConfiguration;
    private final IndexConfiguration indexConfiguration;
    private final Executor executor;

    @PersistenceContext
    private final EntityManager entityManager;

    @Setter(AccessLevel.PACKAGE)
    @Resource
    @Lazy
    private ElasticsearchIndexService selfReference;

    public ElasticsearchIndexService(XmEntityRepositoryInternal xmEntityRepositoryInternal,
                                     XmEntitySearchRepository xmEntitySearchRepository,
                                     ElasticsearchTemplateWrapper elasticsearchTemplateWrapper,
                                     TenantContextHolder tenantContextHolder,
                                     MappingConfiguration mappingConfiguration,
                                     IndexConfiguration indexConfiguration,
                                     @Qualifier("taskExecutor") Executor executor,
                                     EntityManager entityManager) {
        this.xmEntityRepositoryInternal = xmEntityRepositoryInternal;
        this.xmEntitySearchRepository = xmEntitySearchRepository;
        this.elasticsearchTemplateWrapper = elasticsearchTemplateWrapper;
        this.tenantContextHolder = tenantContextHolder;
        this.mappingConfiguration = mappingConfiguration;
        this.indexConfiguration = indexConfiguration;
        this.executor = executor;
        this.entityManager = entityManager;
    }

    /**
     * Recreates index and then reindexes ALL entities from database asynchronously.
     * @return @{@link CompletableFuture<Long>} with a number of reindexed entities.
     */
    @Timed
    public CompletableFuture<Long> reindexAllAsync() {
        TenantKey tenantKey = TenantContextUtils.getRequiredTenantKey(tenantContextHolder);
        String rid = MdcUtils.getRid();
        return CompletableFuture.supplyAsync(() -> execForCustomContext(tenantKey,
                                                                        rid,
                                                                        selfReference::reindexAll), executor);
    }

    /**
     * Refreshes entities in elasticsearch index filtered by typeKey asynchronously.
     *
     * Does not recreate index.
     * @param typeKey typeKey to filter source entities.
     * @return @{@link CompletableFuture<Long>} with a number of reindexed entities.
     */
    @Timed
    public CompletableFuture<Long> reindexByTypeKeyAsync(@Nonnull String typeKey) {

        Objects.requireNonNull(typeKey, "typeKey should not be null");

        TenantKey tenantKey = TenantContextUtils.getRequiredTenantKey(tenantContextHolder);
        String rid = MdcUtils.getRid();
        return CompletableFuture.supplyAsync(() -> execForCustomContext(tenantKey,
                                                                        rid,
                                                                        () -> selfReference.reindexByTypeKey(typeKey)), executor);
    }

    /**
     * Refreshes entities in elasticsearch index filtered by collection of IDs asynchronously.
     *
     * Does not recreate index.
     * @param ids - collection of IDs of entities to be reindexed.
     * @return @{@link CompletableFuture<Long>} with a number of reindexed entities.
     */
    @Timed
    public CompletableFuture<Long> reindexByIdsAsync(@Nonnull Iterable<Long> ids) {

        Objects.requireNonNull(ids, "ids should not be null");

        TenantKey tenantKey = TenantContextUtils.getRequiredTenantKey(tenantContextHolder);
        String rid = MdcUtils.getRid();
        return CompletableFuture.supplyAsync(() -> execForCustomContext(tenantKey,
                                                                        rid,
                                                                        () -> selfReference.reindexByIds(ids)), executor);
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
    public long reindexByTypeKey(@Nonnull String typeKey){

        Objects.requireNonNull(typeKey, "typeKey should not be null");

        Specification<XmEntity> spec = Specification
            .where((root, query, cb) -> cb.equal(root.get(XM_ENTITY_FIELD_TYPEKEY), typeKey));

        return reindexXmEntity(spec);

    }

    @Timed
    @Transactional(readOnly = true)
    public long reindexByTypeKey(@Nonnull String typeKey, Integer startFrom) {
        Objects.requireNonNull(typeKey, "typeKey should not be null");
        Specification spec = (Specification) (root, query, criteriaBuilder) -> {
            query.orderBy(criteriaBuilder.desc(root.get("id")));
            return criteriaBuilder.equal(root.get(XM_ENTITY_FIELD_TYPEKEY), typeKey);
        };
        return reindexXmEntity(spec, startFrom);
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
    public long reindexByIds(@Nonnull Iterable<Long> ids) {

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
        return reindexXmEntity(spec, null);
    }

    private long reindexXmEntity(@Nullable Specification<XmEntity> spec,  Integer startFrom) {

        StopWatch stopWatch = StopWatch.createStarted();
        startFrom = defaultIfNull(startFrom, 0);

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

            for (int i = startFrom; i <= xmEntityRepositoryInternal.count(spec) / PAGE_SIZE ; i++) {
                Pageable page = PageRequest.of(i, PAGE_SIZE);
                log.info("Indexing page {} of {}, pageSize {}", i, xmEntityRepositoryInternal.count(spec) / PAGE_SIZE, PAGE_SIZE);
                Page<XmEntity> results = xmEntityRepositoryInternal.findAll(spec, page);
                results.map(entity -> loadEntityRelationships(relationshipGetters, entity));
                xmEntitySearchRepository.saveAll(results.getContent());
                reindexed += results.getContent().size();
                entityManager.clear();
            }
        }
        log.info("Elasticsearch: Indexed [{}] rows for {} in {} ms",
                 reindexed, clazz.getSimpleName(), stopWatch.getTime());
        return reindexed;

    }

    private void recreateIndex() {

        final Class<XmEntity> clazz = XmEntity.class;

        StopWatch stopWatch = StopWatch.createStarted();

        elasticsearchTemplateWrapper.deleteIndex(clazz);
        try {
            if (indexConfiguration.isConfigExists()) {
                elasticsearchTemplateWrapper.createIndex(clazz, indexConfiguration.getConfiguration());
            } else {
                elasticsearchTemplateWrapper.createIndex(clazz);
            }
        } catch (ResourceAlreadyExistsException e) {
            log.info("Do nothing. Index was already concurrently recreated by some other service");
        }

        if (mappingConfiguration.isMappingExists()) {
            elasticsearchTemplateWrapper.putMapping(clazz, mappingConfiguration.getMapping());
        } else {
            elasticsearchTemplateWrapper.putMapping(clazz);
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

    private Long execForCustomContext(TenantKey tenantKey, String rid, Supplier<Long> runnable) {
        try {
            MdcUtils.putRid(rid);
            return tenantContextHolder.getPrivilegedContext().execute(TenantContextUtils.buildTenant(tenantKey), runnable);
        } finally {
            MdcUtils.removeRid();
        }
    }
}
