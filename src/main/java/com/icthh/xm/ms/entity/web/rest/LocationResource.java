package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.service.LocationService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

/**
 * REST controller for managing Location.
 */
@RestController
@RequestMapping("/api")
public class LocationResource {

    private static final String ENTITY_NAME = "location";

    private final LocationRepository locationRepository;
    private final LocationResource locationResource;
    private final LocationService locationService;

    public LocationResource(
                    LocationRepository locationRepository,
                    @Lazy LocationResource locationResource,
                    LocationService locationService) {
        this.locationRepository = locationRepository;
        this.locationResource = locationResource;
        this.locationService = locationService;
    }


    /**
     * POST  /locations : Create a new location.
     *
     * @param location the location to create
     * @return the ResponseEntity with status 201 (Created) and with body the new location, or with status 400 (Bad Request) if the location has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/locations")
    @Timed
    @PreAuthorize("hasPermission({'location': #location}, 'LOCATION.CREATE')")
    public ResponseEntity<Location> createLocation(@Valid @RequestBody Location location) throws URISyntaxException {
        if (location.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new location cannot already have an ID");
        }
        Location result = locationService.save(location);
        return ResponseEntity.created(new URI("/api/locations/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /locations : Updates an existing location.
     *
     * @param location the location to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated location,
     * or with status 400 (Bad Request) if the location is not valid,
     * or with status 500 (Internal Server Error) if the location couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/locations")
    @Timed
    @PreAuthorize("hasPermission({'id': #location.id, 'newLocation': #location}, 'location', 'LOCATION.UPDATE')")
    public ResponseEntity<Location> updateLocation(@Valid @RequestBody Location location) throws URISyntaxException {
        if (location.getId() == null) {
            //in order to call method with permissions check
            return this.locationResource.createLocation(location);
        }
        Location result = locationService.save(location);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, location.getId().toString()))
            .body(result);
    }

    /**
     * GET  /locations : get all the locations.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of locations in body
     */
    @GetMapping("/locations")
    @Timed
    public List<Location> getAllLocations() {
        return locationService.findAll(null);
    }

    /**
     * GET  /locations/:id : get the "id" location.
     *
     * @param id the id of the location to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the location, or with status 404 (Not Found)
     */
    @GetMapping("/locations/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'LOCATION.GET_LIST.ITEM')")
    public ResponseEntity<Location> getLocation(@PathVariable Long id) {
        Location location = locationRepository.findOne(id);
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(location));
    }

    /**
     * DELETE  /locations/:id : delete the "id" location.
     *
     * @param id the id of the location to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/locations/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'location', 'LOCATION.DELETE')")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
