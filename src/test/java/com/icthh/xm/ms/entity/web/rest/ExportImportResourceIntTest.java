package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.web.rest.TestUtil.convertJsonBytesToObject;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.ExportImportService;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.dto.ImportDto;
import com.icthh.xm.ms.entity.service.dto.LinkExportDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
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


    public static final String TEST_EXPORT_1 = "TEST_EXPORT_1";
    public static final String TEST_EXPORT_2 = "TEST_EXPORT_2";
    public static final String TEST_EXPORT_3 = "TEST_EXPORT_3";
    public static final String TEST_EXPORT_LINK_1 = "TEST_EXPORT_LINK_1";
    public static final String TEST_EXPORT_LINK_2 = "TEST_EXPORT_LINK_2";
    public static final String TEST_EXPORT_LINK_3 = "TEST_EXPORT_LINK_3";
    public static final String TEST_EXPORT_LINK_4 = "TEST_EXPORT_LINK_4";
    public static final String TEST_EXPORT_EVENT_1 = "TEST_EXPORT_EVENT_1";
    public static final String TEST_EXPORT_EVENT_2 = "TEST_EXPORT_EVENT_2";
    public static final String TEST_EXPORT_EVENT_3 = "TEST_EXPORT_EVENT_3";
    public static final String TEST_EXPORT_CALENDAR_1 = "TEST_EXPORT_CALENDAR_1";
    public static final String TEST_EXPORT_CALENDAR_2 = "TEST_EXPORT_CALENDAR_2";
    public static final String TEST_EXPORT_ATTACHMENT_1 = "TEST_EXPORT_ATTACHMENT_1";
    public static final String TEST_EXPORT_ATTACHMENT_2 = "TEST_EXPORT_ATTACHMENT_2";
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

        MvcResult result = restExportImportMockMvc.perform(post("/api/export/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(getExportImportDto("importexport/exportSomeEntities.json")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();
        byte[] response = result.getResponse().getContentAsByteArray();
        ImportDto actual = convertJsonBytesToObject(response, ImportDto.class);

        assertThat(actual.getEntities().size()).isEqualTo(2);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity12)).isEqualTo(true);

        result = restExportImportMockMvc.perform(post("/api/export/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(getExportImportDto("importexport/exportEntities.json")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();
        response = result.getResponse().getContentAsByteArray();
        actual = convertJsonBytesToObject(response, ImportDto.class);

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

        MvcResult result = restExportImportMockMvc.perform(post("/api/export/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(getExportImportDto("importexport/exportLinks.json")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();
        byte[] response = result.getResponse().getContentAsByteArray();
        ImportDto actual = convertJsonBytesToObject(response, ImportDto.class);

        //entities
        assertThat(actual.getEntities().size()).isEqualTo(5);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity21)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity22)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity31)).isEqualTo(true);

        //links
        assertThat(actual.getLinks().size()).isEqualTo(5);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getSourceId).collect(toSet())
                .contains(testExportLink1.getSource().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTargetId).collect(toSet())
                .contains(testExportLink1.getTarget().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getStartDate).collect(toSet())
                .contains(testExportLink1.getStartDate())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTypeKey).collect(toSet())
                .contains(testExportLink1.getTypeKey())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getSourceId).collect(toSet())
                .contains(testExportLink2.getSource().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTargetId).collect(toSet())
                .contains(testExportLink2.getTarget().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getStartDate).collect(toSet())
                .contains(testExportLink2.getStartDate())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTypeKey).collect(toSet())
                .contains(testExportLink2.getTypeKey())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getSourceId).collect(toSet())
                .contains(testExportLink3.getSource().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTargetId).collect(toSet())
                .contains(testExportLink3.getTarget().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getStartDate).collect(toSet())
                .contains(testExportLink3.getStartDate())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTypeKey).collect(toSet())
                .contains(testExportLink3.getTypeKey())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getSourceId).collect(toSet())
                .contains(testExportLink4.getSource().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTargetId).collect(toSet())
                .contains(testExportLink4.getTarget().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getStartDate).collect(toSet())
                .contains(testExportLink4.getStartDate())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTypeKey).collect(toSet())
                .contains(testExportLink4.getTypeKey())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getSourceId).collect(toSet())
                .contains(testExportLink5.getSource().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTargetId).collect(toSet())
                .contains(testExportLink5.getTarget().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getStartDate).collect(toSet())
                .contains(testExportLink5.getStartDate())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTypeKey).collect(toSet())
                .contains(testExportLink5.getTypeKey())).isEqualTo(true);
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

        MvcResult result = restExportImportMockMvc.perform(post("/api/export/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(getExportImportDto("importexport/exportSomeLinks.json")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();
        byte[] response = result.getResponse().getContentAsByteArray();
        ImportDto actual = convertJsonBytesToObject(response, ImportDto.class);

        //entities
        assertThat(actual.getEntities().size()).isEqualTo(2);
        assertThat(actual.getEntities().contains(testExportEntity11)).isEqualTo(true);
        assertThat(actual.getEntities().contains(testExportEntity21)).isEqualTo(true);

        //links
        assertThat(actual.getLinks().size()).isEqualTo(1);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getSourceId).collect(toSet())
                .contains(testExportLink1.getSource().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTargetId).collect(toSet())
                .contains(testExportLink1.getTarget().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getStartDate).collect(toSet())
                .contains(testExportLink1.getStartDate())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTypeKey).collect(toSet())
                .contains(testExportLink1.getTypeKey())).isEqualTo(true);

    }

    @SneakyThrows
    @Test
    @Transactional
    public void testImportData() {
        restExportImportMockMvc.perform(post("/api/import/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(getExportImportDto("importexport/importLinks.json")))
                .andDo(MockMvcResultHandlers.print());

        long count = entityRepositoryInternal.count();
        log.info("<<<<<<<<<<< count {}", count);
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

    @SneakyThrows
    private byte[] getExportImportDto(String filename) {
        return toByteArray(requireNonNull(getClass().getClassLoader().getResourceAsStream(filename)));
    }

    private void saveExportData() {

//
//        Link testExportLink4 = new Link();
//        testExportLink4.setTypeKey(TEST_EXPORT_LINK_4);
//
//        Event testExportEvent1 = new Event();
//        testExportEvent1.setTypeKey(TEST_EXPORT_EVENT_1);
//
//        Event testExportEvent2 = new Event();
//        testExportEvent2.setTypeKey(TEST_EXPORT_EVENT_2);
//
//        Event testExportEvent3 = new Event();
//        testExportEvent3.setTypeKey(TEST_EXPORT_EVENT_3);
//
//        Calendar testExportCalendar1 = new Calendar();
//        testExportCalendar1.setTypeKey(TEST_EXPORT_CALENDAR_1);
//
//        Calendar testExportCalendar2 = new Calendar();
//        testExportCalendar2.setTypeKey(TEST_EXPORT_CALENDAR_2);
//
//        Attachment testExportAttachment1 = new Attachment();
//        testExportAttachment1.setTypeKey(TEST_EXPORT_ATTACHMENT_1);
//        Content testExportContent1 = new Content();
//        testExportContent1.setValue(TEST_EXPORT_ATTACHMENT_1.getBytes());
//        testExportAttachment1.setContent(testExportContent1);
//
//        Attachment testExportAttachment2 = new Attachment();
//        testExportAttachment2.setTypeKey(TEST_EXPORT_ATTACHMENT_2);
//        Content testExportContent2 = new Content();
//        testExportContent2.setValue(TEST_EXPORT_ATTACHMENT_2.getBytes());
//        testExportAttachment2.setContent(testExportContent2);
    }


}

//        assertThat(actual.getCalendars().size()).isEqualTo(1);
//        // assertThat(actual.getCalendars().contains(new CalendarExportDto(calendar))).isEqualTo(true);
//        assertThat(actual.getEvents().size()).isEqualTo(1);
//        //   assertThat(actual.getEvents().contains(new EventExportDto(event))).isEqualTo(true);
//        assertThat(actual.getAttachments().size()).isEqualTo(1);
//        //     assertThat(actual.getAttachments().contains(new AttachmentExportDto(attachment))).isEqualTo(true);
//        assertThat(actual.getContents().size()).isEqualTo(1);
//        //     assertThat(actual.getContents().contains(content)).isEqualTo(true);