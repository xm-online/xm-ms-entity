package com.icthh.xm.ms.entity.web.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.service.StorageService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for the StorageResource REST controller.
 *
 * @see StorageResource
 */
public class StorageResourceIntTest extends AbstractSpringBootTest {

    @Mock
    private StorageService storageService;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    private MockMvc restStorageMockMvc;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        StorageResource storageResource = new StorageResource(storageService, applicationProperties);
        this.restStorageMockMvc = MockMvcBuilders.standaloneSetup(storageResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @After
    @Override
    public void finalize() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void storeObjectSuccess() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "text/plain", "TE".getBytes());
        restStorageMockMvc.perform(multipart("/api/storage/objects")
            .file(file))
            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
            .andExpect(status().isOk());
        verify(storageService).store(eq(file), eq(null));
    }

    @Test
    @Transactional
    public void storeImageSuccess() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "image/plain", "TE".getBytes());
        restStorageMockMvc.perform(multipart("/api/storage/objects?size=100")
            .file(file))
            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
            .andExpect(status().isOk());
        verify(storageService).store(eq(file), eq(100));
    }

    @Test
    @Transactional
    public void storeObjectFileTooBig() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "text/plain", "TEST".getBytes());
        restStorageMockMvc.perform(multipart("/api/storage/objects")
            .file(file))
            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
            .andExpect(status().isBadRequest());
        verify(storageService, times(0)).store(eq(file), eq(null));
    }

}
