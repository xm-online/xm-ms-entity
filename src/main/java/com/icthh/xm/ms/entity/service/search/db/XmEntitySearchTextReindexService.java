package com.icthh.xm.ms.entity.service.search.db;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyResolver;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Rebuilds {@code xm_entity.search_text} for rows that were created before DB full text search
 * was enabled (or before the type spec started to declare {@code fullTextSearch}).
 */
@Slf4j
@Service
@LepService(group = "service.entity.dbsearch")
@RequiredArgsConstructor
public class XmEntitySearchTextReindexService {

    public static final int BATCH_SIZE = 500;

    private final XmEntityRepository xmEntityRepository;
    private final XmEntitySpecService xmEntitySpecService;
    private final SearchTextBuilder searchTextBuilder;
    private final TransactionTemplate transactionTemplate;

    /**
     * Recomputes search_text for entities of the given type (dotted subtypes included).
     * {@code typeKey == null} processes every type of the current tenant with {@code fullTextSearch: true}.
     * Each batch of {@link #BATCH_SIZE} entities is its own transaction.
     *
     * @return number of processed entities
     */
    @LogicExtensionPoint(value = "ReindexSearchText", resolver = TypeKeyResolver.class)
    public long reindex(String typeKey) {
        List<String> typeKeys = typeKey != null ? List.of(typeKey) : enabledTypeKeys();
        long processed = 0;
        for (String key : typeKeys) {
            processed += reindexType(key);
        }
        return processed;
    }

    private long reindexType(String typeKey) {
        Specification<XmEntity> spec = (root, query, cb) -> cb.or(
            cb.equal(root.get(XmEntity_.typeKey), typeKey),
            cb.like(root.get(XmEntity_.typeKey), typeKey + ".%"));
        long processed = 0;
        int pageNumber = 0;
        while (true) {
            int current = pageNumber;
            Integer handled = transactionTemplate.execute(status -> {
                Page<XmEntity> page = xmEntityRepository.findAll(spec,
                    PageRequest.of(current, BATCH_SIZE, Sort.by(XmEntity_.ID)));
                page.getContent().forEach(entity -> {
                    TypeSpec typeSpec = xmEntitySpecService
                        .getTypeSpecByKeyWithoutFunctionFilter(entity.getTypeKey()).orElse(null);
                    entity.setSearchText(searchTextBuilder.build(typeSpec, entity));
                });
                xmEntityRepository.saveAll(page.getContent());
                return page.getNumberOfElements();
            });
            processed += handled == null ? 0 : handled;
            if (handled == null || handled < BATCH_SIZE) {
                break;
            }
            pageNumber++;
        }
        log.info("Reindexed search_text for {} entities of type {}", processed, typeKey);
        return processed;
    }

    private List<String> enabledTypeKeys() {
        return xmEntitySpecService.findAllTypes().stream()
            .filter(t -> Boolean.TRUE.equals(t.getFullTextSearch()))
            .map(TypeSpec::getKey)
            .toList();
    }
}
