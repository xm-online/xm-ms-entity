package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.web.rest.TestUtil.convertJsonBytesToObject;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
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
import com.icthh.xm.ms.entity.repository.ContentRepository;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.repository.RatingRepository;
import com.icthh.xm.ms.entity.repository.TagRepository;
import com.icthh.xm.ms.entity.repository.VoteRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.EventService;
import com.icthh.xm.ms.entity.service.ExportImportService;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.LocationService;
import com.icthh.xm.ms.entity.service.RatingService;
import com.icthh.xm.ms.entity.service.TagService;
import com.icthh.xm.ms.entity.service.VoteService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.dto.AttachmentExportDto;
import com.icthh.xm.ms.entity.service.dto.CalendarExportDto;
import com.icthh.xm.ms.entity.service.dto.CommentExportDto;
import com.icthh.xm.ms.entity.service.dto.EventExportDto;
import com.icthh.xm.ms.entity.service.dto.ImportDto;
import com.icthh.xm.ms.entity.service.dto.LinkExportDto;
import com.icthh.xm.ms.entity.service.dto.LocationExportDto;
import com.icthh.xm.ms.entity.service.dto.RatingExportDto;
import com.icthh.xm.ms.entity.service.dto.TagsExportDto;
import com.icthh.xm.ms.entity.service.dto.VoteExportDto;
import java.time.Instant;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for the ExportImport REST controller.
 *
 * @see com.icthh.xm.ms.entity.web.rest.ExportImportResource
 */
@Slf4j
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class ExportImportResourceIntTest extends AbstractSpringBootTest {

    private static final String TEST_EXPORT_1 = "TEST_EXPORT_1";
    private static final String TEST_EXPORT_2 = "TEST_EXPORT_2";
    private static final String TEST_EXPORT_3 = "TEST_EXPORT_3";
    private static final String TEST_EXPORT_LINK_1 = "TEST_EXPORT_LINK_1";
    private static final String TEST_EXPORT_LINK_2 = "TEST_EXPORT_LINK_2";
    private static final String TEST_EXPORT_LINK_3 = "TEST_EXPORT_LINK_3";
    private static final String TEST_EXPORT_LINK_4 = "TEST_EXPORT_LINK_4";
    private static final String TEST_EXPORT_EVENT_1 = "TEST_EXPORT_EVENT_1";
    private static final String TEST_EXPORT_EVENT_2 = "TEST_EXPORT_EVENT_2";
    private static final String TEST_EXPORT_EVENT_3 = "TEST_EXPORT_EVENT_3";
    private static final String TEST_EXPORT_CALENDAR_1 = "TEST_EXPORT_CALENDAR_1";
    private static final String TEST_EXPORT_CALENDAR_2 = "TEST_EXPORT_CALENDAR_2";
    private static final String TEST_EXPORT_ATTACHMENT_1 = "TEST_EXPORT_ATTACHMENT_1";
    private static final String TEST_EXPORT_ATTACHMENT_2 = "TEST_EXPORT_ATTACHMENT_2";
    private static final String TEST_EXPORT_TAG_1 = "TEST_EXPORT_TAG_1";
    private static final String TEST_EXPORT_TAG_2 = "TEST_EXPORT_TAG_2";
    private static final String TEST_EXPORT_LOCATION_1 = "TEST_EXPORT_LOCATION_1";
    private static final String TEST_EXPORT_LOCATION_2 = "TEST_EXPORT_LOCATION_2";
    private static final String TEST_EXPORT_RATING_1 = "TEST_EXPORT_RATING_1";
    private static final String TEST_EXPORT_RATING_2 = "TEST_EXPORT_RATING_2";

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;
    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;
    @Autowired
    private ExceptionTranslator exceptionTranslator;
    @Autowired
    private XmEntityService entityService;
    @Autowired
    private XmEntityRepositoryInternal entityRepositoryInternal;
    @Autowired
    private LinkService linkService;
    @Autowired
    private EventService eventService;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private XmAuthenticationContextHolder authContextHolder;
    @Autowired
    private LepManager lepManager;
    @Autowired
    private ExportImportService exportImportService;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private CalendarService calendarService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TagService tagService;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CalendarRepository calendarRepository;
    @Autowired
    private RatingService ratingService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private VoteService voteService;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private LinkRepository linkRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private VoteRepository voteRepository;
    @Autowired
    private ContentRepository contentRepository;
    private MockMvc restExportImportMockMvc;


    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
        MockitoAnnotations.initMocks(this);
        ExportImportResource exportImportResourceTest = new ExportImportResource(exportImportService);
        this.restExportImportMockMvc = MockMvcBuilders.standaloneSetup(exportImportResourceTest)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setControllerAdvice(exceptionTranslator)
                .setMessageConverters(jacksonMessageConverter, new ByteArrayHttpMessageConverter()).build();
    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testExportEntities() {
        XmEntity testExportEntity11 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_1");
        XmEntity testExportEntity12 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_2");
        XmEntity testExportEntity21 = createEntity(TEST_EXPORT_2, TEST_EXPORT_2 + "_1");
        XmEntity testExportEntity31 = createEntity(TEST_EXPORT_3, TEST_EXPORT_3 + "_1");

        ImportDto actual = sendExportRequest("importexport/exportSomeEntities.json");

        assertThat(actual.getEntities().size()).isEqualTo(2);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity12)).isEqualTo(true);

        actual = sendExportRequest("importexport/exportEntities.json");

        assertThat(actual.getEntities().size()).isEqualTo(4);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity12)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity21)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity31)).isEqualTo(true);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testExportLinks() {
        XmEntity testExportEntity11 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_1");
        XmEntity testExportEntity21 = createEntity(TEST_EXPORT_2, TEST_EXPORT_2 + "_1");
        XmEntity testExportEntity22 = createEntity(TEST_EXPORT_2, TEST_EXPORT_2 + "_2");
        XmEntity testExportEntity31 = createEntity(TEST_EXPORT_3, TEST_EXPORT_3 + "_1");
        XmEntity testExportEntity32 = createEntity(TEST_EXPORT_3, TEST_EXPORT_3 + "_2");

        Link testExportLink1 = createLink(testExportEntity11, testExportEntity21, TEST_EXPORT_LINK_1);
        Link testExportLink2 = createLink(testExportEntity11, testExportEntity22, TEST_EXPORT_LINK_2);
        Link testExportLink3 = createLink(testExportEntity11, testExportEntity31, TEST_EXPORT_LINK_3);
        Link testExportLink4 = createLink(testExportEntity21, testExportEntity31, TEST_EXPORT_LINK_4);
        Link testExportLink5 = createLink(testExportEntity21, testExportEntity32, TEST_EXPORT_LINK_4);

        ImportDto actual = sendExportRequest("importexport/exportLinks.json");

        //entities
        assertThat(actual.getEntities().size()).isEqualTo(5);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity21)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity22)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity31)).isEqualTo(true);

        //links
        assertThat(actual.getLinks().size()).isEqualTo(5);
        assertLink(testExportLink1, actual);
        assertLink(testExportLink2, actual);
        assertLink(testExportLink3, actual);
        assertLink(testExportLink4, actual);
        assertLink(testExportLink5, actual);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testExportSomeLinks() {
        XmEntity testExportEntity11 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_1");
        XmEntity testExportEntity21 = createEntity(TEST_EXPORT_2, TEST_EXPORT_2 + "_1");
        XmEntity testExportEntity22 = createEntity(TEST_EXPORT_2, TEST_EXPORT_2 + "_2");
        XmEntity testExportEntity31 = createEntity(TEST_EXPORT_3, TEST_EXPORT_3 + "_1");

        Link testExportLink1 = createLink(testExportEntity11, testExportEntity21, TEST_EXPORT_LINK_1);
        createLink(testExportEntity11, testExportEntity22, TEST_EXPORT_LINK_2);
        createLink(testExportEntity11, testExportEntity31, TEST_EXPORT_LINK_3);

        ImportDto actual = sendExportRequest("importexport/exportSomeLinks.json");

        assertThat(actual.getEntities().size()).isEqualTo(2);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity21)).isEqualTo(true);

        assertThat(actual.getLinks().size()).isEqualTo(1);
        assertLink(testExportLink1, actual);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testExportAttachment() {
        XmEntity testExportEntity11 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_1");

        Content testExportContent1 = new Content();
        testExportContent1.setValue(TEST_EXPORT_ATTACHMENT_1.getBytes());
        Attachment testExportAttachment1 = createAttachment(TEST_EXPORT_ATTACHMENT_1,
                testExportContent1, testExportEntity11);
        testExportAttachment1.setContent(testExportContent1);
        testExportAttachment1 = attachmentService.save(testExportAttachment1);

        Content testExportContent2 = new Content();
        testExportContent2.setValue(TEST_EXPORT_ATTACHMENT_2.getBytes());
        Attachment testExportAttachment2 = createAttachment(TEST_EXPORT_ATTACHMENT_2,
                testExportContent2, testExportEntity11);
        testExportAttachment2.setContent(testExportContent2);
        testExportAttachment2 = attachmentService.save(testExportAttachment2);

        ImportDto actual = sendExportRequest("importexport/exportAttachments.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);

        assertThat(actual.getAttachments().size()).isEqualTo(2);
        assertAttachment(testExportAttachment1, actual);
        assertAttachment(testExportAttachment2, actual);

        assertThat(actual.getContents().size()).isEqualTo(2);
        assertThat(actual.getContents().contains(testExportContent1)).isEqualTo(true);
        assertThat(actual.getContents().contains(testExportContent2)).isEqualTo(true);

        actual = sendExportRequest("importexport/exportSomeAttachments.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getAttachments().size()).isEqualTo(1);
        assertAttachment(testExportAttachment1, actual);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testExportCalendars() {
        XmEntity testExportEntity11 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_1");

        Calendar testExportCalendar1 = createCalendar(testExportEntity11, TEST_EXPORT_CALENDAR_1);
        Event testExportEvent1 = createEvent(testExportCalendar1, TEST_EXPORT_EVENT_1);
        Event testExportEvent2 = createEvent(testExportCalendar1, TEST_EXPORT_EVENT_2);
        Set<Event> events = new HashSet<>(Set.of(testExportEvent1, testExportEvent2));
        testExportCalendar1.setEvents(events);
        testExportCalendar1 = calendarService.save(testExportCalendar1);

        Calendar testExportCalendar2 = createCalendar(testExportEntity11, TEST_EXPORT_CALENDAR_2);
        Event testExportEvent3 = createEvent(testExportCalendar2, TEST_EXPORT_EVENT_3);
        events = new HashSet<>(Set.of(testExportEvent3));
        testExportCalendar2.setEvents(events);
        testExportCalendar2 = calendarService.save(testExportCalendar2);

        ImportDto actual = sendExportRequest("importexport/exportCalendars.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);

        assertThat(actual.getCalendars().size()).isEqualTo(2);
        assertCalendar(testExportCalendar1, actual);
        assertCalendar(testExportCalendar2, actual);

        assertThat(actual.getEvents().size()).isEqualTo(3);
        assertEvent(testExportEvent1, actual);
        assertEvent(testExportEvent2, actual);
        assertEvent(testExportEvent3, actual);

        actual = sendExportRequest("importexport/exportSomeCalendars.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getCalendars().size()).isEqualTo(1);
        assertCalendar(testExportCalendar1, actual);

        assertThat(actual.getEvents().size()).isEqualTo(1);
        assertEvent(testExportEvent1, actual);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testExportComment() {
        XmEntity testExportEntity11 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_1");
        Comment comment1 = createComment(testExportEntity11);
        Comment comment2 = createComment(testExportEntity11);
        ImportDto actual = sendExportRequest("importexport/exportComments.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getComments().size()).isEqualTo(2);

        assertComment(comment1, actual);
        assertComment(comment2, actual);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testExportTags() {
        XmEntity testExportEntity11 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_1");
        Tag testExportTag1 = createTag(testExportEntity11, TEST_EXPORT_TAG_1);
        Tag testExportTag2 = createTag(testExportEntity11, TEST_EXPORT_TAG_2);

        ImportDto actual = sendExportRequest("importexport/exportTags.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getTags().size()).isEqualTo(2);

        assertTag(testExportTag1, actual);
        assertTag(testExportTag2, actual);

        actual = sendExportRequest("importexport/exportSomeTags.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getTags().size()).isEqualTo(1);
        assertTag(testExportTag1, actual);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testExportLocations() {
        XmEntity testExportEntity11 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_1");
        Location testExportLocation1 = createLocation(testExportEntity11, TEST_EXPORT_LOCATION_1);
        Location testExportLocation2 = createLocation(testExportEntity11, TEST_EXPORT_LOCATION_2);

        ImportDto actual = sendExportRequest("importexport/exportLocations.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getLocations().size()).isEqualTo(2);

        assertLocation(testExportLocation1, actual);
        assertLocation(testExportLocation2, actual);

        actual = sendExportRequest("importexport/exportSomeLocations.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getLocations().size()).isEqualTo(1);
        assertLocation(testExportLocation1, actual);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testExportRating() {
        XmEntity testExportEntity11 = createEntity(TEST_EXPORT_1, TEST_EXPORT_1 + "_1");

        Rating testExportRating1 = createRating(testExportEntity11, TEST_EXPORT_RATING_1);
        Rating testExportRating2 = createRating(testExportEntity11, TEST_EXPORT_RATING_2);

        Vote testExportVote1 = createVote(testExportRating1, testExportEntity11, "Message");
        Vote testExportVote2 = createVote(testExportRating1, testExportEntity11, "Message2");
        testExportRating1.addVotes(testExportVote1);
        testExportRating1.addVotes(testExportVote2);
        testExportRating1 = ratingService.save(testExportRating1);

        Vote testExportVote3 = createVote(testExportRating2, testExportEntity11, "Message3");
        testExportRating2.addVotes(testExportVote3);
        testExportRating2 = ratingService.save(testExportRating2);

        ImportDto actual = sendExportRequest("importexport/exportRating.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);

        assertThat(actual.getRatings().size()).isEqualTo(2);
        assertRating(testExportRating1, actual);
        assertRating(testExportRating2, actual);

        assertThat(actual.getVotes().size()).isEqualTo(3);
        assertVote(testExportVote1, actual);
        assertVote(testExportVote2, actual);
        assertVote(testExportVote3, actual);

        actual = sendExportRequest("importexport/exportSomeRating.json");

        assertThat(actual.getEntities().size()).isEqualTo(1);
        assertThat(actual.getRatings().size()).isEqualTo(1);
        assertRating(testExportRating1, actual);

        assertThat(actual.getVotes().size()).isEqualTo(2);
        assertVote(testExportVote1, actual);
        assertVote(testExportVote2, actual);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testImportData() {

        entityRepositoryInternal.deleteAll();
        attachmentRepository.deleteAll();
        linkRepository.deleteAll();
        tagRepository.deleteAll();
        locationRepository.deleteAll();
        ratingRepository.deleteAll();
        voteRepository.deleteAll();
        calendarRepository.deleteAll();
        eventRepository.deleteAll();
        contentRepository.deleteAll();

        restExportImportMockMvc.perform(post("/api/import/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(getExportImportDto("importexport/import.json")));

        assertThat(entityRepositoryInternal.count()).isEqualTo(2);
        assertThat(attachmentRepository.count()).isEqualTo(1);
        assertThat(linkRepository.count()).isEqualTo(1);
        assertThat(tagRepository.count()).isEqualTo(1);
        assertThat(locationRepository.count()).isEqualTo(1);
        assertThat(ratingRepository.count()).isEqualTo(1);
        assertThat(voteRepository.count()).isEqualTo(1);
        assertThat(calendarRepository.count()).isEqualTo(1);
        assertThat(eventRepository.count()).isEqualTo(1);
        assertThat(contentRepository.count()).isEqualTo(1);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void testImportDataWithWrongLink() {
        entityRepositoryInternal.deleteAll();
        linkRepository.deleteAll();

        restExportImportMockMvc.perform(post("/api/import/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(getExportImportDto("importexport/importWithWrongLink.json")));

        assertThat(entityRepositoryInternal.count()).isEqualTo(2);
        assertThat(linkRepository.count()).isEqualTo(0);
    }

    private void assertAttachment(Attachment attachment, ImportDto actual) {
        assertThat(actual.getAttachments().stream().map(AttachmentExportDto::getEntityId).collect(toSet())
                .contains(attachment.getXmEntity().getId())).isEqualTo(true);
        assertThat(actual.getAttachments().stream().map(AttachmentExportDto::getContentChecksum).collect(toSet())
                .contains(attachment.getContentChecksum())).isEqualTo(true);
        assertThat(actual.getAttachments().stream().map(AttachmentExportDto::getContentId).collect(toSet())
                .contains(attachment.getContent().getId())).isEqualTo(true);
        assertThat(actual.getAttachments().stream().map(AttachmentExportDto::getTypeKey).collect(toSet())
                .contains(attachment.getTypeKey())).isEqualTo(true);
        assertThat(actual.getAttachments().stream().map(AttachmentExportDto::getValueContentSize).collect(toSet())
                .contains(attachment.getValueContentSize())).isEqualTo(true);
    }

    private void assertLink(Link link, ImportDto actual) {
        assertThat(actual.getLinks().stream().map(LinkExportDto::getSourceId).collect(toSet())
                .contains(link.getSource().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTargetId).collect(toSet())
                .contains(link.getTarget().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getStartDate).collect(toSet())
                .contains(link.getStartDate())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTypeKey).collect(toSet())
                .contains(link.getTypeKey())).isEqualTo(true);
    }

    private Tag createTag(XmEntity entity, String typeKey) {
        Tag testExportTag1 = new Tag();
        testExportTag1.setXmEntity(entity);
        testExportTag1.setName(typeKey);
        testExportTag1.setTypeKey(typeKey);
        testExportTag1 = tagService.save(testExportTag1);
        return testExportTag1;
    }

    private Location createLocation(XmEntity entity, String typeKey) {
        Location location = new Location();
        location.setCity(typeKey);
        location.setCountryKey(typeKey);
        location.setRegion(typeKey);
        location.setAddressLine1(typeKey);
        location.setAddressLine2(typeKey);
        location.setXmEntity(entity);
        location.setName(typeKey);
        location.setTypeKey(typeKey);
        location = locationService.save(location);
        return location;
    }

    private void assertTag(Tag tag, ImportDto actual) {
        assertThat(actual.getTags().stream().map(TagsExportDto::getEntityId).collect(toSet())
                .contains(tag.getXmEntity().getId())).isEqualTo(true);
        assertThat(actual.getTags().stream().map(TagsExportDto::getId).collect(toSet())
                .contains(tag.getId())).isEqualTo(true);
        assertThat(actual.getTags().stream().map(TagsExportDto::getName).collect(toSet())
                .contains(tag.getName())).isEqualTo(true);
        assertThat(actual.getTags().stream().map(TagsExportDto::getTypeKey).collect(toSet())
                .contains(tag.getTypeKey())).isEqualTo(true);
    }

    private void assertLocation(Location location, ImportDto actual) {
        assertThat(actual.getLocations().stream().map(LocationExportDto::getEntityId).collect(toSet())
                .contains(location.getXmEntity().getId())).isEqualTo(true);
        assertThat(actual.getLocations().stream().map(LocationExportDto::getId).collect(toSet())
                .contains(location.getId())).isEqualTo(true);
        assertThat(actual.getLocations().stream().map(LocationExportDto::getName).collect(toSet())
                .contains(location.getName())).isEqualTo(true);
        assertThat(actual.getLocations().stream().map(LocationExportDto::getTypeKey).collect(toSet())
                .contains(location.getTypeKey())).isEqualTo(true);
        assertThat(actual.getLocations().stream().map(LocationExportDto::getCity).collect(toSet())
                .contains(location.getCity())).isEqualTo(true);
        assertThat(actual.getLocations().stream().map(LocationExportDto::getCountryKey).collect(toSet())
                .contains(location.getCountryKey())).isEqualTo(true);
        assertThat(actual.getLocations().stream().map(LocationExportDto::getRegion).collect(toSet())
                .contains(location.getRegion())).isEqualTo(true);
    }

    private void assertComment(Comment comment, ImportDto actual) {
        assertThat(actual.getComments().stream().map(CommentExportDto::getEntityId).collect(toSet())
                .contains(comment.getXmEntity().getId())).isEqualTo(true);
        assertThat(actual.getComments().stream().map(CommentExportDto::getId).collect(toSet())
                .contains(comment.getId())).isEqualTo(true);
        assertThat(actual.getComments().stream().map(CommentExportDto::getMessage).collect(toSet())
                .contains(comment.getMessage())).isEqualTo(true);
        assertThat(actual.getComments().stream().map(CommentExportDto::getUserKey).collect(toSet())
                .contains(comment.getUserKey())).isEqualTo(true);
    }

    @NotNull
    private Comment createComment(XmEntity entity) {
        Comment comment1 = new Comment();
        comment1.setXmEntity(entity);
        comment1.message("TEST MESSAGE" + "_1");
        comment1.setUserKey("TEST-USER-KEY");
        comment1.setEntryDate(Instant.now());
        comment1 = commentRepository.save(comment1);
        return comment1;
    }


    private Event createEvent(Calendar calendar, String typeKey) {
        Event testExportEvent2 = new Event();
        testExportEvent2.setTypeKey(typeKey);
        testExportEvent2.setTitle(typeKey);
        testExportEvent2.setCalendar(calendar);
        testExportEvent2 = eventService.save(testExportEvent2);
        return testExportEvent2;
    }

    private Calendar createCalendar(XmEntity entity, String typeKey) {
        Calendar testExportCalendar1 = new Calendar();
        testExportCalendar1.setTypeKey(typeKey);
        testExportCalendar1.setName(typeKey);
        testExportCalendar1.setXmEntity(entity);
        testExportCalendar1 = calendarService.save(testExportCalendar1);
        return testExportCalendar1;
    }

    private Rating createRating(XmEntity entity, String typeKey) {
        Rating rating = new Rating();
        rating.setTypeKey(typeKey);
        rating.setXmEntity(entity);
        rating.setTypeKey(typeKey);
        rating.setXmEntity(entity);
        rating = ratingService.save(rating);
        return rating;
    }

    private Vote createVote(Rating rating, XmEntity entity, String message) {
        Vote vote = new Vote();
        vote.setUserKey(message);
        vote.setXmEntity(entity);
        vote.setRating(rating);
        vote.setMessage(message);
        vote.setValue(new Random().nextDouble());
        vote = voteService.save(vote);
        return vote;
    }

    private void assertEvent(Event event, ImportDto actual) {
        assertThat(actual.getEvents().stream().map(EventExportDto::getId).collect(toSet())
                .contains(event.getId())).isEqualTo(true);
        assertThat(actual.getEvents().stream().map(EventExportDto::getTitle).collect(toSet())
                .contains(event.getTitle())).isEqualTo(true);
        assertThat(actual.getEvents().stream().map(EventExportDto::getTypeKey).collect(toSet())
                .contains(event.getTypeKey())).isEqualTo(true);
        assertThat(actual.getEvents().stream().map(EventExportDto::getCalendarId).collect(toSet())
                .contains(event.getCalendar().getId())).isEqualTo(true);
    }

    private void assertCalendar(Calendar calendar, ImportDto actual) {
        assertThat(actual.getCalendars().stream().map(CalendarExportDto::getId).collect(toSet())
                .contains(calendar.getId())).isEqualTo(true);
        assertThat(actual.getCalendars().stream().map(CalendarExportDto::getTypeKey).collect(toSet())
                .contains(calendar.getTypeKey())).isEqualTo(true);
        assertThat(actual.getCalendars().stream().map(CalendarExportDto::getName).collect(toSet())
                .contains(calendar.getName())).isEqualTo(true);
        assertThat(actual.getCalendars().stream().map(CalendarExportDto::getEntityId).collect(toSet())
                .contains(calendar.getXmEntity().getId())).isEqualTo(true);
    }

    private void assertRating(Rating rating, ImportDto actual) {
        assertThat(actual.getRatings().stream().map(RatingExportDto::getId).collect(toSet())
                .contains(rating.getId())).isEqualTo(true);
        assertThat(actual.getRatings().stream().map(RatingExportDto::getTypeKey).collect(toSet())
                .contains(rating.getTypeKey())).isEqualTo(true);
        assertThat(actual.getRatings().stream().map(RatingExportDto::getEntityId).collect(toSet())
                .contains(rating.getXmEntity().getId())).isEqualTo(true);
        assertThat(actual.getRatings().stream().map(RatingExportDto::getValue).collect(toSet())
                .contains(rating.getValue())).isEqualTo(true);
    }

    private void assertVote(Vote vote, ImportDto actual) {
        assertThat(actual.getVotes().stream().map(VoteExportDto::getId).collect(toSet())
                .contains(vote.getId())).isEqualTo(true);
        assertThat(actual.getVotes().stream().map(VoteExportDto::getRatingId).collect(toSet())
                .contains(vote.getRating().getId())).isEqualTo(true);
        assertThat(actual.getVotes().stream().map(VoteExportDto::getEntityId).collect(toSet())
                .contains(vote.getXmEntity().getId())).isEqualTo(true);
    }

    private ImportDto sendExportRequest(String contentPath) throws Exception {
        MvcResult result = restExportImportMockMvc.perform(post("/api/export/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(getExportImportDto(contentPath)))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();
        byte[] response = result.getResponse().getContentAsByteArray();
        return convertJsonBytesToObject(response, ImportDto.class);
    }

    private XmEntity createEntity(String typeKey, String name) {
        XmEntity xmEntity = new XmEntity();
        xmEntity.setTypeKey(typeKey);
        xmEntity.setName(name);
        xmEntity.setKey(name);
        return entityService.save(xmEntity);
    }

    private Link createLink(XmEntity source, XmEntity target, String typeKey) {
        Link link = new Link();
        link.setTypeKey(typeKey);
        link.setSource(source);
        link.setTarget(target);
        return linkService.save(link);
    }

    private Attachment createAttachment(String typeKey, Content content, XmEntity entity) {
        Attachment attachment = new Attachment();
        attachment.setTypeKey(typeKey);
        attachment.setName(typeKey);
        attachment.setContent(content);
        attachment.setXmEntity(entity);
        return attachment;
    }

    @SneakyThrows
    private byte[] getExportImportDto(String filename) {
        return toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(filename)));
    }

}
