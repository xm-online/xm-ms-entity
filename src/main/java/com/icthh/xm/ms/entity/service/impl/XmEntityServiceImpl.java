package com.icthh.xm.ms.entity.service.impl;

import static com.icthh.xm.ms.entity.domain.spec.LinkSpec.NEW_BUILDER_TYPE;
import static com.icthh.xm.ms.entity.domain.spec.LinkSpec.SEARCH_BUILDER_TYPE;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;
import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections.MapUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.springframework.beans.BeanUtils.isSimpleValueType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.domain.FileFormatEnum;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.SimpleExportXmEntityDto;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.UniqueField;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.converter.EntityToCsvConverterUtils;
import com.icthh.xm.ms.entity.domain.converter.EntityToExcelConverterUtils;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.LinkSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UniqueFieldSpec;
import com.icthh.xm.ms.entity.domain.template.TemplateParamsHolder;
import com.icthh.xm.ms.entity.lep.keyresolver.TemplateTypeKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.XmEntityTypeKeyResolver;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.XmEntityPermittedRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntityPermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.LifecycleLepStrategy;
import com.icthh.xm.ms.entity.service.LifecycleLepStrategyFactory;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.XmEntityTemplatesSpecService;
import com.icthh.xm.ms.entity.service.dto.LinkSourceDto;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing XmEntity.
 */
@Slf4j
@LepService(group = "service.entity", name = "default")
@Transactional
@RequiredArgsConstructor
public class XmEntityServiceImpl implements XmEntityService {

    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityTemplatesSpecService xmEntityTemplatesSpecService;
    private final XmEntityRepository xmEntityRepository;
    private final XmEntitySearchRepository xmEntitySearchRepository;
    private final LifecycleLepStrategyFactory lifecycleLepStrategyFactory;
    private final XmEntityPermittedRepository xmEntityPermittedRepository;
    private final ProfileService profileService;
    private final LinkService linkService;
    private final StorageService storageService;
    private final AttachmentService attachmentService;
    private final XmEntityPermittedSearchRepository xmEntityPermittedSearchRepository;
    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;
    private final XmAuthenticationContextHolder authContextHolder;
    private final ObjectMapper objectMapper;
    private final TenantConfigService tenantConfigService;

    private XmEntityServiceImpl self;

    /**
     * Save a xmEntity.
     * When you call this method will be run save LEP by xmEntity type
     *
     * @param xmEntity the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint(value = "Save", resolver = XmEntityTypeKeyResolver.class)
    public XmEntity save(XmEntity xmEntity) {
        return self.saveXmEntity(xmEntity);
    }

    /**
     * Save a xmEntity.
     * When you call this method will be run general save LEP
     *
     * @param xmEntity the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint("Save")
    public XmEntity saveXmEntity(XmEntity xmEntity) {
        log.debug("Request to save XmEntity : {}", xmEntity);

        Optional<XmEntity> oldEntity = startUpdateDateGenerationStrategy
            .preProcessStartUpdateDates(xmEntity,
                                        xmEntity.getId(),
                                        xmEntityRepository,
                                        XmEntity::setStartDate,
                                        XmEntity::getStartDate,
                                        XmEntity::setUpdateDate);

        if (oldEntity.isPresent()) {
            preventRenameTenant(xmEntity, oldEntity.get());
            if (xmEntity.getStateKey() == null) {
                xmEntity.setStateKey(oldEntity.get().getStateKey());
            }
        } else if (xmEntity.getCreatedBy() == null) {
            xmEntity.setCreatedBy(authContextHolder.getContext().getUserKey().orElse(null));
            xmEntity.setStateKey(xmEntitySpecService.findFirstStateForTypeKey(xmEntity.getTypeKey()));
        }

        // FIXME It is hack to link each tag with entity before persisting. may be there is more elegant solution.
        xmEntity.updateXmEntityReference(xmEntity.getAttachments(), Attachment::setXmEntity);
        xmEntity.updateXmEntityReference(xmEntity.getCalendars(), Calendar::setXmEntity);
        xmEntity.updateXmEntityReference(xmEntity.getLocations(), Location::setXmEntity);
        xmEntity.updateXmEntityReference(xmEntity.getRatings(), Rating::setXmEntity);
        xmEntity.updateXmEntityReference(xmEntity.getTags(), Tag::setXmEntity);
        xmEntity.updateXmEntityReference(xmEntity.getComments(), Comment::setXmEntity);
        xmEntity.updateXmEntityReference(xmEntity.getTargets(), Link::setSource);
        xmEntity.updateXmEntityReference(xmEntity.getSources(), Link::setTarget);
        xmEntity.updateXmEntityReference(xmEntity.getVotes(), Vote::setXmEntity);
        nullSafe(xmEntity.getTargets()).forEach(link -> link.setTarget(xmEntityRepository.getOne(link.getTarget().getId())));
        nullSafe(xmEntity.getSources()).forEach(link -> link.setSource(xmEntityRepository.getOne(link.getSource().getId())));
        processUniqueField(xmEntity, oldEntity);

        // TODO: amedved: use saveAndFlash() here because old entity was returned if use save()
        // as a result old data may be persisted to elasticsearch

        return xmEntityRepository.save(xmEntity);
    }

    private void preventRenameTenant(XmEntity xmEntity, XmEntity oldEntity) {
        if (Constants.TENANT_TYPE_KEY.equals(xmEntity.getTypeKey())
            && !xmEntity.getName().equals(oldEntity.getName())) {
            xmEntity.setName(oldEntity.getName());
        }
    }

    @SneakyThrows
    private void processUniqueField(XmEntity xmEntity, Optional<XmEntity> oldEntity) {
        oldEntity.ifPresent(it -> it.getUniqueFields().clear());
        xmEntity.getUniqueFields().clear();

        if (isEmpty(xmEntity.getData())) {
            return;
        }

        String json = objectMapper.writeValueAsString(xmEntity.getData());
        TypeSpec typeByKey = xmEntitySpecService.findTypeByKey(xmEntity.getTypeKey());

        if (CollectionUtils.isEmpty(typeByKey.getUniqueFields())) {
            return;
        }

        DocumentContext document = JsonPath.using(defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS)).parse(json);

        for (UniqueFieldSpec uniqueFieldSpec: typeByKey.getUniqueFields()) {
            String jsonPath = uniqueFieldSpec.getJsonPath();
            String value = convertToString(document.read(jsonPath));

            if (isNoneBlank(value)) {
                UniqueField uniqueField = UniqueField.builder()
                    .entityTypeKey(xmEntity.getTypeKey())
                    .fieldJsonPath(jsonPath)
                    .fieldValue(value)
                    .xmEntity(xmEntity)
                    .build();
                xmEntity.getUniqueFields().add(uniqueField);
            }
        }
    }

    @SneakyThrows
    private String convertToString(Object value) {
        if (value == null) {
            return "";
        }

        if (!isSimpleValueType(value.getClass())) {
            return objectMapper.writeValueAsString(value);
        }

        return String.valueOf(value);
    }

    /**
     * Get all the xmEntities.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Override
    @Transactional(readOnly = true)
    @FindWithPermission("XMENTITY.GET_LIST")
    @LogicExtensionPoint(value = "FindAll", resolver = TypeKeyResolver.class)
    public Page<XmEntity> findAll(Pageable pageable, String typeKey, String privilegeKey) {
        log.debug("Request to get all XmEntities");
        if (StringUtils.isNoneBlank(typeKey)) {
            Set<String> typeKeys = xmEntitySpecService.findNonAbstractTypesByPrefix(typeKey).stream()
                .map(TypeSpec::getKey).collect(Collectors.toSet());
            log.debug("Find by typeKeys {}", typeKeys);
            return xmEntityPermittedRepository.findAllByTypeKeyIn(pageable, typeKeys, privilegeKey);
        } else {
            return xmEntityPermittedRepository.findAll(pageable, XmEntity.class, privilegeKey);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @FindWithPermission("XMENTITY.GET_LIST")
    public Page<XmEntity> findByIds(Pageable pageable, Set<Long> ids, Set<String> embed, String privilegeKey) {
        return xmEntityPermittedRepository.findAllByIdsWithEmbed(pageable, ids, embed, privilegeKey);
    }

    @Transactional(readOnly = true)
    @Override
    public List<XmEntity> findAll(Specification<XmEntity> spec) {
        return xmEntityRepository.findAll(spec);
    }

    /***
     * Only for lep usage
     */
    @Transactional(readOnly = true)
    @Override
    public List<XmEntity> findAll(String jpql, Map<String, Object> args, List<String> embed) {
        return xmEntityRepository.findAll(jpql, args, embed);
    }

    /**
     * Get one xmEntity by id or key.
     *
     * @param idOrKey the id or key of the entity
     * @return the entity
     */
    @LogicExtensionPoint("FindOne")
    @Override
    @Transactional(readOnly = true)
    public XmEntity findOne(IdOrKey idOrKey) {
        log.debug("Request to get XmEntity : {}", idOrKey);
        Long xmEntityId;
        if (idOrKey.isKey()) {
            XmEntityIdKeyTypeKey projection = getXmEntityIdKeyTypeKey(idOrKey);
            xmEntityId = projection.getId();
        } else {
            xmEntityId = idOrKey.getId();
        }
        return findOneById(xmEntityId);
    }

    private XmEntity findOneById(Long xmEntityId) {
        XmEntity xmEntity = xmEntityRepository.findOneById(xmEntityId);
        return self.getOneEntity(xmEntity);
    }

    // need for lep post processing with split script by typeKey
    @LogicExtensionPoint(value = "FindOnePostProcessing", resolver = XmEntityTypeKeyResolver.class)
    public XmEntity getOneEntity(XmEntity xmEntity) {
        return xmEntity;
    }

    @Override
    @Transactional
    public XmEntity selectAndUpdate(IdOrKey idOrKey, Consumer<XmEntity> consumer) {
        log.debug("Request to get XmEntity : {}", idOrKey);
        Long xmEntityId;
        if (idOrKey.isKey()) {
            XmEntityIdKeyTypeKey projection = getXmEntityIdKeyTypeKey(idOrKey);
            xmEntityId = projection.getId();
        } else {
            xmEntityId = idOrKey.getId();
        }
        XmEntity entity = xmEntityRepository.findOneByIdForUpdate(xmEntityId);
        consumer.accept(entity);
        return save(entity);
    }

    /**
     * Delete the  xmEntity by id.
     *
     * @param id the id of the entity
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete XmEntity : {}", id);

        XmEntity xmEntity = xmEntityRepository.findOne(id, asList("targets"));

        self.deleteXmEntityByTypeKeyLep(xmEntity);
    }

    @LogicExtensionPoint(value = "Delete", resolver = XmEntityTypeKeyResolver.class)
    public void deleteXmEntityByTypeKeyLep(XmEntity xmEntity) {
        self.deleteXmEntityGeneralLep(xmEntity.getId(), xmEntity);
    }

    @LogicExtensionPoint("Delete")
    public void deleteXmEntityGeneralLep(Long id, XmEntity xmEntity) {
        deleteXmEntity(xmEntity);
    }

    private void deleteXmEntity(XmEntity xmEntity) {

        TypeSpec spec = xmEntitySpecService.findTypeByKey(xmEntity.getTypeKey());
        Map<String, String> linkSpec = ofNullable(spec)
            .map(TypeSpec::getLinks)
            .orElse(emptyList())
            .stream()
            .collect(toMap(LinkSpec::getKey, LinkSpec::getBuilderType));

        Iterator<Link> iterator = nullSafe(xmEntity.getTargets()).iterator();
        while (iterator.hasNext()) {

            Link target = iterator.next();
            iterator.remove();
            linkService.delete(target.getId());
            if (NEW_BUILDER_TYPE.equalsIgnoreCase(linkSpec.get(target.getTypeKey()))) {
                deleteNewLink(target);
            } else if (!SEARCH_BUILDER_TYPE.equals(linkSpec.get(target.getTypeKey()))) {
                log.warn("Unknown link builder type |{}| for link type {}", linkSpec.get(target.getTypeKey()), target.getTypeKey());
            }
        }

        xmEntityRepository.delete(xmEntity.getId());
    }


    private void deleteNewLink(Link linkToDelete) {
        XmEntity entity = linkToDelete.getTarget();
        entity.getSources().remove(linkToDelete);

        for (Link sourceLink : nullSafe(entity.getSources())) {
            if (!sourceLink.linkFromSameEntity(linkToDelete)) {
                log.warn("Entity {} has links from other entity(ies), so deletion of this entity will be ignored in cascade deletion.", entity);
                return;
            }
        }
        deleteXmEntity(entity);
    }

    /**
     * Search for the xmEntity corresponding to the query.
     *
     * @param query    the query of the search
     * @param pageable the pagination information
     * @return the list of entities
     */
    @LogicExtensionPoint("Search")
    @Override
    @Transactional(readOnly = true)
    @FindWithPermission("XMENTITY.SEARCH")
    public Page<XmEntity> search(String query, Pageable pageable, String privilegeKey) {
        return xmEntityPermittedSearchRepository.search(query, pageable, XmEntity.class, privilegeKey);
    }

    @LogicExtensionPoint(value = "SearchByTemplate", resolver = TemplateTypeKeyResolver.class)
    @Override
    @Transactional(readOnly = true)
    @FindWithPermission("XMENTITY.SEARCH")
    public Page<XmEntity> search(String template, TemplateParamsHolder templateParamsHolder, Pageable pageable, String privilegeKey) {
        String query = getTemplateQuery(template, templateParamsHolder);
        return xmEntityPermittedSearchRepository.search(query, pageable, XmEntity.class, privilegeKey);
    }

    @Override
    @Transactional(readOnly = true)
    @FindWithPermission("XMENTITY.SEARCH")
    public Page<XmEntity> searchByQueryAndTypeKey(String query, String typeKey, Pageable pageable, String privilegeKey) {
        return xmEntityPermittedSearchRepository.searchByQueryAndTypeKey(query, typeKey, pageable, privilegeKey);
    }

    @Override
    @Transactional(readOnly = true)
    @FindWithPermission("XMENTITY.SEARCH")
    public Page<XmEntity> searchByQueryAndTypeKey(String template, TemplateParamsHolder templateParamsHolder, String typeKey, Pageable pageable, String privilegeKey) {
        String query = isBlank(template) ? StringUtils.EMPTY : getTemplateQuery(template, templateParamsHolder);
        return xmEntityPermittedSearchRepository.searchByQueryAndTypeKey(query, typeKey, pageable, privilegeKey);
    }

    private String getTemplateQuery(String template, TemplateParamsHolder templateParamsHolder) {
        return StrSubstitutor.replace(xmEntityTemplatesSpecService.findTemplate(template), templateParamsHolder
                .getTemplateParams());
    }

    @Override
    @Transactional(readOnly = true)
    public XmEntity profile() {
        return profileService.getSelfProfile().getXmentity();
    }

    @LogicExtensionPoint("GetLinkTargets")
    @Override
    public List<Link> getLinkTargets(IdOrKey idOrKey, String typeKey) {
        XmEntity source = toSourceXmEntity(idOrKey);
        return linkService.findBySourceIdAndTargetTypeKey(source.getId(), typeKey);
    }

    @Override
    public List<Link> getLinkSources(IdOrKey idOrKey, String typeKey) {
        XmEntity source = toSourceXmEntity(idOrKey);
        return linkService.findByTargetIdAndTypeKey(source.getId(), typeKey);
    }

    @Override
    public Page<LinkSourceDto> getLinkSourcesInverted(final Pageable pageable, final IdOrKey idOrKey,
                                                      final Set<String> typeKey,
                                                      final String privilegeKey) {
        XmEntity source = toSourceXmEntity(idOrKey);
        return linkService.findSourceByTargetIdAndTypeKey(pageable, source.getId(), typeKey, privilegeKey)
                          .map(LinkSourceDto::new);
    }

    @LogicExtensionPoint("SaveLinkTarget")
    @Override
    public Link saveLinkTarget(IdOrKey idOrKey, Link link, MultipartFile file) {
        //resolve source entity by idOrKey
        XmEntity source = toSourceXmEntity(idOrKey);
        //resolve target entity by key
        XmEntity target = findOne(toIdOrKey(link.getTarget()));

        //save link
        link.setSource(source);
        link.setTarget(target);
        link.setStartDate(Instant.now());
        Link savedLink = linkService.save(link);
        log.debug("Link saved with id {}", link.getId());

        //save file to storage and attachment
        if (file != null) {
            addFileAttachment(savedLink.getTarget(), file);
        }
        return savedLink;
    }

    @Override
    public XmEntity addFileAttachment(XmEntity entity, MultipartFile file) {
        //save multipart file to storage
        String storedFileName = storageService.store(file, null);
        log.debug("Multipart file stored with name {}", storedFileName);

        String targetTypeKey = entity.getTypeKey();
        TypeSpec typeSpec = xmEntitySpecService.findTypeByKey(targetTypeKey);
        if (typeSpec == null || ObjectUtils.isEmpty(typeSpec.getAttachments())) {
            throw new IllegalStateException("Attachment type key not found for entity " + targetTypeKey);
        }

        //get first attachment spec type for now
        String attachmentTypeKey = typeSpec.getAttachments().stream().findFirst().get().getKey();
        log.debug("Attachment type key {}", attachmentTypeKey);

        Attachment attachment = attachmentService.save(new Attachment()
            .typeKey(attachmentTypeKey)
            .name(file.getOriginalFilename())
            .contentUrl(storedFileName)
            .startDate(Instant.now())
            .valueContentType(file.getContentType())
            .valueContentSize(file.getSize())
            .xmEntity(entity));
        log.debug("Attachment stored with id {}", attachment.getId());

        entity.getAttachments().add(attachment);

        return entity;
    }

    @LogicExtensionPoint("UpdateLinkTarget")
    @Override
    public Link updateLinkTarget(IdOrKey idOrKey, String targetId, Link link, MultipartFile file) {
        throw new BusinessException("method 'updateLinkTarget' is not supported");
    }

    @Override
    public void deleteLinkTarget(IdOrKey idOrKey, String linkId) {
        XmEntity source = toSourceXmEntity(idOrKey);
        Long longLinkId = Long.parseLong(linkId);

        Link foundLink = linkService.findOne(longLinkId);
        if (foundLink == null) {
            throw new IllegalArgumentException("Link not found by id " + linkId);
        }

        Long foundSourceId = foundLink.getSource().getId();
        if (!foundSourceId.equals(source.getId())) {
            throw new BusinessException("Wrong source id. Expected " + source.getId() + " found " + foundSourceId);
        }
        log.debug("Delete link by id " + linkId);
        linkService.delete(longLinkId);
    }

    private IdOrKey toIdOrKey(XmEntity xmEntity) {
        return xmEntity.getId() == null ? IdOrKey.ofKey(xmEntity.getKey()) : IdOrKey.of(xmEntity.getId());
    }

    private XmEntity toSourceXmEntity(IdOrKey idOrKey) {
        XmEntity source;
        if (idOrKey.isSelf()) {
            source = profile();
            log.debug("Self resolved entity id = {}, typeKet = {}", source.getId(), source.getTypeKey());
        } else {
            source = findOne(idOrKey);
            log.debug("Resolved entity id = {}, typeKet = {}", source.getId(), source.getTypeKey());
        }
        return source;
    }

    @Deprecated
    @Override
    public void updateState(String id, XmEntity xmEntity) {
        updateState(IdOrKey.ofKey(id), xmEntity.getStateKey(), Collections.emptyMap());
    }

    @Override
    public XmEntity updateState(IdOrKey idOrKey, String stateKey, Map<String, Object> context) {
        XmEntityStateProjection entity = findStateProjectionById(idOrKey)
            .orElseThrow(() -> new EntityNotFoundException("XmEntity with key [" + idOrKey.getKey() + "] not found"));

        List<StateSpec> stateSpecs = xmEntitySpecService.nextStates(entity.getTypeKey(), entity.getStateKey());
        if (stateSpecs.stream().map(StateSpec::getKey).anyMatch(stateKey::equals)) {

            LifecycleLepStrategy lifecycleLepStrategy = lifecycleLepStrategyFactory.getLifecycleLepStrategy();
            return lifecycleLepStrategy.changeState(idOrKey, entity.getTypeKey(), entity.getStateKey(), stateKey, context);
        } else {
            throw new BusinessException(ErrorConstants.ERR_VALIDATION, "Entity " + entity + " can not go from ["
                + entity.getStateKey() + "] to [" + stateKey + "]");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<XmEntityStateProjection> findStateProjectionById(IdOrKey idOrKey) {
        XmEntityStateProjection projection;
        if (idOrKey.isId()) {
            // ID case
            projection = xmEntityRepository.findStateProjectionById(idOrKey.getId());
         } else {
            // KEY case
            projection = xmEntityRepository.findStateProjectionByKey(idOrKey.getKey());
        }
        return ofNullable(projection);
    }

    @Override
    public URI updateAvatar(IdOrKey idOrKey, HttpEntity<Resource> avatarHttpEntity) {
        XmEntity source = toSourceXmEntity(idOrKey);

        String avatarUrl = storageService.store(avatarHttpEntity, null);
        log.info("Avatar {} stored for entity {}", avatarUrl, idOrKey);

        source.setAvatarUrl(avatarUrl);
        save(source);

        return URI.create(avatarUrl);
    }

    @Override
    public Object findById(Object id) {
        return findOne(IdOrKey.of(String.valueOf(id)));
    }

    @LogicExtensionPoint("Export")
    @Override
    public byte[] exportEntities(String fileFormat, String typeKey) {
        Set<String> typeKeys = xmEntitySpecService.findNonAbstractTypesByPrefix(typeKey).stream()
                        .map(TypeSpec::getKey).collect(Collectors.toSet());
        List<XmEntity> xmEntities = xmEntityRepository.findAllByTypeKeyIn(
                        new PageRequest(0, Integer.MAX_VALUE), typeKeys).getContent();

        ModelMapper modelMapper = new ModelMapper();

        List<SimpleExportXmEntityDto> simpleEntities = xmEntities.stream()
                        .map(entity -> modelMapper.map(entity, SimpleExportXmEntityDto.class))
                        .collect(Collectors.toList());
        switch (FileFormatEnum.valueOf(fileFormat.toUpperCase())) {
            case CSV:
                return EntityToCsvConverterUtils.toCsv(simpleEntities,
                                SimpleExportXmEntityDto.class);
            case XLSX:
                return EntityToExcelConverterUtils.toExcel(simpleEntities, typeKey);
            default:
                throw new BusinessException(ErrorConstants.ERR_VALIDATION, String.format(
                                "Converter doesn't support '%s' file format", fileFormat));
        }
    }

    @LogicExtensionPoint("FindOneEmbed")
    @Override
    @Transactional(readOnly = true)
    public XmEntity findOne(IdOrKey idOrKey, List<String> embed) {
        if (idOrKey.isKey()) {
            throw new IllegalArgumentException("Key mode is not supported yet");
        }
        return self.getOneEntity(xmEntityRepository.findOne(idOrKey.getId(), embed));
    }

    @Transactional(readOnly = true)
    @Override
    public XmEntityIdKeyTypeKey getXmEntityIdKeyTypeKey(IdOrKey idOrKey) {
        XmEntityIdKeyTypeKey projection;
        if (idOrKey.isId()) {
            // ID case
            projection = xmEntityRepository.findOneIdKeyTypeKeyById(idOrKey.getId());
            if (projection == null) {
                throw new EntityNotFoundException("XmEntity with id [" + idOrKey.getId() + "] not found");
            }
        } else if (idOrKey.isSelf()) {
            // SELF keys
            XmEntity profile = profile();
            projection = xmEntityRepository.findOneIdKeyTypeKeyById(profile.getId());
            if (projection == null) {
                throw new EntityNotFoundException("XmEntity with key [" + idOrKey.getId() + "] not found");
            }
        } else {
            // KEY case
            projection = xmEntityRepository.findOneIdKeyTypeKeyByKey(idOrKey.getKey());
            if (projection == null) {
                throw new EntityNotFoundException("XmEntity with key [" + idOrKey.getKey() + "] not found");
            }
        }

        return projection;
    }

    @Override
    public boolean existsByTypeKeyIgnoreCase(String typeKey, String name) {
        return xmEntityRepository.existsByTypeKeyAndNameIgnoreCase(typeKey, name);
    }

    @Autowired
    public void setSelf(XmEntityServiceImpl self) {
        if (this.self == null) {
            this.self = self;
        }
    }

}
