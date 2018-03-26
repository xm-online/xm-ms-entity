package com.icthh.xm.ms.entity.service.impl;

import org.apache.commons.lang3.Validate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Strategy for generation start and update dates.
 *
 * Can be used in XmEntity and other model entities with start/update dates.
 */
@Component
public class StartUpdateDateGenerationStrategy {

    public Instant generateStartDate() {
        return Instant.now();
    }

    public Instant generateUpdateDate() {
        return Instant.now();
    }

    /**
     * Pre-process entity's start date.
     *
     * If entity already exists then start date will be set from oldEntity.
     *
     * Otherwise start date will be generated using {@link StartUpdateDateGenerationStrategy}.
     *
     * @param entity          - Entity to be processed
     * @param id              - Entity ID, nullable
     * @param repository      - Entity repository
     * @param startDateSetter - Entity getStartDate method reference
     * @param startDateGetter - Entity setStartDate method reference
     * @return - Optional old entity found by ID using repository.
     */
    public <T> Optional<T> preProcessStartDate(final T entity,
                                               final Long id,
                                               JpaRepository<T, Long> repository,
                                               BiConsumer<T, Instant> startDateSetter,
                                               Function<T, Instant> startDateGetter) {

        return preProcessStartUpdateDates(entity, id, repository, startDateSetter, startDateGetter, null);

    }

    /**
     * Pre-process entity's start and/or update date.
     *
     * If entity already exists then start and updates date will be set from oldEntity.
     *
     * Otherwise start date will be generated using {@link StartUpdateDateGenerationStrategy}.
     *
     * Update date will always be set from {@link StartUpdateDateGenerationStrategy}
     *
     * @param entity           - Entity to be processed
     * @param id               - Entity ID, nullable
     * @param repository       - Entity repository
     * @param startDateSetter  - Entity getStartDate method reference
     * @param startDateGetter  - Entity setStartDate method reference
     * @param updateDateSetter - Entity setUpdateDate method reference, nullable
     * @return - Optional old entity found by ID using repository.
     */
    public <T> Optional<T> preProcessStartUpdateDates(final T entity,
                                                      final Long id,
                                                      JpaRepository<T, Long> repository,
                                                      BiConsumer<T, Instant> startDateSetter,
                                                      Function<T, Instant> startDateGetter,
                                                      BiConsumer<T, Instant> updateDateSetter) {

        Validate.notNull(entity, "entity can not be null");
        Validate.notNull(repository, "repository can not be null");
        Validate.notNull(startDateSetter, "startDateSetter can not be null");
        Validate.notNull(startDateGetter, "startDateGetter can not be null");

        T oldEntity = null;

        if (id != null) {
            oldEntity = repository.findOne(id);
        }

        if (oldEntity != null) {
            // prevent to update Start date from API
            startDateSetter.accept(entity, startDateGetter.apply(oldEntity));

        } else {
            startDateSetter.accept(entity, generateStartDate());
        }

        if (updateDateSetter != null) {
            updateDateSetter.accept(entity, generateUpdateDate());
        }

        return Optional.ofNullable(oldEntity);
    }

}
