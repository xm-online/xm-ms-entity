package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.repository.RatingRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.RatingSearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for managing Rating.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;

    private final RatingSearchRepository ratingSearchRepository;

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    /**
     * Save a rating.
     *
     * @param rating the entity to save
     * @return the persisted entity
     */
    public Rating save(Rating rating) {

        startUpdateDateGenerationStrategy.preProcessStartDate(rating,
                                                              rating.getId(),
                                                              ratingRepository,
                                                              Rating::setStartDate,
                                                              Rating::getStartDate);

        Rating result = ratingRepository.save(rating);
        ratingSearchRepository.save(result);
        return result;
    }

    /**
     *  Get all the ratings.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("RATING.GET_LIST")
    public List<Rating> findAll(String privilegeKey) {
        return permittedRepository.findAll(Rating.class, privilegeKey);
    }

    /**
     *  Get one rating by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Rating findOne(Long id) {
        return ratingRepository.findOne(id);
    }

    /**
     *  Delete the  rating by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        ratingRepository.delete(id);
        ratingSearchRepository.delete(id);
    }

    /**
     * Search for the rating corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("RATING.SEARCH")
    public List<Rating> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Rating.class, privilegeKey);
    }
}
