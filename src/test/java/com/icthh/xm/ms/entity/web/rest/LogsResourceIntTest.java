package com.icthh.xm.ms.entity.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.logging.web.rest.LogsResource;
import com.icthh.xm.commons.logging.web.rest.vm.LoggerVm;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test class for the LogsResource REST controller.
 *
 * @see LogsResource
 */
public class LogsResourceIntTest extends AbstractJupiterSpringBootTest {

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    private MockMvc restLogsMockMvc;

    @BeforeEach
    public void setup() {
        LogsResource logsResource = new LogsResource();
        this.restLogsMockMvc = MockMvcBuilders
            .standaloneSetup(logsResource)
            .setMessageConverters(jacksonMessageConverter)
            .build();
    }

    @Test
    public void getAllLogs() throws Exception {
        restLogsMockMvc
            .perform(get("/management/logs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void changeLogs() throws Exception {
        LoggerVm logger = new LoggerVm();
        logger.setLevel("INFO");
        logger.setName("ROOT");

        restLogsMockMvc
            .perform(put("/management/logs")
                         .contentType(TestUtil.APPLICATION_JSON_UTF8)
                         .content(TestUtil.convertObjectToJsonBytes(logger)))
            .andExpect(status().isNoContent());
    }

}
