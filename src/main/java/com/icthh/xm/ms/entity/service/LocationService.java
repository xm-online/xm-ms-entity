package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.lep.keyresolver.LocationTypeKeyResolver;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@LepService(group = "service.location")
@Transactional
@RequiredArgsConstructor
public class LocationService {

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final LocationRepository locationRepository;

    private final XmEntityRepository xmEntityRepository;

    @Transactional(readOnly = true)
    @LogicExtensionPoint("FindById")
    public Optional<Location> findById(Long id) {
        return locationRepository.findById(id);
    }

    @FindWithPermission("LOCATION.GET_LIST")
    @LogicExtensionPoint("FindAll")
    @Transactional(readOnly = true)
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
    @LogicExtensionPoint(value = "Save", resolver = LocationTypeKeyResolver.class)
    public Location save(Location location) {
        log.debug("Request to save location : {}", location);
        location.setXmEntity(xmEntityRepository.getOne(location.getXmEntity().getId()));
        return locationRepository.save(location);
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
