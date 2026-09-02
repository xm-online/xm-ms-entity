package com.icthh.xm.ms.entity.service;

import static com.google.common.collect.ImmutableMap.of;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyResolver;
import com.icthh.xm.ms.entity.repository.TagRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@LepService(group = "service.tag")
@Transactional
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    private final PermittedRepository permittedRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private final XmEntityRepository xmEntityRepository;

    /**
     * Save a tag.
     *
     * @param tag the entity to save
     * @return the persisted entity
     */
    public Tag save(Tag tag) {

        startUpdateDateGenerationStrategy.preProcessStartDate(tag,
                                                              tag.getId(),
                                                              tagRepository,
                                                              Tag::setStartDate,
                                                              Tag::getStartDate);
        tag.setXmEntity(xmEntityRepository.getOne(tag.getXmEntity().getId()));
        return tagRepository.save(tag);
    }

    /**
     * Get one tag by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Tag findOne(Long id) {
        return tagRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    @FindWithPermission("TAG.GET_LIST")
    @PrivilegeDescription("Privilege to get all the tags")
    public List<Tag> findAll(String privilegeKey) {
        return permittedRepository.findAll(Tag.class, privilegeKey);
    }

    @Transactional(readOnly = true)
    @FindWithPermission("TAG.GET_LIST.BY_XM_ENTITY")
    @LogicExtensionPoint(value = "FindByXmEntity", resolver = TypeKeyResolver.class)
    @PrivilegeDescription("Privilege to search for the tag by xmEntity id and typeKey")
    public Page<Tag> findByXmEntity(Long id, String typeKey, Pageable pageable, String privilegeKey) {
        return permittedRepository.findByCondition("returnObject.xmEntity.id = :id and returnObject.typeKey = :typeKey",
            of("id", id, "typeKey", typeKey), pageable, Tag.class, privilegeKey);
    }

    /**
     * Delete the  tag by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        tagRepository.deleteById(id);
    }

    public List<Tag> saveAll(List<Tag> list) {
        return tagRepository.saveAll(list);
    }

    public void deleteInBatch(List<Tag> list) {
        tagRepository.deleteInBatch(list);
    }
}
