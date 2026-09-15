package com.icthh.xm.ms.entity.web.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.ms.entity.AbstractOracleIntTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end on a real Oracle: HTTP URL string (query params for filters, full text, paging, sorting) → MockMvc →
 * resource → service → Oracle (Testcontainers) → JSON response. Same scenarios as the PostgreSQL e2e test, so a
 * dialect difference in json extraction, comparison or ordering shows up as a diverging response.
 */
@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchOracleE2eIntTest extends AbstractOracleIntTest {

    private static final String URL = "/api/_search-db/xm-entities";

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private PageableHandlerMethodArgumentResolver pageableArgumentResolver;
    @Autowired private ExceptionTranslator exceptionTranslator;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private XmEntityRepository repository;
    @Autowired private LinkRepository linkRepository;

    private MockMvc mockMvc;
    private XmEntity alpha;
    private XmEntity beta;
    private XmEntity gamma;
    private XmEntity product;

    @BeforeEach
    public void setUp() {
        pushDbSearchSpec();
        mockMvc = MockMvcBuilders.standaloneSetup(resource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
            .build();

        alpha = order("Alpha Kyiv", "Kyiv", 1, 5, "ACTIVE");
        beta = order("Beta Kyiv", "Kyiv", 2, 9, "ACTIVE");
        gamma = order("Gamma Kyiv", "Kyiv", 3, 7, "ACTIVE");
        order("Delta Kyiv", "Kyiv", 3, 1, "CLOSED");   // filtered out by stateKey
        order("Omega", "Lviv", 2, 8, "ACTIVE");        // filtered out by full text query (name and city)
        order("Zeta Kyiv", "Kyiv", 9, 9, "ACTIVE");    // filtered out by orderNo.in
        order("Tenth Kyiv", "Kyiv", 10, 10, "ACTIVE"); // filtered out by orderNo.in; lexically "10" < "2"
        product = repository.save(newEntity("PRODUCT", "Product", Map.of("sku", "P-1")));
        link(alpha, product, "ORDER.ITEM");
    }

    private XmEntity order(String name, String city, int orderNo, int position, String stateKey) {
        return repository.save(newEntity("ORDER", name,
            Map.of("orderNo", orderNo, "position", position, "customer", Map.of("city", city))).stateKey(stateKey));
    }

    private void link(XmEntity source, XmEntity target, String typeKey) {
        Link link = new Link();
        link.setTypeKey(typeKey);
        link.setSource(source);
        link.setTarget(target);
        link.setStartDate(Instant.now());
        linkRepository.save(link);
    }

    private static final String FILTERS = "typeKey=ORDER&query=kyi&data.orderNo.in=1,2,3&data.position.gte=5&stateKey.eq=ACTIVE";

    @Test
    public void getWithFiltersFullTextPagingAndJsonSort() throws Exception {
        mockMvc.perform(get(URL + "?" + FILTERS + "&sort=data.position,desc&page=0&size=2"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "3"))
            .andExpect(header().doesNotExist("Link"))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].id", contains(beta.getId().intValue(), gamma.getId().intValue())))
            .andExpect(jsonPath("$[0].data.position").value(9))
            .andExpect(jsonPath("$[1].data.position").value(7));

        mockMvc.perform(get(URL + "?" + FILTERS + "&sort=data.position,desc&page=1&size=2"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "3"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(alpha.getId()));
    }

    @Test
    public void getSortedByColumnDescThenJsonAsc() throws Exception {
        mockMvc.perform(get(URL + "?" + FILTERS + "&sort=name,desc&page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Gamma Kyiv", "Beta Kyiv", "Alpha Kyiv")));

        mockMvc.perform(get(URL + "?" + FILTERS + "&sort=stateKey,asc&sort=data.orderNo,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Alpha Kyiv", "Beta Kyiv", "Gamma Kyiv")));
    }

    @Test
    public void getNumericRangeFiltersAndSortAreNumericNotLexical() throws Exception {
        // gt=3 must return 9 and 10; a text comparison would drop "10" since "10" < "3"
        mockMvc.perform(get(URL + "?typeKey=ORDER&data.orderNo.gt=3&sort=data.orderNo,asc"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "2"))
            .andExpect(jsonPath("$[*].data.orderNo", contains(9, 10)));

        mockMvc.perform(get(URL + "?typeKey=ORDER&data.orderNo.lt=3&stateKey.eq=ACTIVE&sort=data.orderNo,desc&sort=name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Beta Kyiv", "Omega", "Alpha Kyiv")));

        mockMvc.perform(get(URL + "?typeKey=ORDER&data.position.lte=5&sort=name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Alpha Kyiv", "Delta Kyiv")));

        // whole set sorted numerically: 10 comes last, not between 1 and 2
        mockMvc.perform(get(URL + "?typeKey=ORDER&sort=data.orderNo,desc&sort=name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].data.orderNo", contains(10, 9, 3, 3, 2, 2, 1)));
    }

    @Test
    public void getWithNestedJsonFilterAndIncludeSubTypesFalse() throws Exception {
        repository.save(newEntity("ORDER.EXPRESS", "Express Kyiv", Map.of("orderNo", 1, "position", 5)).stateKey("ACTIVE"));

        mockMvc.perform(get(URL + "?typeKey=ORDER&data.customer.city.eq=Kyiv&data.orderNo.eq=1&sort=name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Alpha Kyiv")));

        mockMvc.perform(get(URL + "?typeKey=ORDER&includeSubTypes=true&data.orderNo.eq=1&sort=name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Alpha Kyiv", "Express Kyiv")));

        mockMvc.perform(get(URL + "?typeKey=ORDER&includeSubTypes=false&data.orderNo.eq=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Alpha Kyiv")));
    }

    @Test
    public void getStringJsonFilterContainsIsCaseInsensitive() throws Exception {
        mockMvc.perform(get(URL + "?typeKey=ORDER&data.customer.city.contains=LVI"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Omega")));
    }

    @Test
    public void postBodyWithTypedFilterAndSortParam() throws Exception {
        String body = """
            {"typeKey": "ORDER", "query": "kyiv",
             "filter": {"data.orderNo.in": [1, 2, 3], "data.position.gte": 5, "stateKey.eq": "ACTIVE"}}
            """;
        mockMvc.perform(post(URL + "?sort=data.orderNo,desc&size=10").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "3"))
            .andExpect(jsonPath("$[*].data.orderNo", contains(3, 2, 1)));
    }

    @Test
    public void badFilterIsBadRequest() throws Exception {
        mockMvc.perform(get(URL + "?typeKey=ORDER&name.like=a")).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "?typeKey=ORDER&sort=nope,asc")).andExpect(status().isBadRequest());
    }

    @Test
    public void linkCandidatesAndTargetsByUrl() throws Exception {
        XmEntity free = repository.save(newEntity("PRODUCT", "Free product", Map.of("sku", "P-2")));

        // unique link: the already linked product is excluded
        mockMvc.perform(get("/api/_search-db/xm-entities/ORDER/" + alpha.getId() + "/links/ORDER.ITEM?sort=name,asc"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "1"))
            .andExpect(jsonPath("$[0].id").value(free.getId()));

        // links of alpha filtered by target json field
        mockMvc.perform(get("/api/_search-db/xm-entities/" + alpha.getId() + "/targets/ORDER.ITEM?data.sku.eq=P-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].target.id").value(product.getId()));
    }
}
