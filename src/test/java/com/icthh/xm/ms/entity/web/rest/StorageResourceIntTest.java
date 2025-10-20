package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.repository.backend.S3StorageRepository;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.XmeStorageServiceFacade;
import com.icthh.xm.ms.entity.service.impl.XmeStorageServiceFacadeImpl;
import com.icthh.xm.ms.entity.service.storage.StorageServiceImpl;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for the StorageResource REST controller.
 *
 * @see StorageResource
 */
@Slf4j
public class StorageResourceIntTest extends AbstractJupiterSpringBootTest {

    @Mock
    private S3StorageRepository s3StorageRepository;

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

    private AutoCloseable mocks;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @BeforeEach
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        StorageService storageService = new StorageServiceImpl(s3StorageRepository, applicationProperties);
        XmeStorageServiceFacade storageServiceFacade = new XmeStorageServiceFacadeImpl(storageService, null, null);
        StorageResource storageResource = new StorageResource(storageServiceFacade, applicationProperties);
        this.restStorageMockMvc = MockMvcBuilders.standaloneSetup(storageResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        mocks.close();
    }

    @Test
    @Transactional
    public void storeObjectSuccess() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "text/plain", "TE".getBytes());
        when(s3StorageRepository.store(Mockito.any(HttpEntity.class), eq(null))).thenReturn(file.getName());
        MvcResult result = restStorageMockMvc.perform(multipart("/api/storage/objects")
            .file(file))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk())
            .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains(file.getName());
        verify(s3StorageRepository, times(1)).store(Mockito.any(HttpEntity.class), eq(null));
    }

    @Test
    @Transactional
    public void storeImageSuccess() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "image/plain", "TE".getBytes());
        HttpEntity<Resource> httpResource = XmHttpEntityUtils.buildAvatarHttpEntity(file);
        when(s3StorageRepository.store(Mockito.any(HttpEntity.class), eq(null))).thenReturn(file.getName());
        MvcResult result = restStorageMockMvc.perform(multipart("/api/storage/objects")
            .file(file))
            .andDo(MockMvcResultHandlers.log())
            .andExpect(status().isOk())
            .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains(file.getName());
        verify(s3StorageRepository, times(1)).store(Mockito.any(HttpEntity.class), eq(null));
    }

    @Test
    @Transactional
    public void storeObjectFileTooBig() throws Exception {
        MockMultipartFile file =
            new MockMultipartFile("file", "test.txt", "text/plain", "TEST".getBytes());
        HttpEntity<Resource> storageResource = XmHttpEntityUtils.buildAvatarHttpEntity(file);
        restStorageMockMvc.perform(multipart("/api/storage/objects")
            .file(file))
            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
            .andExpect(status().isBadRequest());
        verify(s3StorageRepository, times(0)).store(Mockito.any(HttpEntity.class), eq(null));
    }

}
