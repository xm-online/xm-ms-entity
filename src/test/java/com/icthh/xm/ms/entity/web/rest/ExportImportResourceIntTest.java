package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.util.stream.Collectors.toSet;
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
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.CalendarService;
import com.icthh.xm.ms.entity.service.ExportImportService;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.dto.ExportDto;
import com.icthh.xm.ms.entity.service.dto.ExportDto.CalendarDto;
import com.icthh.xm.ms.entity.service.dto.ImportDto;
import com.icthh.xm.ms.entity.service.dto.LinkExportDto;
import java.util.List;
import java.util.Set;
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

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private XmEntityService xmEntityService;

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
    public void testSuccessExportEntities() {
        XmEntity source = new XmEntity();
        source.setTypeKey("TEST_EXPORT_SOURCE");
        source.setKey("TEST_EXPORT_SOURCE_KEY");
        source.setName("TEST_EXPORT_SOURCE_NAME");

        source = xmEntityService.save(source);

        XmEntity target = new XmEntity();
        target.setTypeKey("TEST_EXPORT_TARGET");
        target.setKey("TEST_EXPORT_TARGET_KEY");
        target.setName("TEST_EXPORT_TARGET_NAME");
        target = xmEntityService.save(target);

        Event event = new Event();
        event.setTypeKey("B");
        event.setTitle("title");
        Calendar calendar = new Calendar();
        calendar.setTypeKey("A");
        calendar.setEvents(Set.of(event));
        calendar.setXmEntity(source);
        calendar.setName("name");
        calendar = calendarService.save(calendar);
        event.setCalendar(calendar);

        Content content = new Content();
        content.setValue("content".getBytes());
        Attachment attachment = new Attachment();
        attachment.setTypeKey("C");

        attachment.setContent(content);
        attachment.setName("name");
        attachment.setXmEntity(source);
        attachment = attachmentService.save(attachment);

        Link link = new Link();
        link.setTarget(target);
        link.setSource(source);
        link.setTypeKey("EXPORT_LINK");
        link = linkService.save(link);

        ExportDto exportDto = new ExportDto();
        exportDto.setTypeKey("TEST_EXPORT_SOURCE");
        exportDto.setLinkTypeKeys(List.of("TEST_EXPORT_TARGET"));
        exportDto.setAttachmentTypeKeys(List.of("C"));

        CalendarDto calendarDto = new CalendarDto();
        calendarDto.setEvents(List.of("B"));
        calendarDto.setTypeKey("A");
        exportDto.setCalendars(List.of(calendarDto));

        MvcResult result = restExportImportMockMvc.perform(post("/api/export/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(Set.of(exportDto))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();
        byte[] response = result.getResponse().getContentAsByteArray();
        ImportDto actual = TestUtil.convertJsonBytesToObject(response, ImportDto.class);

        assertThat(actual.getEntities().size()).isEqualTo(2);
        assertThat(actual.getEntities().contains(source)).isEqualTo(true);
        assertThat(actual.getEntities().contains(target)).isEqualTo(true);
        assertThat(actual.getLinks().size()).isEqualTo(1);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getSourceId).collect(toSet())
                .contains(link.getSource().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTargetId).collect(toSet())
                .contains(link.getTarget().getId())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getStartDate).collect(toSet())
                .contains(link.getStartDate())).isEqualTo(true);
        assertThat(actual.getLinks().stream().map(LinkExportDto::getTypeKey).collect(toSet())
                .contains(link.getTypeKey())).isEqualTo(true);


        assertThat(actual.getCalendars().size()).isEqualTo(1);
        // assertThat(actual.getCalendars().contains(new CalendarExportDto(calendar))).isEqualTo(true);
        assertThat(actual.getEvents().size()).isEqualTo(1);
        //   assertThat(actual.getEvents().contains(new EventExportDto(event))).isEqualTo(true);
        assertThat(actual.getAttachments().size()).isEqualTo(1);
        //     assertThat(actual.getAttachments().contains(new AttachmentExportDto(attachment))).isEqualTo(true);
        assertThat(actual.getContents().size()).isEqualTo(1);
        //     assertThat(actual.getContents().contains(content)).isEqualTo(true);
    }


}
