package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ImportDto {

    private Set<XmEntity> entities = new HashSet<>();
    private List<LinkExportDto> links = new ArrayList<>();
    private List<AttachmentExportDto> attachments = new ArrayList<>();
    private List<Content> contents = new ArrayList<>();
    private List<CalendarExportDto> calendars = new ArrayList<>();
    private List<EventExportDto> events = new ArrayList<>();
    private List<CommentExportDto> comments = new ArrayList<>();
    private List<TagsExportDto> tags = new ArrayList<>();
    private List<LocationExportDto> locations = new ArrayList<>();
    private List<RatingExportDto> ratings = new ArrayList<>();
    private List<VoteExportDto> votes = new ArrayList<>();
}
