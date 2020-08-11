package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ImportDto {
    private Set<XmEntity> entities = new HashSet<>();
    private Set<LinkExportDto> links = new HashSet<>();
    private Set<AttachmentExportDto> attachments = new HashSet<>();
    private Set<Content> contents = new HashSet<>();
    private Set<CalendarExportDto> calendars = new HashSet<>();
    private Set<EventExportDto> events = new HashSet<>();
}
