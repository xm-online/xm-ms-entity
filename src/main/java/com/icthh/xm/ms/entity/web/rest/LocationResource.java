package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.service.dto.LocationDto;
import com.icthh.xm.ms.entity.web.rest.facade.LocationFacade;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import jakarta.validation.Valid;
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

/**
 * REST controller for managing Location.
 */
@RestController
@RequestMapping("/api")
public class LocationResource {

    private static final String ENTITY_NAME = "location";

    private final LocationResource locationResource;
    private final LocationFacade locationFacade;

    public LocationResource(
                    @Lazy LocationResource locationResource,
                    LocationFacade locationFacade) {
        this.locationResource = locationResource;
        this.locationFacade = locationFacade;
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
    @PrivilegeDescription("Privilege to create a new location")
    public ResponseEntity<LocationDto> createLocation(@Valid @RequestBody LocationDto location) throws URISyntaxException {
        if (location.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new location cannot already have an ID");
        }
        LocationDto result = locationFacade.save(location);
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
    @PrivilegeDescription("Privilege to updates an existing location")
    public ResponseEntity<LocationDto> updateLocation(@Valid @RequestBody LocationDto location) throws URISyntaxException {
        if (location.getId() == null) {
            //in order to call method with permissions check
            return this.locationResource.createLocation(location);
        }
        LocationDto result = locationFacade.save(location);
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
    public List<LocationDto> getAllLocations() {
        return locationFacade.findAll(null);
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
    @PrivilegeDescription("Privilege to get the location by id")
    public ResponseEntity<LocationDto> getLocation(@PathVariable Long id) {
        return RespContentUtil.wrapOrNotFound(locationFacade.findById(id));
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
    @PrivilegeDescription("Privilege to delete the location by id")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationFacade.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
