package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.repository.RatingRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
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

    private final PermittedRepository permittedRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private final XmEntityRepository xmEntityRepository;

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
        rating.setXmEntity(xmEntityRepository.getOne(rating.getXmEntity().getId()));
        return ratingRepository.save(rating);
    }

    /**
     *  Get all the ratings.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("RATING.GET_LIST")
    @PrivilegeDescription("Privilege to get all the ratings")
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
        return ratingRepository.findById(id).orElse(null);
    }

    /**
     *  Delete the  rating by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        ratingRepository.deleteById(id);
    }

}
