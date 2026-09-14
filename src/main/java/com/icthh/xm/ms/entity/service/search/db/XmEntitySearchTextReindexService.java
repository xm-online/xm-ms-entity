package com.icthh.xm.ms.entity.service.search.db;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;
import static com.icthh.xm.ms.entity.domain.XmEntity_.ID;

import com.icthh.xm.commons.exceptions.BusinessException;
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

/** Rebuilds xm_entity.search_text for existing rows, in batches, one transaction per batch. */
@Slf4j
@Service
@LepService(group = "service.entity.dbsearch")
@RequiredArgsConstructor
public class XmEntitySearchTextReindexService {

    public static final int BATCH_SIZE = 500;

    private final XmEntityRepository xmEntityRepository;
    private final XmEntitySpecService xmEntitySpecService;
    private final SearchTextUpdater searchTextUpdater;
    private final TransactionTemplate transactionTemplate;

    /** @param typeKey type (with subtypes) to process; {@code null} → every type with fullTextSearch: true */
    @LogicExtensionPoint(value = "ReindexSearchText", resolver = TypeKeyResolver.class)
    public long reindex(String typeKey) {
        if (typeKey != null) {
            if (xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(typeKey).isEmpty()) {
                throw new BusinessException(ERR_VALIDATION, "Unknown entity typeKey: " + typeKey);
            }
            // explicit type: the type itself and its subtypes
            return reindexType(typeKey, (root, query, cb) -> cb.or(
                cb.equal(root.get(XmEntity_.typeKey), typeKey),
                cb.like(root.get(XmEntity_.typeKey), typeKey + ".%")));
        }
        // no type given: exactly the types with fullTextSearch, so a subtype that disabled it is left alone
        List<String> enabled = enabledTypeKeys();
        if (enabled.isEmpty()) {
            return 0;
        }
        return reindexType(String.join(", ", enabled),
            (root, query, cb) -> root.get(XmEntity_.typeKey).in(enabled));
    }

    private long reindexType(String type, Specification<XmEntity> ofType) {
        long processed = 0;
        for (int pageNumber = 0; ; pageNumber++) {
            PageRequest page = PageRequest.of(pageNumber, BATCH_SIZE, Sort.by(ID));
            int handled = transactionTemplate.execute(status -> reindexBatch(ofType, page));
            processed += handled;
            log.info("Reindex search_text: type {}, batch {}, processed {} entities so far", type, pageNumber, processed);
            if (handled < BATCH_SIZE) {
                break;
            }
        }
        return processed;
    }

    private int reindexBatch(Specification<XmEntity> ofType, PageRequest page) {
        Page<XmEntity> entities = xmEntityRepository.findAll(ofType, page);
        entities.getContent().forEach(searchTextUpdater::refresh);
        xmEntityRepository.saveAll(entities.getContent());
        return entities.getNumberOfElements();
    }

    /** Every type whose effective spec has fullTextSearch: true (inheritance is already applied by the spec service). */
    private List<String> enabledTypeKeys() {
        return xmEntitySpecService.findAllTypes().stream()
            .filter(spec -> Boolean.TRUE.equals(spec.getFullTextSearch()))
            .map(TypeSpec::getKey)
            .toList();
    }
}
