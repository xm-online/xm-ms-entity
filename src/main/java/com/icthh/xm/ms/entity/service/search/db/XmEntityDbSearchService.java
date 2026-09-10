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
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.lep.keyresolver.DbSearchRequestTypeKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.EntityTypeKeyAndLinkTypeKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.JpqlTemplateKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.LinkTypeKeyParamResolver;
import com.icthh.xm.ms.entity.repository.search.db.PermittedSpecificationRepository;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterCondition;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterParser;
import com.icthh.xm.ms.entity.service.search.db.filter.SortTranslator;
import com.icthh.xm.ms.entity.service.search.db.filter.XmEntityFilterSpecificationBuilder;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplate;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateExecutor;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateParamsService;
import com.icthh.xm.ms.entity.service.search.db.template.XmEntityJpqlTemplatesService;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    private final JpqlTemplateExecutor templateExecutor;
    private final JpqlTemplateParamsService templateParamsService;
    private final XmEntityJpqlTemplatesService jpqlTemplatesService;

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
        XmEntityIdKeyTypeKey source = xmEntityService.getXmEntityIdKeyTypeKey(idOrKey);
        if (!isSameOrSubType(source.getTypeKey(), entityTypeKey)) {
            throw new BusinessException(ERR_VALIDATION, "Entity " + idOrKey + " has type " + source.getTypeKey()
                + ", not " + entityTypeKey);
        }
        List<FilterCondition> conditions = parseConditions(request);
        Specification<XmEntity> spec = buildSpecification(linkSpec.getTypeKey(), request.includeSubTypes(),
            request.getQuery(), conditions, root -> root);

        if (Boolean.TRUE.equals(linkSpec.getIsUnique())) {
            spec = spec.and(notLinkedYet(source.getId(), linkTypeKey));
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
        XmEntityIdKeyTypeKey source = xmEntityService.getXmEntityIdKeyTypeKey(idOrKey);
        Long sourceId = source.getId();
        xmEntitySpecService.getLinkSpec(source.getTypeKey(), linkTypeKey)
            .orElseThrow(() -> new BusinessException(ERR_VALIDATION,
                "Link spec not found for entity type " + source.getTypeKey() + " and link type " + linkTypeKey));
        List<FilterCondition> conditions = parseConditions(request);
        Function<Root<Link>, Path<XmEntity>> target = root -> root.get(Link_.target);

        Specification<Link> spec = (root, query, cb) -> cb.and(
            cb.equal(root.get(Link_.source).get(XmEntity_.id), sourceId),
            cb.equal(root.get(Link_.typeKey), linkTypeKey));
        spec = spec.and(specificationBuilder.build(conditions, target));
        if (StringUtils.isNotBlank(request.getTypeKey())) {
            spec = spec.and(specificationBuilder.typeKey(request.getTypeKey(), request.includeSubTypes(), target));
        }
        if (!XmEntityFilterSpecificationBuilder.includesRemoved(conditions)) {
            spec = spec.and(specificationBuilder.notRemoved(target));
        }
        if (StringUtils.isNotBlank(request.getQuery())) {
            spec = spec.and(specificationBuilder.fullText(request.getQuery(), target));
        }
        return permittedSpecificationRepository.findAll(Link.class, spec,
            sortTranslator.toOrderProvider(pageable.getSort(), target), pageable, privilegeKey);
    }

    /** ENTITY template: JPQL WHERE fragment over alias {@code entity}. Access is controlled on the API level only. */
    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDbByEntityTemplate", resolver = JpqlTemplateKeyResolver.class)
    public Page<XmEntity> searchByEntityTemplate(String templateKey, Map<String, Object> requestParams, Pageable pageable) {
        JpqlTemplate template = jpqlTemplatesService.getTemplate(templateKey);
        if (template.getType() != JpqlTemplateType.ENTITY) {
            throw new BusinessException(ERR_VALIDATION, "Template is not of type ENTITY: " + templateKey);
        }
        Map<String, Object> params = templateParamsService.getParams(templateKey, requestParams);
        return permittedSpecificationRepository.findAll(XmEntity.class, template.getQuery(), params, null,
            sortTranslator.toOrderProvider(pageable.getSort()), pageable, null);
    }

    /** RAW template: full JPQL from tenant config, rows as maps. Access is controlled on the API level only. */
    @Transactional(readOnly = true)
    @LogicExtensionPoint(value = "SearchDbByRawTemplate", resolver = JpqlTemplateKeyResolver.class)
    public JpqlTemplateExecutor.RawResult searchByRawTemplate(String templateKey, Map<String, Object> requestParams,
                                                              Pageable pageable, Function<Object, Object> entityToDto) {
        JpqlTemplate template = jpqlTemplatesService.getTemplate(templateKey);
        if (template.getType() != JpqlTemplateType.RAW) {
            throw new BusinessException(ERR_VALIDATION, "Template is not of type RAW: " + templateKey);
        }
        Map<String, Object> params = templateParamsService.getParams(templateKey, requestParams);
        return templateExecutor.executeRaw(template, params, pageable, entityToDto);
    }

    /** Entities of the target type that this source is not linked to yet, and not the source itself. */
    private static Specification<XmEntity> notLinkedYet(Long sourceId, String linkTypeKey) {
        return (root, query, cb) -> {
            Subquery<Integer> linked = query.subquery(Integer.class);
            Root<Link> link = linked.from(Link.class);
            linked.select(cb.literal(1)).where(
                cb.equal(link.get(Link_.source).get(XmEntity_.id), sourceId),
                cb.equal(link.get(Link_.typeKey), linkTypeKey),
                cb.equal(link.get(Link_.target), root));
            return cb.and(cb.notEqual(root.get(XmEntity_.id), sourceId), cb.not(cb.exists(linked)));
        };
    }

    private static boolean isSameOrSubType(String actualTypeKey, String expectedTypeKey) {
        return actualTypeKey != null
            && (actualTypeKey.equals(expectedTypeKey) || actualTypeKey.startsWith(expectedTypeKey + "."));
    }

    /** Shared by entity search, link-dialog search and link target search. */
    <T> Specification<T> buildSpecification(String typeKey, boolean includeSubTypes, String query,
                                            List<FilterCondition> conditions,
                                            Function<Root<T>, Path<XmEntity>> entityPath) {
        Specification<T> spec = specificationBuilder.typeKey(typeKey, includeSubTypes, entityPath)
            .and(specificationBuilder.build(conditions, entityPath));
        if (!XmEntityFilterSpecificationBuilder.includesRemoved(conditions)) {
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
