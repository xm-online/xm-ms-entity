package com.icthh.xm.ms.entity.service.dto;

import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Event;
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
public class EventExportDto {

    private Long id;
    private String typeKey;
    private String repeatRuleKey;
    private String title;
    private String description;
    private Instant startDate;
    private Instant endDate;
    private Long calendarId;
    private Long assignedId;
    private String color;

    public EventExportDto(Event event) {
        if (event != null) {
            this.id = event.getId();
            this.typeKey = event.getTypeKey();
            this.repeatRuleKey = event.getRepeatRuleKey();
            this.title = event.getTitle();
            this.description = event.getDescription();
            this.startDate = event.getStartDate();
            this.endDate = event.getEndDate();
            this.calendarId = Optional.ofNullable(event.getCalendar()).map(Calendar::getId).orElse(null);
            this.assignedId = Optional.ofNullable(event.getAssigned()).map(XmEntity::getId).orElse(null);
            this.color = event.getColor();
        }
    }

    public Event toEvent(Calendar calendar, XmEntity assigned) {
        Event event = new Event();
        event.setTypeKey(this.getTypeKey());
        event.setRepeatRuleKey(this.getRepeatRuleKey());
        event.setTitle(this.getTitle());
        event.setDescription(this.getDescription());
        event.setStartDate(this.getStartDate());
        event.setEndDate(this.getEndDate());
        event.setCalendar(calendar);
        event.setAssigned(assigned);
        event.setColor(this.getColor());
        return event;
    }

}
