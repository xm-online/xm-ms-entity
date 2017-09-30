package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.errors.ExceptionTranslator;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.repository.backend.StorageRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for the StorageResource REST controller.
 *
 * @see StorageResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class StorageResourceIntTest {

    @Mock
    private StorageRepository storageRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    private MockMvc restStorageMockMvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        StorageResource storageResource = new StorageResource(storageRepository, applicationProperties);
        this.restStorageMockMvc = MockMvcBuilders.standaloneSetup(storageResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Test
    @Transactional
    public void storeObjectSuccess() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "text/plain", "TE".getBytes());
        restStorageMockMvc.perform(fileUpload("/api/storage/objects")
            .file(file))
            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
            .andExpect(status().isOk());
        verify(storageRepository).store(eq(file), eq(null));
    }

    @Test
    @Transactional
    public void storeImageSuccess() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "image/plain", "TE".getBytes());
        restStorageMockMvc.perform(fileUpload("/api/storage/objects?size=100")
            .file(file))
            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
            .andExpect(status().isOk());
        verify(storageRepository).store(eq(file), eq(100));
    }

    @Test
    @Transactional
    public void storeObjectFileTooBig() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "text/plain", "TEST".getBytes());
        restStorageMockMvc.perform(fileUpload("/api/storage/objects")
            .file(file))
            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
            .andExpect(status().isBadRequest());
        verify(storageRepository, times(0)).store(eq(file), eq(null));
    }

}
