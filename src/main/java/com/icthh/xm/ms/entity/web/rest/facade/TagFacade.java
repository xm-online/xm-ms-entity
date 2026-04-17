package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.service.TagService;
import com.icthh.xm.ms.entity.service.dto.TagDto;
import com.icthh.xm.ms.entity.service.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagFacade {

    private final TagService tagService;
    private final TagMapper tagMapper;

    public TagDto save(TagDto dto) {
        Tag entity = tagMapper.toEntity(dto);
        Tag saved = tagService.save(entity);
        return tagMapper.toDto(saved);
    }

    public List<TagDto> findAll(String privilegeKey) {
        return tagService.findAll(privilegeKey).stream()
            .map(tagMapper::toDto)
            .toList();
    }

    public TagDto findOne(Long id) {
        return tagMapper.toDto(tagService.findOne(id));
    }

    public void delete(Long id) {
        tagService.delete(id);
    }
}
