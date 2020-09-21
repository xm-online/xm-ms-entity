package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.LocationSpec;
import com.icthh.xm.ms.entity.lep.keyresolver.LocationTypeKeyResolver;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@LepService(group = "service.location")
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
    @Transactional(readOnly = true)
    @LogicExtensionPoint("FindById")
    public Optional<Location> findById(Long id) {
        return locationRepository.findById(id);
    }


    @FindWithPermission("LOCATION.GET_LIST")
    @LogicExtensionPoint("FindAll")
    @Transactional(readOnly = true)
    @PrivilegeDescription("Privilege to get all the locations")
    public List<Location> findAll(String privilegeKey) {
        return permittedRepository.findAll(Location.class, privilegeKey);
    }

    /**
     * Search for the location corresponding to the query.
     *
     * @param query the query of the search
     * @return the list of entities
     */
    @Deprecated
    @Transactional(readOnly = true)
    @FindWithPermission("LOCATION.SEARCH")
    @PrivilegeDescription("Privilege to search for the location corresponding to the query")
    public List<Location> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, Location.class, privilegeKey);
    }


    /**
     * Save a xmEntity.
     *
     * @param location the location to save
     * @return the persisted XmEntity
     */
    @LogicExtensionPoint(value = "Save", resolver = LocationTypeKeyResolver.class)
    public Location save(Location location) {
        log.debug("Request to save location : {}", location);

        XmEntity entity = Optional.ofNullable(location)
            .map(Location::getXmEntity)
            .map(XmEntity::getId)
            .flatMap(xmEntityRepository::findById)
            .orElseThrow(
                () -> new EntityNotFoundException("No entity found by id")
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
        Objects.requireNonNull(entity);
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

    /**
     * Delete location by ID
     *
     * @param locationId
     */
    @LogicExtensionPoint("Delete")
    public void delete(long locationId) {
        log.debug("Request to delete location : {}", locationId);
        locationRepository.deleteById(locationId);
    }

}
