package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.repository.RatingRepository;
import com.icthh.xm.ms.entity.repository.search.RatingSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * Service Implementation for managing Rating.
 */
@Service
@Transactional
public class RatingService {

    private final Logger log = LoggerFactory.getLogger(RatingService.class);

    private final RatingRepository ratingRepository;

    private final RatingSearchRepository ratingSearchRepository;

    public RatingService(RatingRepository ratingRepository, RatingSearchRepository ratingSearchRepository) {
        this.ratingRepository = ratingRepository;
        this.ratingSearchRepository = ratingSearchRepository;
    }

    /**
     * Save a rating.
     *
     * @param rating the entity to save
     * @return the persisted entity
     */
    public Rating save(Rating rating) {
        log.debug("Request to save Rating : {}", rating);
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
    public List<Rating> findAll() {
        log.debug("Request to get all Ratings");
        return ratingRepository.findAll();
    }

    /**
     *  Get one rating by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Rating findOne(Long id) {
        log.debug("Request to get Rating : {}", id);
        return ratingRepository.findOne(id);
    }

    /**
     *  Delete the  rating by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Rating : {}", id);
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
    public List<Rating> search(String query) {
        log.debug("Request to search Ratings for query {}", query);
        return StreamSupport
            .stream(ratingSearchRepository.search(queryStringQuery(query)).spliterator(), false)
            .collect(Collectors.toList());
    }
}
