package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ContentService {

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    @FindWithPermission("CONTENT.GET_LIST")
    public List<Content> findAll(String privilegeKey) {
        return permittedRepository.findAll(Content.class, privilegeKey);
    }

    /**
     * Search for the content corresponding to the query.
     *
     * @param query    the query of the search
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("CONTENT.SEARCH")
    public List<Content> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Content.class, privilegeKey);
    }
}
