package com.icthh.xm.ms.entity.security.access;

import com.icthh.xm.commons.permission.access.AbstractResourceFactory;
import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.CommentRepository;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.RatingRepository;
import com.icthh.xm.ms.entity.repository.TagRepository;
import com.icthh.xm.ms.entity.repository.VoteRepository;
import com.icthh.xm.ms.entity.service.XmEntityService;

import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class EntityResourceFactory extends AbstractResourceFactory {

    private final AttachmentRepository attachmentRepository;
    private final CalendarRepository calendarRepository;
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final LinkRepository linkRepository;
    private final LocationRepository locationRepository;
    private final RatingRepository ratingRepository;
    private final TagRepository tagRepository;
    private final VoteRepository voteRepository;
    private final XmEntityService xmEntityService;

    public EntityResourceFactory(AttachmentRepository attachmentRepository, CalendarRepository calendarRepository,
                                 CommentRepository commentRepository,
                                 EventRepository eventRepository, LinkRepository linkRepository,
                                 LocationRepository locationRepository, RatingRepository ratingRepository,
                                 TagRepository tagRepository, VoteRepository voteRepository,
                                 @Lazy XmEntityService xmEntityService) {
        this.attachmentRepository = attachmentRepository;
        this.calendarRepository = calendarRepository;
        this.commentRepository = commentRepository;
        this.eventRepository = eventRepository;
        this.linkRepository = linkRepository;
        this.locationRepository = locationRepository;
        this.ratingRepository = ratingRepository;
        this.tagRepository = tagRepository;
        this.voteRepository = voteRepository;
        this.xmEntityService = xmEntityService;
    }

    @Override
    protected Map<String, ? extends ResourceRepository<?, ?>> getRepositories() {
        return Map.of(
            "attachment", attachmentRepository,
            "calendar", calendarRepository,
            "comment", commentRepository,
            "event", eventRepository,
            "link", linkRepository,
            "location", locationRepository,
            "rating", ratingRepository,
            "tag", tagRepository,
            "vote", voteRepository,
           "xmEntity", xmEntityService
        );
    }
}
