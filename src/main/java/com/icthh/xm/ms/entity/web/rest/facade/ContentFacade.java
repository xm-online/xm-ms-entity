package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.service.ContentService;
import com.icthh.xm.ms.entity.service.dto.ContentDto;
import com.icthh.xm.ms.entity.service.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContentFacade {

    private final ContentRepository contentRepository;
    private final ContentService contentService;
    private final ContentMapper contentMapper;

    public ContentDto save(ContentDto dto) {
        Content entity = contentMapper.toEntity(dto);
        Content saved = contentRepository.save(entity);
        return contentMapper.toDto(saved);
    }

    public List<ContentDto> findAll(String privilegeKey) {
        return contentService.findAll(privilegeKey).stream()
            .map(contentMapper::toDto)
            .toList();
    }

    public Optional<ContentDto> findById(Long id) {
        return contentRepository.findById(id).map(contentMapper::toDto);
    }

    public void delete(Long id) {
        contentRepository.deleteById(id);
    }
}
