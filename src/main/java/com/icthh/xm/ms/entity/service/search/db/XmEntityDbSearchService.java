package com.icthh.xm.ms.entity.service.search.db;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Link_;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.LinkSpec;
import com.icthh.xm.ms.entity.lep.keyresolver.DbSearchRequestTypeKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.EntityTypeKeyAndLinkTypeKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.LinkTypeKeyParamResolver;
import com.icthh.xm.ms.entity.repository.search.db.PermittedSpecificationRepository;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterCondition;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterParser;
import com.icthh.xm.ms.entity.service.search.db.filter.SortTranslator;
import com.icthh.xm.ms.entity.service.search.db.filter.XmEntityFilterSpecificationBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
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
    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityService xmEntityService;
    private final LinkService linkService;

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

    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDbToLink", resolver = EntityTypeKeyAndLinkTypeKeyResolver.class)
    @FindWithPermission(ROW_PRIVILEGE)
    @PrivilegeDescription("Privilege to search link candidates in DB for an xm entity and link type")
    public Page<XmEntity> searchToLink(IdOrKey idOrKey, String entityTypeKey, String linkTypeKey,
                                       XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey) {
        LinkSpec linkSpec = xmEntitySpecService.getLinkSpec(entityTypeKey, linkTypeKey)
            .orElseThrow(() -> new BusinessException(ERR_VALIDATION,
                "Link spec not found for entity type " + entityTypeKey + " and link type " + linkTypeKey));
        List<FilterCondition> conditions = parseConditions(request);
        Specification<XmEntity> spec = buildSpecification(linkSpec.getTypeKey(), request.includeSubTypes(),
            request.getQuery(), conditions, root -> root);

        if (Boolean.TRUE.equals(linkSpec.getIsUnique())) {
            Long sourceId = xmEntityService.getXmEntityIdKeyTypeKey(idOrKey).getId();
            Set<Long> excluded = linkService.findLinkProjectionsBySourceIdAndTypeKey(sourceId, linkTypeKey).stream()
                .map(link -> link.getTarget().getId())
                .collect(Collectors.toCollection(HashSet::new));
            excluded.add(sourceId);
            spec = spec.and((root, query, cb) -> cb.not(root.get(XmEntity_.id).in(excluded)));
        }
        return permittedSpecificationRepository.findAll(XmEntity.class, spec,
            sortTranslator.toOrderProvider(pageable.getSort()), pageable, privilegeKey);
    }

    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDbTargets", resolver = LinkTypeKeyParamResolver.class)
    @FindWithPermission("LINK.SEARCH.DB")
    @PrivilegeDescription("Privilege to search links of a source xm entity in DB, filtered by target fields")
    public Page<Link> searchTargets(IdOrKey idOrKey, String linkTypeKey, XmEntityDbSearchRequest request,
                                    Pageable pageable, String privilegeKey) {
        Long sourceId = xmEntityService.getXmEntityIdKeyTypeKey(idOrKey).getId();
        List<FilterCondition> conditions = parseConditions(request);
        Function<Root<Link>, Path<XmEntity>> target = root -> root.get(Link_.target);

        Specification<Link> spec = (root, query, cb) -> cb.and(
            cb.equal(root.get(Link_.source).get(XmEntity_.id), sourceId),
            cb.equal(root.get(Link_.typeKey), linkTypeKey));
        spec = spec.and(specificationBuilder.build(conditions, target));
        if (StringUtils.isNotBlank(request.getTypeKey())) {
            spec = spec.and(specificationBuilder.typeKey(request.getTypeKey(), request.includeSubTypes(), target));
        }
        if (!XmEntityFilterSpecificationBuilder.hasRemovedCondition(conditions)) {
            spec = spec.and(specificationBuilder.notRemoved(target));
        }
        if (StringUtils.isNotBlank(request.getQuery())) {
            spec = spec.and(specificationBuilder.fullText(request.getQuery(), target));
        }
        return permittedSpecificationRepository.findAll(Link.class, spec,
            sortTranslator.toOrderProvider(pageable.getSort(), target), pageable, privilegeKey);
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
