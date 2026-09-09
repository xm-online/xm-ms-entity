package com.icthh.xm.ms.entity.service.search.db;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.keyresolver.DbSearchRequestTypeKeyResolver;
import com.icthh.xm.ms.entity.repository.search.db.PermittedSpecificationRepository;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterCondition;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterParser;
import com.icthh.xm.ms.entity.service.search.db.filter.SortTranslator;
import com.icthh.xm.ms.entity.service.search.db.filter.XmEntityFilterSpecificationBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DB-backed (no Elasticsearch) search of xm entities: typeKey, full text on search_text, filters, sorting. */
@Slf4j
@Service
@LepService(group = "service.entity.dbsearch")
@RequiredArgsConstructor
public class XmEntityDbSearchService {

    public static final String ROW_PRIVILEGE = "XMENTITY.SEARCH.DB";

    private final PermittedSpecificationRepository permittedSpecificationRepository;
    private final XmEntityFilterSpecificationBuilder specificationBuilder;
    private final SortTranslator sortTranslator;
    private final FilterParser filterParser;

    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDb", resolver = DbSearchRequestTypeKeyResolver.class)
    @FindWithPermission(ROW_PRIVILEGE)
    @PrivilegeDescription("Privilege to search xm entities in DB by typeKey, full text query and filters")
    public Page<XmEntity> search(XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey) {
        if (StringUtils.isBlank(request.getTypeKey())) {
            throw new BusinessException(ERR_VALIDATION, "typeKey is required");
        }
        List<FilterCondition> conditions = parseConditions(request);
        Specification<XmEntity> spec = buildSpecification(request.getTypeKey(), request.includeSubTypes(),
            request.getQuery(), conditions, root -> root);
        return permittedSpecificationRepository.findAll(XmEntity.class, spec,
            sortTranslator.toOrderProvider(pageable.getSort()), pageable, privilegeKey);
    }

    /** Shared by entity search, link-dialog search and link target search. */
    <T> Specification<T> buildSpecification(String typeKey, boolean includeSubTypes, String query,
                                            List<FilterCondition> conditions,
                                            Function<Root<T>, Path<XmEntity>> entityPath) {
        Specification<T> spec = specificationBuilder.typeKey(typeKey, includeSubTypes, entityPath)
            .and(specificationBuilder.build(conditions, entityPath));
        if (!XmEntityFilterSpecificationBuilder.hasRemovedCondition(conditions)) {
            spec = spec.and(specificationBuilder.notRemoved(entityPath));
        }
        if (StringUtils.isNotBlank(query)) {
            spec = spec.and(specificationBuilder.fullText(query, entityPath));
        }
        return spec;
    }

    List<FilterCondition> parseConditions(XmEntityDbSearchRequest request) {
        return request.isRawStringValues()
            ? filterParser.parseQueryParams(toListMap(request.getFilter()))
            : filterParser.parseBody(request.getFilter());
    }

    private static Map<String, List<String>> toListMap(Map<String, Object> filter) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (filter != null) {
            filter.forEach((k, v) -> result.put(k, List.of(String.valueOf(v))));
        }
        return result;
    }
}
