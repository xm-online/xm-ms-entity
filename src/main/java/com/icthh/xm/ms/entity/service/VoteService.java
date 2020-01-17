package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.repository.VoteRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class VoteService {

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final VoteRepository voteRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private final XmEntityRepository xmEntityRepository;

    /**
     * Save a vote.
     *
     * @param vote the entity to save
     * @return the persisted entity
     */
    public Vote save(Vote vote) {

        startUpdateDateGenerationStrategy.preProcessStartDate(vote,
                                                              vote.getId(),
                                                              voteRepository,
                                                              Vote::setEntryDate,

                                                              Vote::getEntryDate);
        vote.setXmEntity(xmEntityRepository.getOne(vote.getXmEntity().getId()));
        return voteRepository.save(vote);
    }

    /**
     * Get one vote by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Vote findOne(Long id) {
        return voteRepository.findById(id).orElse(null);
    }

    @FindWithPermission("VOTE.GET_LIST")
    @PrivilegeDescription("Privilege to get all the votes")
    public Page<Vote> findAll(Pageable pageable, String privilegeKey) {
        return permittedRepository.findAll(pageable, Vote.class, privilegeKey);
    }

    /**
     * Search for the votes corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Deprecated
    @Transactional(readOnly = true)
    @FindWithPermission("VOTE.SEARCH")
    @PrivilegeDescription("Privilege to search for the votes corresponding to the query")
    public Page<Vote> search(String query, Pageable pageable, String privilegeKey) {
        return permittedSearchRepository.search(query, pageable, Vote.class, privilegeKey);
    }

    /**
     * Delete the vote by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        voteRepository.deleteById(id);
    }
}
