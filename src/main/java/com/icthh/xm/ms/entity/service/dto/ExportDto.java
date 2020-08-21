package com.icthh.xm.ms.entity.service.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ExportDto {

    private String typeKey;
    private List<String> linkTypeKeys = new ArrayList<>();
    private List<String> attachmentTypeKeys = new ArrayList<>();
    private List<CalendarDto> calendars = new ArrayList<>();
    private List<String> locationTypeKeys = new ArrayList<>();
    private List<String> ratingTypeKeys = new ArrayList<>();
    private List<String> tagTypeKeys = new ArrayList<>();
    private boolean comments;

    @Data
    public static class CalendarDto {

        private String typeKey;
        private List<String> eventTypeKeys = new ArrayList<>();
    }


}
