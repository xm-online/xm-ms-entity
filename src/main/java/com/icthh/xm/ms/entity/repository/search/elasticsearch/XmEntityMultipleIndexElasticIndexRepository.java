package com.icthh.xm.ms.entity.repository.search.elasticsearch;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.repository.search.elasticsearch.index.ElasticIndexNameResolver;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@ConditionalOnProperty(value = "application.use-elasticsearch-multiple-indices")
@Component
@RequiredArgsConstructor
@Slf4j
public class XmEntityMultipleIndexElasticIndexRepository implements XmEntityElasticRepository {

    public static final String INDEX_TYPE_NAME = "xmentity";
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final MappingConfiguration mappingConfiguration;
    private final IndexConfiguration indexConfiguration;
    private final XmEntitySpecService xmEntitySpecService;
    private final ElasticIndexNameResolver elasticIndexNameResolver;

    private static final String XM_ENTITY_FIELD_TYPEKEY = "typeKey";

    @Override
    public XmEntity save(XmEntity xmEntity) {
        IndexQuery fooIdxQuery = new IndexQueryBuilder()
            .withIndexName(elasticIndexNameResolver.resolve(xmEntity.getTypeKey()))
            .withObject(xmEntity)
            .build();
        elasticsearchTemplate.index(fooIdxQuery);
        return xmEntity;
    }

    @Override
    public void saveAll(List<XmEntity> xmEntity) {
        List<IndexQuery> queries = xmEntity.stream()
            .map(entity -> new IndexQueryBuilder()
                .withIndexName(elasticIndexNameResolver.resolve(entity.getTypeKey()))
                .withObject(entity)
                .build())
            .collect(Collectors.toList());
        elasticsearchTemplate.bulkIndex(queries);
    }

    @Override
    public void delete(XmEntity xmEntity) {
        String typeKey = xmEntity.getTypeKey();
        DeleteQuery deleteQuery = new DeleteQuery();
        deleteQuery.setIndex(typeKey);
        deleteQuery.setQuery(termQuery("id", xmEntity.getId()));
        elasticsearchTemplate.delete(deleteQuery, XmEntity.class);
    }

    @Override
    @Transactional(readOnly = true)
    public long handleReindex(Function<Specification, Long> specificationFunction) {
        AtomicLong reIndexCount = new AtomicLong(0L);
        List<TypeSpec> allTypes = xmEntitySpecService.findAllTypes();
        List<String> typeKeyList = allTypes
            .stream()
            .map(spec -> spec.getKey())
            .sorted()
            .collect(Collectors.toList());
        IntStream.iterate(0, i -> i < typeKeyList.size(), i -> i + 1)
            .peek(i -> log.info("{} from {} resolvedTypeKey [{}] is being indexed", i,
                typeKeyList.size(), typeKeyList.get(i)))
            .mapToObj(i-> typeKeyList.get(i))
            .forEach(typeKey -> {
                Specification spec = (Specification) (root, query, criteriaBuilder) -> {
                    query.orderBy(criteriaBuilder.desc(root.get("id")));
                    return criteriaBuilder.like(root.get(XM_ENTITY_FIELD_TYPEKEY), typeKey);
                };
                if (elasticIndexNameResolver.isOnlyRoot(typeKey)) {
                    recreateIndex(typeKey);
                }
                Long count = specificationFunction.apply(spec);
                reIndexCount.addAndGet(count);
            });
        return reIndexCount.get();
    }

    private void recreateIndex(String typeKey) {

        String resolvedTypeKey = elasticIndexNameResolver.resolve(typeKey);
        StopWatch stopWatch = createStarted();
        elasticsearchTemplate.deleteIndex(resolvedTypeKey);
        try {
            String indexConfig = defaultIfEmpty(
                indexConfiguration.getMapping(resolvedTypeKey),
                indexConfiguration.getMapping()
            );
            if (isNotEmpty(indexConfig)) {
                elasticsearchTemplate.createIndex(resolvedTypeKey, indexConfig);
            } else {
                elasticsearchTemplate.createIndex(resolvedTypeKey);
            }
        } catch (ResourceAlreadyExistsException e) {
            log.info("Do nothing. Index was already concurrently recreated by some other service");
        }
        String mapping = mappingConfiguration.getMapping(resolvedTypeKey);
        if (isNotEmpty(mapping)) {
            elasticsearchTemplate.putMapping(resolvedTypeKey, INDEX_TYPE_NAME, mapping);
        } else if (mappingConfiguration.isMappingExists()) {
            elasticsearchTemplate.putMapping(resolvedTypeKey, INDEX_TYPE_NAME, mappingConfiguration.getMapping());
        }

        log.info("elasticsearch index was recreated for {}, typeKey:  in {} ms",
            XmEntity.class.getSimpleName(), resolvedTypeKey, stopWatch.getTime());
    }

}
