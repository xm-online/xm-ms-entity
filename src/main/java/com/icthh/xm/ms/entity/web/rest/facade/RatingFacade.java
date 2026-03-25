package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.service.RatingService;
import com.icthh.xm.ms.entity.service.dto.RatingDto;
import com.icthh.xm.ms.entity.service.mapper.RatingMapper;
import com.icthh.xm.ms.entity.web.rest.dto.RatingCountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingFacade {

    private final RatingService ratingService;
    private final RatingMapper ratingMapper;

    public RatingDto save(RatingDto dto) {
        Rating entity = ratingMapper.toEntity(dto);
        Rating saved = ratingService.save(entity);
        return ratingMapper.toDto(saved);
    }

    public List<RatingDto> findAll(String privilegeKey) {
        return ratingService.findAll(privilegeKey).stream()
            .map(ratingMapper::toDto)
            .toList();
    }

    public RatingDto findOne(Long id) {
        return ratingMapper.toDto(ratingService.findOne(id));
    }

    public void delete(Long id) {
        ratingService.delete(id);
    }

    public RatingCountDto getVotesCount(Long id) {
        return ratingService.getVotesCount(id);
    }
}
