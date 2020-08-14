package com.icthh.xm.ms.entity.service;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.data.domain.PageRequest.of;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.CommentRepository;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.RatingRepository;
import com.icthh.xm.ms.entity.repository.TagRepository;
import com.icthh.xm.ms.entity.repository.VoteRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.AttachmentExportDto;
import com.icthh.xm.ms.entity.service.dto.CalendarExportDto;
import com.icthh.xm.ms.entity.service.dto.CommentExportDto;
import com.icthh.xm.ms.entity.service.dto.EventExportDto;
import com.icthh.xm.ms.entity.service.dto.ExportDto;
import com.icthh.xm.ms.entity.service.dto.ImportDto;
import com.icthh.xm.ms.entity.service.dto.LinkExportDto;
import com.icthh.xm.ms.entity.service.dto.LocationExportDto;
import com.icthh.xm.ms.entity.service.dto.RatingExportDto;
import com.icthh.xm.ms.entity.service.dto.TagsExportDto;
import com.icthh.xm.ms.entity.service.dto.VoteExportDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final LocationRepository locationRepository;
    private final RatingRepository ratingRepository;
    private final VoteRepository voteRepository;

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
            processLocations(importDto, exportDto, typeKey);
            processRating(importDto, exportDto, typeKey);
            processTags(importDto, exportDto, typeKey);
        });
        processComments(exportEntities, importDto);

        return objectMapper.writeValueAsString(importDto).getBytes();
    }

    @LogicExtensionPoint("importEntities")
    public void importEntities(ImportDto importDto) {
        Map<Long, XmEntity> savedEntities = saveEntities(importDto);
        saveLinks(importDto.getLinks(), savedEntities);
        saveCalendars(importDto.getCalendars(), importDto.getEvents(), savedEntities);
        saveAttachments(importDto.getAttachments(), importDto.getContents(), savedEntities);
        saveComments(importDto.getComments(), savedEntities);
        saveLocations(importDto.getLocations(), savedEntities);
        saveTags(importDto.getTags(), savedEntities);
        Map<Long, Rating> savedRatings = saveRatings(importDto.getRatings(), savedEntities);
        saveVotes(importDto.getVotes(), savedRatings, savedEntities);
    }

    private Map<Long, Rating> saveRatings(List<RatingExportDto> ratingExportDtos, Map<Long, XmEntity> savedEntities) {
        Map<Long, Rating> savedRatings = new HashMap<>();
        ratingExportDtos.forEach(ratingExportDto -> {
            XmEntity entity = savedEntities.get(ratingExportDto.getEntityId());
            if (entity == null) {
                log.info("Rating with id: {} skipped", ratingExportDto.getId());
                return;
            }
            Long oldId = ratingExportDto.getId();
            Rating savedRating = ratingRepository.save(ratingExportDto.toRating(entity));
            savedRatings.put(oldId, savedRating);
        });
        return savedRatings;
    }

    private void saveVotes(List<VoteExportDto> voteExportDtos, Map<Long, Rating> savedRatings,
            Map<Long, XmEntity> savedEntities) {
        List<Vote> votes = new ArrayList<>();
        voteExportDtos.forEach(voteExportDto -> {
            XmEntity entity = savedEntities.get(voteExportDto.getEntityId());
            Rating rating = savedRatings.get(voteExportDto.getRatingId());
            if (entity == null || rating == null) {
                log.info("Vote with id: {} skipped", voteExportDto.getId());
                return;
            }
            Vote vote = voteExportDto.toVote(rating, entity);
            votes.add(vote);
        });
        voteRepository.saveAll(votes);
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
        List<Link> links = linkRepository.findBySourceTypeKeyAndTypeKeyIn(typeKey, linkTypeKeys);
        log.info("For typeKey: {} found links count: {}", typeKey, links.size());
        importDto.getLinks().addAll(links.stream().map(LinkExportDto::new).collect(toSet()));
        importDto.getEntities().addAll(links.stream().map(Link::getTarget).collect(toSet()));
    }

    private void processLocations(ImportDto importDto, ExportDto exportDto, String typeKey) {
        List<String> locationTypeKeys = exportDto.getLocationTypeKeys();
        List<Location> locations = locationRepository.findAllByXmEntityTypeKeyAndTypeKeyIn(typeKey, locationTypeKeys);
        log.info("For typeKey: {} found locations count: {}", typeKey, locations.size());
        importDto.getLocations().addAll(locations.stream().map(LocationExportDto::new).collect(toSet()));
    }

    private void processAttachments(ImportDto importDto, ExportDto exportDto, String typeKey) {
        List<String> attTypeKeys = exportDto.getAttachmentTypeKeys();
        List<Attachment> attachments = attachmentRepository.findByXmEntityTypeKeyAndTypeKeyIn(typeKey, attTypeKeys);
        log.info("For typeKey: {} found attachments count: {}", typeKey, attachments.size());
        importDto.getAttachments()
                .addAll(attachments.stream().map(AttachmentExportDto::new).collect(toSet()));
        importDto.getContents().addAll(attachments.stream().map(Attachment::getContent).collect(toSet()));
    }

    private void processRating(ImportDto importDto, ExportDto exportDto, String typeKey) {
        List<String> ratingTypeKeys = exportDto.getRatingTypeKeys();
        List<Rating> ratings = ratingRepository.findByXmEntityTypeKeyAndTypeKeyIn(typeKey, ratingTypeKeys);
        log.info("For typeKey: {} found ratings count: {}", typeKey, ratings.size());
        importDto.getRatings()
                .addAll(ratings.stream().map(RatingExportDto::new).collect(toSet()));
        importDto.getVotes().addAll(ratings.stream()
                .flatMap(rating -> rating.getVotes().stream())
                .map(VoteExportDto::new)
                .collect(toSet()));
    }

    private void processCalendars(ImportDto importDto, ExportDto exportDto, String typeKey) {
        exportDto.getCalendars().forEach(calendarDto -> {
            String calendarTypeKey = calendarDto.getTypeKey();
            List<String> eventTypeKeys = calendarDto.getEventTypeKeys();

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

    private void processComments(Set<ExportDto> exportEntities, ImportDto importDto) {
        List<String> commentsTypeKeys = exportEntities.stream()
                .filter(ExportDto::isComments)
                .map(ExportDto::getTypeKey)
                .collect(toList());
        List<Comment> comments = commentRepository.findAllByXmEntityTypeKeyIn(commentsTypeKeys);
        importDto.getComments().addAll(comments.stream().map(CommentExportDto::new).collect(toSet()));
    }

    private void processTags(ImportDto importDto, ExportDto exportDto, String typeKey) {
        List<String> tagTypeKeys = exportDto.getTagTypeKeys();
        List<Tag> tags = tagRepository.findByXmEntityTypeKeyAndTypeKeyIn(typeKey, tagTypeKeys);
        log.info("For typeKey: {} found tags count: {}", typeKey, tags.size());
        importDto.getTags().addAll(tags.stream().map(TagsExportDto::new).collect(toSet()));

    }

    private Map<Long, XmEntity> saveEntities(ImportDto importDto) {
        Map<Long, XmEntity> savedEntities = new HashMap<>();
        importDto.getEntities().forEach(xmEntity -> {
            Long oldId = xmEntity.getId();
            xmEntity.setId(null);
            XmEntity saved = entityRepository.save(xmEntity);
            savedEntities.put(oldId, saved);
        });
        return savedEntities;
    }

    private void saveLinks(List<LinkExportDto> linkExportDtos, Map<Long, XmEntity> savedEntities) {
        List<Link> links = new ArrayList<>();
        linkExportDtos.forEach(linkExportDto -> {
            XmEntity target = savedEntities.get(linkExportDto.getTargetId());
            XmEntity source = savedEntities.get(linkExportDto.getSourceId());
            if (target == null || source == null) {
                log.info("Link with id: {} skipped", linkExportDto.getId());
                return;
            }
            Link link = linkExportDto.toLink(source, target);
            links.add(link);
        });
        linkRepository.saveAll(links);
    }

    private void saveCalendars(List<CalendarExportDto> calendarExportDtos, List<EventExportDto> eventExportDtos,
            Map<Long, XmEntity> savedEntities) {
        calendarExportDtos.forEach(calendarExportDto -> {
            XmEntity entity = savedEntities.get(calendarExportDto.getEntityId());
            if (entity == null) {
                log.info("Calendar with id: {} skipped", calendarExportDto.getId());
                return;
            }

            Calendar calendar = calendarExportDto.toCalendar(entity);
            Calendar saved = calendarRepository.save(calendar);

            List<Event> events = new ArrayList<>();
            eventExportDtos.forEach(eventExportDto -> {
                if (!Objects.equals(eventExportDto.getCalendarId(), calendarExportDto.getId())) {
                    return;
                }
                XmEntity assigned = ofNullable(savedEntities.get(eventExportDto.getAssignedId())).orElse(null);
                Event event = eventExportDto.toEvent(saved, assigned);
                events.add(event);
            });
            eventRepository.saveAll(events);
        });
    }

    private void saveAttachments(List<AttachmentExportDto> attachmentExportDtos, List<Content> contents,
            Map<Long, XmEntity> savedEntities) {
        List<Attachment> attachments = new ArrayList<>();
        attachmentExportDtos.forEach(attachmentExportDto -> {
            Optional<Content> contentOptional = contents.stream()
                    .filter(cont -> Objects.equals(cont.getId(), attachmentExportDto.getContentId()))
                    .findFirst();
            XmEntity entity = savedEntities.get(attachmentExportDto.getEntityId());

            if (contentOptional.isEmpty() || entity == null) {
                log.info("Attachment with id: {} skipped", attachmentExportDto.getId());
                return;
            }
            Content content = contentOptional.get();
            content.setId(null);
            Attachment attachment = attachmentExportDto.toAttachment(content, entity);
            attachments.add(attachment);
        });
        attachmentRepository.saveAll(attachments);
    }

    private void saveComments(List<CommentExportDto> commentExportDtos, Map<Long, XmEntity> savedEntities) {
        Map<Long, Comment> savedComments = new HashMap<>();
        List<CommentExportDto> comments = commentExportDtos.stream()
                .filter(commentExportDto -> Objects.isNull(commentExportDto.getCommentId())).collect(toList());

        comments.forEach(commentExportDto -> {
            Long oldCommentId = commentExportDto.getId();
            XmEntity entity = savedEntities.get(commentExportDto.getEntityId());
            if (entity == null) {
                log.info("Comment with id: {} skipped", commentExportDto.getId());
                return;
            }
            Comment comment = commentRepository.save(commentExportDto.toComment(entity, null));

            commentExportDtos.remove(commentExportDto);
            savedComments.put(oldCommentId, comment);
        });

        saveCommentsRecursive(commentExportDtos, savedComments, savedEntities);
    }

    private void saveCommentsRecursive(List<CommentExportDto> commentExportDtos, Map<Long, Comment> savedComments,
            Map<Long, XmEntity> savedEntities) {

        if (CollectionUtils.isEmpty(commentExportDtos)) {
            return;
        }
        List<CommentExportDto> comments = commentExportDtos.stream()
                .filter(commentExportDto -> savedComments.containsKey(commentExportDto.getCommentId()))
                .collect(toList());
        comments.forEach(commentExportDto -> {
            XmEntity entity = savedEntities.get(commentExportDto.getEntityId());
            Comment savedComment = savedComments.get(commentExportDto.getCommentId());

            if (entity == null || savedComment == null) {
                log.info("Comment with id: {} skipped", commentExportDto.getId());
                return;
            }
            Long oldCommentId = commentExportDto.getId();
            Comment comment = commentRepository
                    .save(commentExportDto.toComment(entity, savedComment));
            commentExportDtos.remove(commentExportDto);
            savedComments.put(oldCommentId, comment);
        });
        saveCommentsRecursive(commentExportDtos, savedComments, savedEntities);
    }

    private void saveTags(List<TagsExportDto> tagsExportDtos, Map<Long, XmEntity> savedEntities) {
        List<Tag> tags = new ArrayList<>();
        tagsExportDtos.forEach(tagsExportDto -> {
            XmEntity entity = savedEntities.get(tagsExportDto.getEntityId());
            if (entity == null) {
                log.info("Tag with id: {} skipped", tagsExportDto.getId());
                return;
            }
            Tag tag = tagsExportDto.toTag(entity);
            tags.add(tag);
        });
        tagRepository.saveAll(tags);
    }

    private void saveLocations(List<LocationExportDto> locationExportDtos, Map<Long, XmEntity> savedEntities) {
        List<Location> locations = new ArrayList<>();
        locationExportDtos.forEach(locationExportDto -> {
            XmEntity entity = savedEntities.get(locationExportDto.getEntityId());

            if (entity == null) {
                log.info("Location with id: {} skipped", locationExportDto.getId());
                return;
            }
            Location location = locationExportDto.toLocation(entity);
            locations.add(location);
        });
        locationRepository.saveAll(locations);
    }
}
