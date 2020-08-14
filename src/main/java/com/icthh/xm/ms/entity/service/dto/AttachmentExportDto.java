package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.time.Instant;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class AttachmentExportDto {

    private Long id;
    private String typeKey;
    private String name;
    private String contentUrl;
    private String description;
    private Instant startDate;
    private Instant endDate;
    private String valueContentType;
    private Long valueContentSize;
    private String contentChecksum;
    private Long contentId;
    private Long entityId;

    public AttachmentExportDto(Attachment attachment) {
        if (attachment != null) {
            this.id = attachment.getId();
            this.typeKey = attachment.getTypeKey();
            this.name = attachment.getName();
            this.contentUrl = attachment.getContentUrl();
            this.description = attachment.getDescription();
            this.startDate = attachment.getStartDate();
            this.endDate = attachment.getEndDate();
            this.valueContentType = attachment.getValueContentType();
            this.valueContentSize = attachment.getValueContentSize();
            this.contentChecksum = attachment.getContentChecksum();
            this.contentId = Optional.ofNullable(attachment.getContent()).map(Content::getId).orElse(null);
            this.entityId = Optional.ofNullable(attachment.getXmEntity()).map(XmEntity::getId).orElse(null);
        }
    }

    public Attachment toAttachment(Content content, XmEntity entity) {
        Attachment attachment = new Attachment();
        attachment.setTypeKey(this.getTypeKey());
        attachment.setName(this.getName());
        attachment.setContentUrl(this.getContentUrl());
        attachment.setDescription(this.getDescription());
        attachment.setStartDate(this.getStartDate());
        attachment.setEndDate(this.getEndDate());
        attachment.setValueContentType(this.getValueContentType());
        attachment.setValueContentSize(this.getValueContentSize());
        attachment.setContentChecksum(this.getContentChecksum());
        attachment.setContent(content);
        attachment.setXmEntity(entity);
        return attachment;
    }
}
