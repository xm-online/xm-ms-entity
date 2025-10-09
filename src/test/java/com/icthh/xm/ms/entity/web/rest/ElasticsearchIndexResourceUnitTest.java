package com.icthh.xm.ms.entity.web.rest;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.ms.entity.AbstractJupiterWebMvcTest;
import com.icthh.xm.ms.entity.service.ElasticsearchIndexService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ElasticsearchIndexResource.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes={ElasticsearchIndexResource.class})
public class ElasticsearchIndexResourceUnitTest extends AbstractJupiterWebMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ElasticsearchIndexService service;

    @SneakyThrows
    @Test
    public void testReindexCallsService() {
        mockMvc.perform(post("/api/elasticsearch/index"))
            .andExpect(status().isAccepted());

        verify(service).reindexAllAsync();
    }
}
