package com.icthh.xm.ms.entity.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME;

/**
 * The {@link XmHttpEntityUtils} class.
 */
@UtilityClass
public final class XmHttpEntityUtils {

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

    public static String getFileName(HttpHeaders xmImageHeaders) {
        MediaType contentType;
        String fileName = null;
        List<String> fileNames = xmImageHeaders.get(XM_HEADER_CONTENT_NAME);
        if (!CollectionUtils.isEmpty(fileNames)) {
            String value = fileNames.iterator().next();
            if (StringUtils.isNoneBlank(value)) {
                fileName = value;
            }
        }

        // generate file name
        if (fileName == null) {
            fileName = "file";

            // try to get file extension
            contentType = xmImageHeaders.getContentType();
            if (contentType != null && !contentType.isWildcardSubtype()) {
                fileName += "." + contentType.getSubtype().toLowerCase();
            }
        }
        return fileName;
    }

}
