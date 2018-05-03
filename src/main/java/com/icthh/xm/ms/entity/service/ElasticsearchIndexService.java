package com.icthh.xm.ms.entity.service;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.CommentRepository;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.FunctionContextRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.ProfileRepository;
import com.icthh.xm.ms.entity.repository.RatingRepository;
import com.icthh.xm.ms.entity.repository.TagRepository;
import com.icthh.xm.ms.entity.repository.VoteRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.AttachmentSearchRepository;
import com.icthh.xm.ms.entity.repository.search.CalendarSearchRepository;
import com.icthh.xm.ms.entity.repository.search.CommentSearchRepository;
import com.icthh.xm.ms.entity.repository.search.EventSearchRepository;
import com.icthh.xm.ms.entity.repository.search.FunctionContextSearchRepository;
import com.icthh.xm.ms.entity.repository.search.LinkSearchRepository;
import com.icthh.xm.ms.entity.repository.search.LocationSearchRepository;
import com.icthh.xm.ms.entity.repository.search.ProfileSearchRepository;
import com.icthh.xm.ms.entity.repository.search.RatingSearchRepository;
import com.icthh.xm.ms.entity.repository.search.TagSearchRepository;
import com.icthh.xm.ms.entity.repository.search.VoteSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.persistence.ManyToMany;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ElasticsearchIndexService {

    private static final Lock reindexLock = new ReentrantLock();

    private final AttachmentRepository attachmentRepository;
    private final AttachmentSearchRepository attachmentSearchRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarSearchRepository calendarSearchRepository;
    private final CommentRepository commentRepository;
    private final CommentSearchRepository commentSearchRepository;
    private final EventRepository eventRepository;
    private final EventSearchRepository eventSearchRepository;
    private final FunctionContextRepository functionContextRepository;
    private final FunctionContextSearchRepository functionContextSearchRepository;
    private final LinkRepository linkRepository;
    private final LinkSearchRepository linkSearchRepository;
    private final LocationRepository locationRepository;
    private final LocationSearchRepository locationSearchRepository;
    private final ProfileRepository profileRepository;
    private final ProfileSearchRepository profileSearchRepository;
    private final RatingRepository ratingRepository;
    private final RatingSearchRepository ratingSearchRepository;
    private final TagRepository tagRepository;
    private final TagSearchRepository tagSearchRepository;
    private final VoteRepository voteRepository;
    private final VoteSearchRepository voteSearchRepository;
    private final XmEntityRepository xmEntityRepository;
    private final XmEntitySearchRepository xmEntitySearchRepository;
    private final ElasticsearchTemplate elasticsearchTemplate;

    public void reindexAll() {
        reindexAllAsync(MdcUtils.getRid());
    }

    @Async
    @Timed
    public void reindexAllAsync(String rid) {
        execForCustomRid(rid, () -> {
            if (reindexLock.tryLock()) {
                try {
                    reindexForClass(Attachment.class, attachmentRepository, attachmentSearchRepository);
                    reindexForClass(Calendar.class, calendarRepository, calendarSearchRepository);
                    reindexForClass(Comment.class, commentRepository, commentSearchRepository);
                    reindexForClass(Event.class, eventRepository, eventSearchRepository);
                    reindexForClass(FunctionContext.class, functionContextRepository, functionContextSearchRepository);
                    reindexForClass(Link.class, linkRepository, linkSearchRepository);
                    reindexForClass(Location.class, locationRepository, locationSearchRepository);
                    reindexForClass(Profile.class, profileRepository, profileSearchRepository);
                    reindexForClass(Rating.class, ratingRepository, ratingSearchRepository);
                    reindexForClass(Tag.class, tagRepository, tagSearchRepository);
                    reindexForClass(Vote.class, voteRepository, voteSearchRepository);
                    reindexForClass(XmEntity.class, xmEntityRepository, xmEntitySearchRepository);

                    log.info("Elasticsearch: Successfully performed reindexing");
                } finally {
                    reindexLock.unlock();
                }
            } else {
                log.info("Elasticsearch: concurrent reindexing attempt");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T, ID extends Serializable> void reindexForClass(Class<T> entityClass, JpaRepository<T, ID> jpaRepository,
                                                              ElasticsearchRepository<T, ID> elasticsearchRepository) {
        elasticsearchTemplate.deleteIndex(entityClass);
        try {
            elasticsearchTemplate.createIndex(entityClass);
        } catch (IndexAlreadyExistsException e) {
            // Do nothing. Index was already concurrently recreated by some other service.
        }
        elasticsearchTemplate.putMapping(entityClass);
        if (jpaRepository.count() > 0) {
            // if a JHipster entity field is the owner side of a many-to-many relationship, it should be loaded manually
            List<Method> relationshipGetters = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.getType().equals(Set.class))
                .filter(field -> field.getAnnotation(ManyToMany.class) != null)
                .filter(field -> field.getAnnotation(ManyToMany.class).mappedBy().isEmpty())
                .filter(field -> field.getAnnotation(JsonIgnore.class) == null)
                .map(field -> {
                    try {
                        return new PropertyDescriptor(field.getName(), entityClass).getReadMethod();
                    } catch (IntrospectionException e) {
                        log.error("Error retrieving getter for class {}, field {}. Field will NOT be indexed",
                            entityClass.getSimpleName(), field.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            int size = 100;
            for (int i = 0; i <= jpaRepository.count() / size; i++) {
                Pageable page = new PageRequest(i, size);
                log.info("Indexing page {} of {}, size {}", i, jpaRepository.count() / size, size);
                Page<T> results = jpaRepository.findAll(page);
                results.map(result -> {
                    // if there are any relationships to load, do it now
                    relationshipGetters.forEach(method -> {
                        try {
                            // eagerly load the relationship set
                            ((Set) method.invoke(result)).size();
                        } catch (Exception ex) {
                            log.error(ex.getMessage());
                        }
                    });
                    return result;
                });
                elasticsearchRepository.save(results.getContent());
            }
        }
        log.info("Elasticsearch: Indexed all rows for {}", entityClass.getSimpleName());
    }

    private void execForCustomRid(String rid, Runnable runnable) {
        final String oldRid = MdcUtils.getRid();
        try {
            MdcUtils.putRid(rid);
            runnable.run();
        } finally {
            if (oldRid != null) {
                MdcUtils.putRid(oldRid);
            } else {
                MdcUtils.removeRid();
            }
        }
    }
}
