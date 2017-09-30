package com.icthh.xm.ms.entity.web.client;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class MutipartMixedConverterUnitTest {

    private MultipartMixedConverter multipartMixedConverter;

    @Mock
    private HttpInputMessage httpInputMessage;
    @Mock
    private HttpOutputMessage httpOutputMessage;
    @Mock
    private HttpHeaders httpHeaders;
    @Mock
    private OutputStream outputStream;
    @Mock
    private InputStream inputStream;

    @Before
    public void before() throws IOException {
        MockitoAnnotations.initMocks(this);
        multipartMixedConverter = new MultipartMixedConverter();

        when(httpHeaders.getContentType()).thenReturn(MediaType.ALL);
        when(httpOutputMessage.getHeaders()).thenReturn(httpHeaders);
        when(httpOutputMessage.getBody()).thenReturn(outputStream);
    }

    @Test
    public void testCanReadTrue() {
        boolean result = multipartMixedConverter
            .canRead(LinkedMultiValueMap.class, MediaType.APPLICATION_FORM_URLENCODED);

        assertTrue(result);
    }

    @Test
    public void testCanReadFalse() {
        boolean result = multipartMixedConverter.canRead(LinkedMultiValueMap.class, MediaType.MULTIPART_FORM_DATA);

        assertFalse(result);
    }

    @Test
    public void testCanWrite() {
        boolean result = multipartMixedConverter.canWrite(LinkedMultiValueMap.class, MediaType.MULTIPART_FORM_DATA);

        assertTrue(result);
    }

    @Test
    public void testRead() throws IOException {
        when(httpInputMessage.getHeaders()).thenReturn(httpHeaders);
        when(httpInputMessage.getBody()).thenReturn(IOUtils.toInputStream("text=value", Charsets.UTF_8));

        MultiValueMap<String, String> result = multipartMixedConverter.read(null, httpInputMessage);

        assertEquals("value", result.getFirst("text"));
    }

    @Test
    public void testWrite() throws IOException {
        multipartMixedConverter.write(new LinkedMultiValueMap<String, String>(), null, httpOutputMessage);
    }

    @Test
    public void testWriteForm() throws IOException {
        multipartMixedConverter.write(new LinkedMultiValueMap<String, String>(), MediaType.ALL, httpOutputMessage);
    }

    @Test
    public void testWriteMultipart() throws IOException {
        LinkedMultiValueMap<String, String> parts = new LinkedMultiValueMap<>();
        parts.add("first", "value");
        parts.add("second", "value");
        multipartMixedConverter.write(parts, MediaType.MULTIPART_FORM_DATA, httpOutputMessage);
    }
}
