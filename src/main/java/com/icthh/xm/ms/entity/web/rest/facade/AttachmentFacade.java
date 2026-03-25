package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.dto.AttachmentDto;
import com.icthh.xm.ms.entity.service.mapper.AttachmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttachmentFacade {

    private final AttachmentService attachmentService;
    private final AttachmentMapper attachmentMapper;

    public AttachmentDto save(AttachmentDto dto) {
        Attachment entity = attachmentMapper.toEntity(dto);
        Attachment saved = attachmentService.save(entity);
        return attachmentMapper.toDto(saved);
    }

    public List<AttachmentDto> findAll(String privilegeKey) {
        return attachmentService.findAll(privilegeKey).stream()
            .map(attachmentMapper::toDto)
            .toList();
    }

    public Optional<AttachmentDto> getOneWithContent(Long id) {
        return attachmentService.getOneWithContent(id).map(attachmentMapper::toDto);
    }

    public void delete(Long id) {
        attachmentService.delete(id);
    }

    public String getAttachmentDownloadLink(Long id) {
        return attachmentService.getAttachmentDownloadLink(id);
    }
}
