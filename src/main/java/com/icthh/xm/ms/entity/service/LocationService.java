package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.LocationSpec;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LocationService {

    public static final String ZERO_RESTRICTION = "error.location.zero";
    public static final String MAX_RESTRICTION = "error.location.max";

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final LocationRepository locationRepository;

    private final XmEntityRepository xmEntityRepository;

    private final XmEntitySpecService xmEntitySpecService;

    @FindWithPermission("LOCATION.GET_LIST")
    public List<Location> findAll(String privilegeKey) {
        return permittedRepository.findAll(Location.class, privilegeKey);
    }

    /**
     * Search for the location corresponding to the query.
     *
     * @param query the query of the search
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("LOCATION.SEARCH")
    public List<Location> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Location.class, privilegeKey);
    }


    /**
     * Save a xmEntity.
     *
     * @param location the location to save
     * @return the persisted XmEntity
     */
    public Location save(Location location) {
        log.debug("Request to save location : {}", location);

        Objects.nonNull(location);
        Objects.nonNull(location.getXmEntity());
        Objects.nonNull(location.getXmEntity().getId());

        XmEntity entity = xmEntityRepository.findById(location.getXmEntity().getId()).orElseThrow(
            () -> new EntityNotFoundException("No entity found by id: " + location.getXmEntity().getId())
        );

        LocationSpec spec = getSpec(entity, location);

        if (location.getId() == null && spec.getMax() != null) {
            //forbid to add element if spec.max = 0
            assertZeroRestriction(spec);
            //forbid to add element if spec.max <= addedSize
            assertLimitRestriction(spec, entity);
        }

        location.setXmEntity(entity);
        return locationRepository.save(location);
    }

    protected LocationSpec getSpec(XmEntity entity, Location location) {
        Objects.nonNull(entity);
        return xmEntitySpecService
            .findLocation(entity.getTypeKey(), location.getTypeKey())
            .orElseThrow(
                () -> new EntityNotFoundException("Spec.Location not found for entity typeKey " + entity.getTypeKey()
                    + " and locationTypeKey: " + location.getTypeKey())
            );
    }

    protected void assertZeroRestriction(LocationSpec spec) {
        if (Integer.valueOf(0).equals(spec.getMax())) {
            throw new BusinessException(ZERO_RESTRICTION, "Spec for " + spec.getKey() + " allows to add " + spec.getMax() + " elements");
        }
    }

    protected void assertLimitRestriction(LocationSpec spec, XmEntity entity) {
        if (locationRepository.countByXmEntityIdAndTypeKey(entity.getId(), spec.getKey()) >= spec.getMax()) {
            throw new BusinessException(MAX_RESTRICTION, "Spec for " + spec.getKey() + " allows to add " + spec.getMax() + " elements");
        }
    }

}
