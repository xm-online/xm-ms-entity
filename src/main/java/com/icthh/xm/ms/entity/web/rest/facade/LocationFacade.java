package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.service.LocationService;
import com.icthh.xm.ms.entity.service.dto.LocationDto;
import com.icthh.xm.ms.entity.service.mapper.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationFacade {

    private final LocationService locationService;
    private final LocationMapper locationMapper;

    public LocationDto save(LocationDto dto) {
        Location entity = locationMapper.toEntity(dto);
        Location saved = locationService.save(entity);
        return locationMapper.toDto(saved);
    }

    public List<LocationDto> findAll(String privilegeKey) {
        return locationService.findAll(privilegeKey).stream()
            .map(locationMapper::toDto)
            .toList();
    }

    public Optional<LocationDto> findById(Long id) {
        return locationService.findById(id).map(locationMapper::toDto);
    }

    public void delete(Long id) {
        locationService.delete(id);
    }
}
