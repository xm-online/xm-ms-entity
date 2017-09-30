package com.icthh.xm.ms.entity.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME;

/**
 * The {@link XmHttpEntityUtils} class.
 */
public final class XmHttpEntityUtils {

    private XmHttpEntityUtils() {
        throw new IllegalAccessError("Prevent call utils class constructor");
    }

    public static HttpEntity<Resource> buildAvatarHttpEntity(MultipartFile multipartFile) throws IOException {
        // result headers
        HttpHeaders headers = new HttpHeaders();

        // 'Content-Type' header
        String contentType = multipartFile.getContentType();
        if (StringUtils.isNotBlank(contentType)) {
            headers.setContentType(MediaType.valueOf(contentType));
        }

        // 'Content-Length' header
        long contentLength = multipartFile.getSize();
        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }

        // File name header
        String fileName = multipartFile.getOriginalFilename();
        headers.set(XM_HEADER_CONTENT_NAME, fileName);

        Resource resource = new InputStreamResource(multipartFile.getInputStream());
        return new HttpEntity<>(resource, headers);
    }

    public static HttpEntity<Resource> buildAvatarHttpEntity(HttpServletRequest request,
                                                             String fileName) throws IOException {
        // result headers
        HttpHeaders headers = new HttpHeaders();

        // 'Content-Type' header
        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        if (StringUtils.isNotBlank(contentType)) {
            headers.setContentType(MediaType.valueOf(contentType));
        }

        // 'Content-Length' header
        String headerContentLength = request.getHeader(HttpHeaders.CONTENT_LENGTH);
        long contentLength = -1L;
        if (StringUtils.isBlank(headerContentLength)) {
            try {
                contentLength = Long.parseLong(headerContentLength);
            } catch (NumberFormatException e) {
                contentLength = -1L;
            }
        }

        if (contentLength < 0) {
            contentLength = request.getContentLengthLong();
        }

        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }

        // File name header
        headers.set(XM_HEADER_CONTENT_NAME, fileName);

        Resource resource = new InputStreamResource(request.getInputStream());
        return new HttpEntity<>(resource, headers);
    }

}
