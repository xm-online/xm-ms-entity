package com.icthh.xm.ms.entity.security.access;

import com.icthh.xm.commons.permission.access.ResourceFactory;
import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.CommentRepository;
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.RatingRepository;
import com.icthh.xm.ms.entity.repository.TagRepository;
import com.icthh.xm.ms.entity.repository.VoteRepository;
import com.icthh.xm.ms.entity.service.XmEntityService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;

@Component
public class EntityResourceFactory implements ResourceFactory {

    private Map<String, ResourceRepository> repositories = new HashMap<>();

    private final AttachmentRepository attachmentRepository;
    private final CalendarRepository calendarRepository;
    private final CommentRepository commentRepository;
    private final ContentRepository contentRepository;
    private final EventRepository eventRepository;
    private final LinkRepository linkRepository;
    private final LocationRepository locationRepository;
    private final RatingRepository ratingRepository;
    private final TagRepository tagRepository;
    private final VoteRepository voteRepository;
    private final XmEntityService xmEntityService;

    public EntityResourceFactory(AttachmentRepository attachmentRepository, CalendarRepository calendarRepository,
                                 CommentRepository commentRepository, ContentRepository contentRepository,
                                 EventRepository eventRepository, LinkRepository linkRepository,
                                 LocationRepository locationRepository, RatingRepository ratingRepository,
                                 TagRepository tagRepository, VoteRepository voteRepository,
                                 @Lazy XmEntityService xmEntityService) {
        this.attachmentRepository = attachmentRepository;
        this.calendarRepository = calendarRepository;
        this.commentRepository = commentRepository;
        this.contentRepository = contentRepository;
        this.eventRepository = eventRepository;
        this.linkRepository = linkRepository;
        this.locationRepository = locationRepository;
        this.ratingRepository = ratingRepository;
        this.tagRepository = tagRepository;
        this.voteRepository = voteRepository;
        this.xmEntityService = xmEntityService;
    }

    @PostConstruct
    public void init() {
        repositories.put("attachment", attachmentRepository);
        repositories.put("calendar", calendarRepository);
        repositories.put("comment", commentRepository);
        repositories.put("content", contentRepository);
        repositories.put("event", eventRepository);
        repositories.put("link", linkRepository);
        repositories.put("location", locationRepository);
        repositories.put("rating", ratingRepository);
        repositories.put("tag", tagRepository);
        repositories.put("vote", voteRepository);
        repositories.put("xmEntity", xmEntityService);
    }

    @Override
    public Object getResource(Object resourceId, String objectType) {
        Object result = null;
        ResourceRepository resourceRepository = repositories.get(objectType);
        if (resourceRepository != null) {
            result = resourceRepository.findById(resourceId);
        }
        return result;
    }
}
