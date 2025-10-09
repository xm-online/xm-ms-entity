package com.icthh.xm.ms.entity.domain.idresolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.ms.entity.AbstractJupiterWebMvcTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.web.rest.LinkResource;
import com.icthh.xm.ms.entity.web.rest.TestUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@WebMvcTest(controllers = LinkResource.class)
@ContextConfiguration(classes = {LinkResource.class, ExceptionTranslator.class})
public class XmEntityObjectIdResolverUnitTest extends AbstractJupiterWebMvcTest {

    private static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";

    @MockBean
    private LocalizationMessageService localizationMessageService;

    @MockBean
    private XmEntityRepository entityRepository;

    @MockBean
    private LinkService linkService;

    @MockBean
    private CalendarRepository calendarRepository;

    @Autowired
    private LinkResource linkResource;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(linkResource)
                                      .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @SneakyThrows
    @Test
    public void shouldCreatePrototypeEveryTime() {
        XmEntity source = createRef(1L);
        XmEntity target = createRef(2L);

        when(entityRepository.findById(1L)).thenReturn(Optional.ofNullable(source));
        when(entityRepository.findById(2L)).thenReturn(Optional.ofNullable(target));

        Link link = new Link().typeKey(DEFAULT_TYPE_KEY)
            .startDate(Instant.now())
            .source(source)
            .target(target);

        Link any = any();
        Link savedLink = new Link().typeKey(DEFAULT_TYPE_KEY)
            .startDate(Instant.now())
            .source(source)
            .target(target);
        savedLink.setId(1L);
        when(linkService.save(any)).thenReturn(savedLink);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/links")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(link)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value(1L))
                .andExpect(jsonPath("$.target.id").value(2L));
        }
        verify(entityRepository, times(2)).findById(1L);
    }

    private XmEntity createRef(Long id) {
        XmEntity entity = new XmEntity();
        entity.setId(id);
        return entity;
    }
}
