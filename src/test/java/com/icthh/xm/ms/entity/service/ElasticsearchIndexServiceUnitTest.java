package com.icthh.xm.ms.entity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
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
import com.icthh.xm.ms.entity.repository.ContentRepository;
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
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchIndexServiceUnitTest {

    @InjectMocks
    private ElasticsearchIndexService service;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private AttachmentSearchRepository attachmentSearchRepository;
    @Mock
    private CalendarRepository calendarRepository;
    @Mock
    private CalendarSearchRepository calendarSearchRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentSearchRepository commentSearchRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventSearchRepository eventSearchRepository;
    @Mock
    private FunctionContextRepository functionContextRepository;
    @Mock
    private FunctionContextSearchRepository functionContextSearchRepository;
    @Mock
    private LinkRepository linkRepository;
    @Mock
    private LinkSearchRepository linkSearchRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private LocationSearchRepository locationSearchRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ProfileSearchRepository profileSearchRepository;
    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private RatingSearchRepository ratingSearchRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TagSearchRepository tagSearchRepository;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private VoteSearchRepository voteSearchRepository;
    @Mock
    private XmEntityRepository xmEntityRepository;
    @Mock
    private XmEntitySearchRepository xmEntitySearchRepository;
    @Mock
    private ElasticsearchTemplate elasticsearchTemplate;
    @Mock
    TenantContextHolder tenantContextHolder;

    @Before
    public void before() {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("XM")));
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);

        PrivilegedTenantContext privilegedTenantContext = mock(PrivilegedTenantContext.class);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
    }

    @Test
    public void reindexAll() {
        prepareInternal(Attachment.class, attachmentRepository);
        prepareInternal(Calendar.class, calendarRepository);
        prepareInternal(Comment.class, commentRepository);
        prepareInternal(Event.class, eventRepository);
        prepareInternal(FunctionContext.class, functionContextRepository);
        prepareInternal(Link.class, linkRepository);
        prepareInternal(Location.class, locationRepository);
        prepareInternal(Profile.class, profileRepository);
        prepareInternal(Rating.class, ratingRepository);
        prepareInternal(Tag.class, tagRepository);
        prepareInternal(Vote.class, voteRepository);
        prepareInternal(XmEntity.class, xmEntityRepository);

        service.reindexAll();

        verifyInternal(Attachment.class, attachmentRepository, attachmentSearchRepository);
        verifyInternal(Calendar.class, calendarRepository, calendarSearchRepository);
        verifyInternal(Comment.class, commentRepository, commentSearchRepository);
        verifyInternal(Event.class, eventRepository, eventSearchRepository);
        verifyInternal(FunctionContext.class, functionContextRepository, functionContextSearchRepository);
        verifyInternal(Link.class, linkRepository, linkSearchRepository);
        verifyInternal(Location.class, locationRepository, locationSearchRepository);
        verifyInternal(Profile.class, profileRepository, profileSearchRepository);
        verifyInternal(Rating.class, ratingRepository, ratingSearchRepository);
        verifyInternal(Tag.class, tagRepository, tagSearchRepository);
        verifyInternal(Vote.class, voteRepository, voteSearchRepository);
        verifyInternal(XmEntity.class, xmEntityRepository, xmEntitySearchRepository);
    }

    @SneakyThrows
    private <T, ID extends Serializable> void prepareInternal(Class<T> entityClass,
        JpaRepository<T, ID> jpaRepository) {
        when(jpaRepository.count()).thenReturn(10L);
        when(jpaRepository.findAll(new PageRequest(0, 100))).thenReturn(
            new PageImpl<>(Collections.singletonList(createObject(entityClass))));
    }

    @SneakyThrows
    private <T, ID extends Serializable> void verifyInternal(Class<T> entityClass, JpaRepository<T, ID> jpaRepository,
        ElasticsearchRepository<T, ID> elasticsearchRepository) {
        verify(elasticsearchTemplate).deleteIndex(entityClass);
        verify(elasticsearchTemplate).createIndex(entityClass);
        verify(elasticsearchTemplate).putMapping(entityClass);

        verify(jpaRepository, times(4)).count();

        ArgumentCaptor<List> list = ArgumentCaptor.forClass(List.class);
        verify(elasticsearchRepository).save(list.capture());

        assertThat(list.getValue()).containsExactly(createObject(entityClass));
    }

    @SneakyThrows
    private static <T> T createObject(Class<T> entityClass) {
        T instance = entityClass.newInstance();
        Field id = ReflectionUtils.findField(entityClass, "id");
        id.setAccessible(true);
        ReflectionUtils.setField(ReflectionUtils.findField(entityClass, "id"), instance, 777L);
        return instance;
    }
}
