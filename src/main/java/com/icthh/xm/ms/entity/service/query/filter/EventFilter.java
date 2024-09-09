package com.icthh.xm.ms.entity.service.query.filter;

import java.io.Serializable;
import lombok.Data;
import tech.jhipster.service.filter.Filter;
import tech.jhipster.service.filter.InstantFilter;
import tech.jhipster.service.filter.LongFilter;
import tech.jhipster.service.filter.StringFilter;

/**
 * Filter class for the {@link com.icthh.xm.ms.entity.domain.Event} entity. This class is used
 * in {@link com.icthh.xm.ms.entity.web.rest} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /events?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 *
 * <p>NOTE: field names of this class must not match with any fields within
 *      {@link org.springframework.web.bind.annotation.RestController} methods, otherwise it may lead to errors while
 *      binding {@link org.springframework.web.bind.annotation.PathVariable} or
 *      {@link org.springframework.web.bind.annotation.RequestParam}.
 */
@Data
public class EventFilter implements Serializable {

    private LongFilter id;

    private StringFilter typeKey;

    private InstantFilter startDate;

    private InstantFilter endDate;

    private LongFilter assignedId;
}
