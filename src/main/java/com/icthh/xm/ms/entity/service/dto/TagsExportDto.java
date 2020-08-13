package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Tag;
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
public class TagsExportDto {

    private Long id;
    private String typeKey;
    private String name;
    private Instant startDate;
    private Long entityId;

    public TagsExportDto(Tag tag) {

        if (tag != null) {
            this.id = tag.getId();
            this.typeKey = tag.getTypeKey();
            this.name = tag.getName();
            this.startDate = tag.getStartDate();
            this.entityId = Optional.ofNullable(tag.getXmEntity()).map(XmEntity::getId).orElse(null);
        }
    }

    public Tag toTag(XmEntity entity) {
        Tag tag = new Tag();
        tag.setTypeKey(this.getTypeKey());
        tag.setName(this.getName());
        tag.setStartDate(this.getStartDate());
        tag.setXmEntity(entity);
        return tag;
    }
}
