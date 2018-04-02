package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.repository.TagRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.TagSearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    private final TagSearchRepository tagSearchRepository;

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

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
        Tag result = tagRepository.save(tag);
        tagSearchRepository.save(result);
        return result;
    }

    /**
     * Get one tag by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Tag findOne(Long id) {
        return tagRepository.findOne(id);
    }

    @Transactional(readOnly = true)
    @FindWithPermission("TAG.GET_LIST")
    public List<Tag> findAll(String privilegeKey) {
        return permittedRepository.findAll(Tag.class, privilegeKey);
    }

    /**
     * Search for the tags corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("TAG.SEARCH")
    public List<Tag> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Tag.class, privilegeKey);
    }

    /**
     * Delete the  tag by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        tagRepository.delete(id);
        tagSearchRepository.delete(id);
    }
}
