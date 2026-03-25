package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.service.CommentService;
import com.icthh.xm.ms.entity.service.dto.CommentDto;
import com.icthh.xm.ms.entity.service.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentFacade {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    public CommentDto save(CommentDto dto) {
        Comment entity = commentMapper.toEntity(dto);
        Comment saved = commentService.save(entity);
        return commentMapper.toDto(saved);
    }

    public Page<CommentDto> findAll(Pageable pageable, String privilegeKey) {
        return commentService.findAll(pageable, privilegeKey).map(commentMapper::toDto);
    }

    public CommentDto findOne(Long id) {
        return commentMapper.toDto(commentService.findOne(id));
    }

    public void delete(Long id) {
        commentService.delete(id);
    }

    public Page<CommentDto> findByXmEntity(Long id, Pageable pageable, String privilegeKey) {
        return commentService.findByXmEntity(id, pageable, privilegeKey).map(commentMapper::toDto);
    }
}
