package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.mapper.LinkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkFacade {

    private final LinkService linkService;
    private final LinkMapper linkMapper;

    public LinkDto save(LinkDto dto) {
        Link entity = linkMapper.toEntity(dto);
        Link saved = linkService.save(entity);
        return linkMapper.toDto(saved);
    }

    /**
     * Create links in bulk within a single transaction (all or nothing) using a single repository saveAll.
     */
    @Transactional
    public List<LinkDto> createAll(List<LinkDto> dtos) {
        List<Link> entities = dtos.stream().map(linkMapper::toEntity).toList();
        return linkMapper.toDtoList(linkService.createAll(entities));
    }

    public Page<LinkDto> findAll(Pageable pageable, String privilegeKey) {
        return linkService.findAll(pageable, privilegeKey).map(linkMapper::toDto);
    }

    public LinkDto findOne(Long id) {
        return linkMapper.toDto(linkService.findOne(id));
    }

    public void delete(Long id) {
        linkService.delete(id);
    }

    public Page<LinkDto> findSourcesByXmEntity(Long id, String typeKey, Pageable pageable, String privilegeKey) {
        return linkService.findSourcesByXmEntity(id, typeKey, pageable, privilegeKey).map(linkMapper::toDto);
    }

    public Page<LinkDto> findTargetsByXmEntity(Long id, String typeKey, Pageable pageable, String privilegeKey) {
        return linkService.findTargetsByXmEntity(id, typeKey, pageable, privilegeKey).map(linkMapper::toDto);
    }
}
