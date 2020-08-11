package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Link;
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
public class LinkExportDto {

    private Long id;
    private String typeKey;
    private String name;
    private String description;
    private Instant startDate;
    private Instant endDate;
    private Long targetId;
    private Long sourceId;

    public LinkExportDto(Link link) {

        if (link != null) {
            this.id = link.getId();
            this.typeKey = link.getTypeKey();
            this.name = link.getName();
            this.description = link.getDescription();
            this.startDate = link.getStartDate();
            this.endDate = link.getEndDate();
            this.targetId = Optional.ofNullable(link.getTarget()).map(XmEntity::getId).orElse(null);
            this.sourceId = Optional.ofNullable(link.getSource()).map(XmEntity::getId).orElse(null);
        }
    }

    public Link toLink(XmEntity source, XmEntity target) {
        Link link = new Link();
        link.setTypeKey(this.getTypeKey());
        link.setName(this.getName());
        link.setDescription(this.getDescription());
        link.setStartDate(this.getStartDate());
        link.setEndDate(this.getEndDate());
        link.setTarget(target);
        link.setSource(source);
        return link;
    }
}
