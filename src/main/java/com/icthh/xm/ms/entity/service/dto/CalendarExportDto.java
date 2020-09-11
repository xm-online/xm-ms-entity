package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Calendar;
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
public class CalendarExportDto {

    private Long id;
    private String typeKey;
    private String name;
    private String description;
    private Instant startDate;
    private Instant endDate;
    private Long entityId;

    public CalendarExportDto(Calendar calendar) {
        if (calendar != null) {
            this.id = calendar.getId();
            this.typeKey = calendar.getTypeKey();
            this.name = calendar.getName();
            this.description = calendar.getDescription();
            this.startDate = calendar.getStartDate();
            this.endDate = calendar.getEndDate();
            this.entityId = Optional.ofNullable(calendar.getXmEntity()).map(XmEntity::getId).orElse(null);
        }
    }

    public Calendar toCalendar(XmEntity entity) {
        Calendar calendar = new Calendar();
        calendar.setTypeKey(this.getTypeKey());
        calendar.setName(this.getName());
        calendar.setDescription(this.getDescription());
        calendar.setStartDate(this.getStartDate());
        calendar.setEndDate(this.getEndDate());
        calendar.setXmEntity(entity);
        return calendar;
    }

}
