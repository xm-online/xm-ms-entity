package com.icthh.xm.ms.entity.service;

import static java.lang.Integer.MAX_VALUE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.data.domain.PageRequest.of;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.service.dto.ExportDto;
import com.icthh.xm.ms.entity.service.dto.ImportDto;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.AttachmentExportDto;
import com.icthh.xm.ms.entity.service.dto.CalendarExportDto;
import com.icthh.xm.ms.entity.service.dto.EventExportDto;
import com.icthh.xm.ms.entity.service.dto.LinkExportDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@LepService(group = "service.exportImport")
@Transactional
@RequiredArgsConstructor
public class ExportImportService {

    private final XmEntityRepository entityRepository;
    private final ObjectMapper objectMapper;
    private final AttachmentRepository attachmentRepository;
    private final CalendarRepository calendarRepository;
    private final LinkRepository linkRepository;
    private final EventRepository eventRepository;

    @SneakyThrows
    @LogicExtensionPoint("exportEntities")
    public byte[] exportEntities(Set<ExportDto> exportEntities) {
        ImportDto importDto = new ImportDto();
        processXmEntities(exportEntities, importDto);

        exportEntities.forEach(exportDto -> {
            String typeKey = exportDto.getTypeKey();
            processLinks(importDto, exportDto, typeKey);
            processAttachments(importDto, exportDto, typeKey);
            processCalendars(importDto, exportDto, typeKey);
        });
        return objectMapper.writeValueAsString(importDto).getBytes();
    }

    @LogicExtensionPoint("importEntities")
    public void importEntities(ImportDto importDto) {
        saveEntities(importDto);
        saveLinks(importDto);
        saveCalendars(importDto);
        saveAttachments(importDto);
    }

    private void processXmEntities(Set<ExportDto> exportEntities, ImportDto importDto) {
        Set<String> entityTypeKeys = exportEntities.stream().map(ExportDto::getTypeKey).collect(toSet());
        List<XmEntity> entities = entityRepository.findAllByTypeKeyIn(of(0, MAX_VALUE), entityTypeKeys)
                .getContent();
        importDto.getEntities().addAll(entities);
        log.info("Found export entities count: {}", entities.size());
    }

    private void processLinks(ImportDto importDto, ExportDto exportDto, String typeKey) {
        List<String> linkTypeKeys = exportDto.getLinkTypeKeys();
        List<Link> links = linkRepository.findBySourceTypeKeyAndTargetTypeKeyIn(typeKey, linkTypeKeys);
        log.info("For typeKey: {} found links count: {}", typeKey, links.size());
        importDto.getLinks().addAll(links.stream().map(LinkExportDto::new).collect(toSet()));
        importDto.getEntities().addAll(links.stream().map(Link::getTarget).collect(toSet()));
    }

    private void processAttachments(ImportDto importDto, ExportDto exportDto, String typeKey) {
        List<String> attTypeKeys = exportDto.getAttachmentTypeKeys();
        List<Attachment> attachments = attachmentRepository.findByXmEntityTypeKeyAndTypeKeyIn(typeKey, attTypeKeys);
        log.info("For typeKey: {} found attachments count: {}", typeKey, attachments.size());
        importDto.getAttachments()
                .addAll(attachments.stream().map(AttachmentExportDto::new).collect(toSet()));
        importDto.getContents().addAll(attachments.stream().map(Attachment::getContent).collect(toSet()));
    }

    private void processCalendars(ImportDto importDto, ExportDto exportDto, String typeKey) {
        exportDto.getCalendars().forEach(calendarDto -> {
            String calendarTypeKey = calendarDto.getTypeKey();
            List<String> eventTypeKeys = calendarDto.getEvents();

            Set<Calendar> calendars = calendarRepository
                    .findByXmEntityTypeKeyAndTypeKeyAndEventsTypeKeyIn(typeKey, calendarTypeKey, eventTypeKeys);
            log.info("For typeKey: {} and calendar typeKey: {} found calendars count: {}", typeKey, calendarTypeKey,
                    calendars.size());

            List<Event> events = calendars.stream()
                    .flatMap(calendar -> calendar.getEvents().stream())
                    .filter(event -> eventTypeKeys.contains(event.getTypeKey()))
                    .collect(toList());
            log.info("For typeKey: {} and calendar typeKey: {} found events count: {}", typeKey, calendarTypeKey,
                    events.size());

            importDto.getCalendars().addAll(calendars.stream().map(CalendarExportDto::new).collect(toSet()));
            importDto.getEvents().addAll(events.stream().map(EventExportDto::new).collect(toSet()));

        });
    }

    private void saveEntities(ImportDto importDto) {
        importDto.getEntities().forEach(xmEntity -> {
            Long oldId = xmEntity.getId();
            xmEntity.setId(null);
            xmEntity.setId(entityRepository.save(xmEntity).getId());
            importDto.getLinks().forEach(link -> {
                if (Objects.equals(link.getSourceId(), oldId)) {
                    link.setSourceId(xmEntity.getId());
                }
                if (Objects.equals(link.getTargetId(), oldId)) {
                    link.setTargetId(xmEntity.getId());
                }
            });
            importDto.getAttachments().forEach(attachmentExportDto -> {
                if (Objects.equals(attachmentExportDto.getEntityId(), oldId)) {
                    attachmentExportDto.setEntityId(xmEntity.getId());
                }
            });
            importDto.getCalendars().forEach(calendarExportDto -> {
                if (Objects.equals(calendarExportDto.getEntityId(), oldId)) {
                    calendarExportDto.setEntityId(xmEntity.getId());
                }
            });
            importDto.getEvents().forEach(eventExportDto -> {
                if (Objects.equals(eventExportDto.getAssignedId(), oldId)) {
                    eventExportDto.setAssignedId(xmEntity.getId());
                }
            });
        });
    }

    private void saveLinks(ImportDto importDto) {
        List<Link> links = new ArrayList<>();
        importDto.getLinks().forEach(linkExportDto -> {
            Optional<XmEntity> targetOptional = importDto.getEntities().stream()
                    .filter(xmEntity -> Objects.equals(xmEntity.getId(), linkExportDto.getTargetId())).findFirst();
            Optional<XmEntity> sourceOptional = importDto.getEntities().stream()
                    .filter(xmEntity -> Objects.equals(xmEntity.getId(), linkExportDto.getSourceId())).findFirst();

            if (targetOptional.isEmpty() || sourceOptional.isEmpty()) {
                log.info("Link with id: {} skipped", linkExportDto.getId());
                return;
            }
            Link link = linkExportDto.toLink(sourceOptional.get(), targetOptional.get());
            links.add(link);
        });
        linkRepository.saveAll(links);
    }

    private void saveCalendars(ImportDto importDto) {
        importDto.getCalendars().forEach(calendarExportDto -> {
            Optional<XmEntity> entityOptional = importDto.getEntities().stream()
                    .filter(xmEntity -> Objects.equals(xmEntity.getId(), calendarExportDto.getEntityId()))
                    .findFirst();

            if (entityOptional.isEmpty()) {
                log.info("Calendar with id: {} skipped", calendarExportDto.getId());
                return;
            }

            Calendar calendar = calendarExportDto.toCalendar(entityOptional.get());
            Calendar saved = calendarRepository.save(calendar);

            List<Event> events = new ArrayList<>();
            importDto.getEvents().forEach(eventExportDto -> {
                if (!Objects.equals(eventExportDto.getCalendarId(), calendarExportDto.getId())) {
                    return;
                }
                XmEntity assigned = importDto.getEntities().stream()
                        .filter(xmEntity -> Objects.equals(xmEntity.getId(), eventExportDto.getAssignedId()))
                        .findFirst().orElse(null);
                Event event = eventExportDto.toEvent(saved, assigned);
                events.add(event);
            });
            eventRepository.saveAll(events);
        });
    }

    private void saveAttachments(ImportDto importDto) {
        List<Attachment> attachments = new ArrayList<>();
        importDto.getAttachments().forEach(attachmentExportDto -> {
            Optional<Content> contentOptional = importDto.getContents().stream()
                    .filter(cont -> Objects.equals(cont.getId(), attachmentExportDto.getContentId()))
                    .findFirst();
            Optional<XmEntity> entityOptional = importDto.getEntities().stream()
                    .filter(xmEntity -> Objects.equals(xmEntity.getId(), attachmentExportDto.getEntityId()))
                    .findFirst();

            if (contentOptional.isEmpty() || entityOptional.isEmpty()) {
                log.info("Attachment with id: {} skipped", attachmentExportDto.getId());
                return;
            }
            Content content = contentOptional.get();
            content.setId(null);
            Attachment attachment = attachmentExportDto.toAttachment(content, entityOptional.get());
            attachments.add(attachment);
        });
        attachmentRepository.saveAll(attachments);
    }

}
