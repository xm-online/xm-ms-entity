package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.search.LocationSearchRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LocationService {

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final LocationRepository locationRepository;

    private final LocationSearchRepository locationSearchRepository;

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
        Location result = locationRepository.save(location);
        locationSearchRepository.save(location);
        return result;
    }
}
