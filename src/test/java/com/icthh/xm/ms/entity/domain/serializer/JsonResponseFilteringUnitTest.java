package com.icthh.xm.ms.entity.domain.serializer;

import static com.icthh.xm.ms.entity.web.rest.XmEntityResourceExtendedIntTest.createEntityComplexIncoming;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.ms.entity.AbstractWebMvcTest;
import com.icthh.xm.ms.entity.config.JacksonConfiguration;
import com.icthh.xm.ms.entity.config.WebMvcConfiguration;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.web.rest.LinkResource;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@WebMvcTest(controllers = {XmEntityResource.class, LinkResource.class})
@ContextConfiguration(classes = {
    JacksonConfiguration.class,
    XmEntityResource.class,
    LinkResource.class,
    ExceptionTranslator.class,
    PageableHandlerMethodArgumentResolver.class
})
public class JsonResponseFilteringUnitTest extends AbstractWebMvcTest {

    private static long SEQUENCE = 0L;

    @MockBean
    private XmEntityService xmEntityService;
    @MockBean
    private ProfileService profileService;
    @MockBean
    private ProfileEventProducer profileEventProducer;
    @MockBean
    private TenantService tenantService;
    @MockBean
    private FunctionService functionService;
    @MockBean
    private LocalizationMessageService localizationMessageService;

    @MockBean
    private LinkService linkService;

    @Autowired
    private XmEntityResource xmEntityResource;

    @Autowired
    private LinkResource linkResource;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private XmSquigglyInterceptor xmSquigglyInterceptor;

    @Autowired
    private JacksonConfiguration.HttpMessageConverterCustomizer httpMessageConverterCustomizer;

    @Before
    @SneakyThrows
    public void setup() {

        httpMessageConverterCustomizer.customize(Collections.singletonList(jacksonMessageConverter));
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        this.mockMvc = MockMvcBuilders.standaloneSetup(xmEntityResource, linkResource)
                                      .setControllerAdvice(exceptionTranslator)
                                      .setMessageConverters(jacksonMessageConverter)
                                      .setCustomArgumentResolvers(pageableArgumentResolver)
                                      .addMappedInterceptors(WebMvcConfiguration.getJsonFilterAllowedURIs(),
                                                             xmSquigglyInterceptor)
                                      .build();

    }

    @SneakyThrows
    private ResultActions performGet(String url, Object... params) {
        return mockMvc.perform(get(url, params))
                      .andDo(this::printMvcResult);
    }

    private void printMvcResult(MvcResult result) throws UnsupportedEncodingException {
        log.info("MVC result: {}", result.getResponse().getContentAsString());
    }

    @Test
    @SneakyThrows
    public void testLinksFilterDefault() {

        XmEntity source = createMockResultEntity("ACCOUNT.USER");
        XmEntity target = createMockResultEntity("ACCOUNT.USER");

        addTargetLink(source, target);

        Page<Link> result = new PageImpl<>(new ArrayList<>(source.getTargets()));

        when(linkService.findAll(any(), any())).thenReturn(result);

        // Get the link
        ResultActions actions = performGet("/api/links")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        assertLinkStructure(actions, 1);

    }

    @Test
    @SneakyThrows
    public void testLinksFilteringDisabled() {

        XmEntity source = createMockResultEntity("ACCOUNT.USER");
        XmEntity target = createMockResultEntity("ACCOUNT.USER");

        addTargetLink(source, target);

        Page<Link> result = new PageImpl<>(new ArrayList<>(source.getTargets()));

        when(linkService.findAll(any(), any())).thenReturn(result);

        // Get the link
        ResultActions actions = performGet("/api/links?filter={filter}", "id,target.id")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        assertLinkStructure(actions, 1);

    }

    @Test
    @SneakyThrows
    public void testXmEntityLinkTargetFilterDefault() {

        String targetTypeKey = "ACCOUNT.USER";

        XmEntity source = createMockResultEntity("ACCOUNT.ADMIN");

        XmEntity target1 = createMockResultEntity(targetTypeKey);
        XmEntity target2 = createMockResultEntity(targetTypeKey);

        addTargetLink(source, target1);
        addTargetLink(source, target2);

        List<Link> result = new ArrayList<>(source.getTargets());

        Assert.assertNotNull(source.getId());
        Integer srcId = source.getId().intValue();

        when(xmEntityService.getLinkTargets(IdOrKey.of(source.getId()), targetTypeKey)).thenReturn(result);

        ResultActions actions = performGet("/api/xm-entities/{id}/links/targets?typeKey={typeKey}",
                                           srcId,
                                           targetTypeKey)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));

        assertLinkStructure(actions, 2);

    }

    @Test
    @SneakyThrows
    public void testXmEntityLinkTargetFilterEnabled() {

        String targetTypeKey = "ACCOUNT.USER";

        XmEntity source = createMockResultEntity("ACCOUNT.ADMIN");

        XmEntity target1 = createMockResultEntity(targetTypeKey);
        XmEntity target2 = createMockResultEntity(targetTypeKey);

        addTargetLink(source, target1);
        addTargetLink(source, target2);

        List<Link> result = new ArrayList<>(source.getTargets());

        Assert.assertNotNull(source.getId());
        Long srcId = source.getId();

        when(xmEntityService.getLinkTargets(IdOrKey.of(srcId), targetTypeKey)).thenReturn(result);

        performGet("/api/xm-entities/{id}/links/targets?typeKey={typeKey}&fields={fields}",
                   srcId, targetTypeKey, "target.tags")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].target.tags").exists())
            .andExpect(jsonPath("$.[*].target.tags.[*].id", hasSize(2)))
            .andExpect(jsonPath("$.[*].target.tags.[*].typeKey", containsInAnyOrder(two("FAVORIT"))))
            .andExpect(jsonPath("$.[*].target.tags.[*].name", hasSize(2)))
            .andExpect(jsonPath("$.[*].target.tags.[*].startDate", hasSize(2)))
            .andExpect(jsonPath("$.[*].target.tags.[*].xmEntity", hasSize(2)))

            .andExpect(jsonPath("$.[*].id").doesNotExist())
            .andExpect(jsonPath("$.[*].typeKey").doesNotExist())
            .andExpect(jsonPath("$.[*].name").doesNotExist())
            .andExpect(jsonPath("$.[*].description").doesNotExist())
            .andExpect(jsonPath("$.[*].startDate").doesNotExist())
            .andExpect(jsonPath("$.[*].endDate").doesNotExist())
            .andExpect(jsonPath("$.[*].source").doesNotExist())

            .andExpect(jsonPath("$.[*].target").exists())
            .andExpect(jsonPath("$.[*].target.id").doesNotExist())
            .andExpect(jsonPath("$.[*].target.key").doesNotExist())
            .andExpect(jsonPath("$.[*].target.typeKey").doesNotExist())
            .andExpect(jsonPath("$.[*].target.stateKey").doesNotExist())
            .andExpect(jsonPath("$.[*].target.name").doesNotExist())
            .andExpect(jsonPath("$.[*].target.startDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target.endDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target.updateDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target.description").doesNotExist())
            .andExpect(jsonPath("$.[*].target.createdBy").doesNotExist())
            .andExpect(jsonPath("$.[*].target.removed").doesNotExist())
            .andExpect(jsonPath("$.[*].target.data").doesNotExist())
            .andExpect(jsonPath("$.[*].target.data.AAAAAAAAAA").doesNotExist())

            .andExpect(jsonPath("$.[*].target.avatarUrlRelative").doesNotExist())
            .andExpect(jsonPath("$.[*].target.avatarUrlFull").doesNotExist())
            .andExpect(jsonPath("$.[*].target.version").doesNotExist())
            .andExpect(jsonPath("$.[*].target.targets").doesNotExist())
            .andExpect(jsonPath("$.[*].target.sources").doesNotExist())
            .andExpect(jsonPath("$.[*].target.attachments").doesNotExist())
            .andExpect(jsonPath("$.[*].target.locations").doesNotExist())
            .andExpect(jsonPath("$.[*].target.tags").exists())
            .andExpect(jsonPath("$.[*].target.calendars").doesNotExist())
            .andExpect(jsonPath("$.[*].target.ratings").doesNotExist())
            .andExpect(jsonPath("$.[*].target.comments").doesNotExist())
            .andExpect(jsonPath("$.[*].target.votes").doesNotExist())
            .andExpect(jsonPath("$.[*].target.functionContexts").doesNotExist())
            .andExpect(jsonPath("$.[*].target.events").doesNotExist())
            .andExpect(jsonPath("$.[*].target.uniqueFields").doesNotExist())

        ;

        performGet("/api/xm-entities/{id}/links/targets?typeKey={typeKey}&fields={fields}",
                   srcId, targetTypeKey, "target.attachments.contentUrl")
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$.[*].target").exists())
            .andExpect(jsonPath("$.[*].target.attachments").exists())
            .andExpect(jsonPath("$.[*].target.attachments.[*].contentUrl", containsInAnyOrder(two("content url"))))

            .andExpect(jsonPath("$.[*].id").doesNotExist())
            .andExpect(jsonPath("$.[*].typeKey").doesNotExist())
            .andExpect(jsonPath("$.[*].name").doesNotExist())
            .andExpect(jsonPath("$.[*].description").doesNotExist())
            .andExpect(jsonPath("$.[*].startDate").doesNotExist())
            .andExpect(jsonPath("$.[*].endDate").doesNotExist())
            .andExpect(jsonPath("$.[*].source").doesNotExist())

            .andExpect(jsonPath("$.[*].target").exists())
            .andExpect(jsonPath("$.[*].target.id").doesNotExist())
            .andExpect(jsonPath("$.[*].target.key").doesNotExist())
            .andExpect(jsonPath("$.[*].target.typeKey").doesNotExist())
            .andExpect(jsonPath("$.[*].target.stateKey").doesNotExist())
            .andExpect(jsonPath("$.[*].target.name").doesNotExist())
            .andExpect(jsonPath("$.[*].target.startDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target.endDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target.updateDate").doesNotExist())
            .andExpect(jsonPath("$.[*].target.description").doesNotExist())
            .andExpect(jsonPath("$.[*].target.createdBy").doesNotExist())
            .andExpect(jsonPath("$.[*].target.removed").doesNotExist())
            .andExpect(jsonPath("$.[*].target.data").doesNotExist())
            .andExpect(jsonPath("$.[*].target.data.AAAAAAAAAA").doesNotExist())

            .andExpect(jsonPath("$.[*].target.avatarUrlRelative").doesNotExist())
            .andExpect(jsonPath("$.[*].target.avatarUrlFull").doesNotExist())
            .andExpect(jsonPath("$.[*].target.version").doesNotExist())
            .andExpect(jsonPath("$.[*].target.targets").doesNotExist())
            .andExpect(jsonPath("$.[*].target.sources").doesNotExist())
            .andExpect(jsonPath("$.[*].target.attachments").exists())
            .andExpect(jsonPath("$.[*].target.locations").doesNotExist())
            .andExpect(jsonPath("$.[*].target.tags").doesNotExist())
            .andExpect(jsonPath("$.[*].target.calendars").doesNotExist())
            .andExpect(jsonPath("$.[*].target.ratings").doesNotExist())
            .andExpect(jsonPath("$.[*].target.comments").doesNotExist())
            .andExpect(jsonPath("$.[*].target.votes").doesNotExist())
            .andExpect(jsonPath("$.[*].target.functionContexts").doesNotExist())
            .andExpect(jsonPath("$.[*].target.events").doesNotExist())
            .andExpect(jsonPath("$.[*].target.uniqueFields").doesNotExist())

        ;
    }

    @SneakyThrows
    static ResultActions assertLinkStructure(ResultActions actions, int expectedSize) {
        return actions.andExpect(jsonPath("$", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].id", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].typeKey", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].name", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].description", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].startDate", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].endDate", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].source", hasSize(expectedSize)))

                      .andExpect(jsonPath("$.[*].target").exists())
                      .andExpect(jsonPath("$.[*].target.id", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.key", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.typeKey", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.stateKey", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.name", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.startDate", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.endDate", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.updateDate", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.description", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.createdBy", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.removed", hasSize(expectedSize)))
                      .andExpect(jsonPath("$.[*].target.data").exists())
                      .andExpect(jsonPath("$.[*].target.data.AAAAAAAAAA", hasSize(expectedSize)))

                      .andExpect(jsonPath("$.[*].target.avatarUrlRelative").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.avatarUrlFull").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.version").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.targets").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.sources").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.attachments").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.locations").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.tags").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.calendars").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.ratings").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.comments").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.votes").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.functionContexts").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.events").doesNotExist())
                      .andExpect(jsonPath("$.[*].target.uniqueFields").doesNotExist());
    }

    static XmEntity createMockResultEntity(String typeKey) {
        XmEntity entity = createEntityComplexIncoming()
            .typeKey(typeKey)
            .createdBy("admin");
        entity.setId(nextId());
        entity.getTags().stream()
              .peek(tag -> tag.setId(nextId()))
              .forEach(tag -> tag.setXmEntity(entity))
        ;
        return entity;
    }

    static void addTargetLink(XmEntity source, XmEntity target) {
        Link link = new Link()
            .typeKey("LINK")
            .name("link name")
            .startDate(Instant.now())
            .endDate(Instant.now().plusMillis(1000))
            .description("descr")
            .target(target)
            .source(source);
        link.setId(nextId());
        source.getTargets().add(link);
    }

    private static long nextId() {
        return ++SEQUENCE;
    }

    private Object[] two(Object single) {
        return new Object[]{single, single};
    }
}
