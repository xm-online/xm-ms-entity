package com.icthh.xm.ms.entity.web.client;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

public class MultipartMixedConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private Charset defaultCharset = DEFAULT_CHARSET;

    private List<MediaType> supportedMediaTypes = new ArrayList<>();

    private List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

    private MediaType mixed = new MediaType("multipart", "mixed");
    private MediaType related = new MediaType("multipart", "related");


    public MultipartMixedConverter() {
        this.supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
        this.supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);
        this.supportedMediaTypes.add(mixed);
        this.supportedMediaTypes.add(related);

        this.partConverters.add(new ByteArrayHttpMessageConverter());
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
        stringHttpMessageConverter.setWriteAcceptCharset(false);
        this.partConverters.add(stringHttpMessageConverter);
        this.partConverters.add(new ResourceHttpMessageConverter());
        this.partConverters.add(new MappingJackson2HttpMessageConverter());
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(this.supportedMediaTypes);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        if (!MultiValueMap.class.isAssignableFrom(clazz)) {
            return false;
        }
        if (mediaType == null) {
            return true;
        }
        for (MediaType supportedMediaType : getSupportedMediaTypes()) {
            // We can't read multipart....
            if (!supportedMediaType.equals(MediaType.MULTIPART_FORM_DATA) && supportedMediaType.includes(mediaType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if (!MultiValueMap.class.isAssignableFrom(clazz)) {
            return false;
        }
        if (mediaType == null || MediaType.ALL.equals(mediaType)) {
            return true;
        }
        for (MediaType supportedMediaType : getSupportedMediaTypes()) {
            if (supportedMediaType.isCompatibleWith(mediaType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MultiValueMap<String, String> read(Class<? extends MultiValueMap<String, ?>> clazz,
        HttpInputMessage inputMessage) throws IOException {

        MediaType contentType = inputMessage.getHeaders().getContentType();
        Charset charset = contentType.getCharset() != null ? contentType.getCharset() : this.defaultCharset;
        String body = StreamUtils.copyToString(inputMessage.getBody(), charset);

        String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx == -1) {
                result.add(URLDecoder.decode(pair, charset.name()), null);
            } else {
                String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
                String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
                result.add(name, value);
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(MultiValueMap<String, ?> map, MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException {

        if (!isMultipart(map, contentType)) {
            writeForm((MultiValueMap<String, String>) map, contentType, outputMessage);
        } else {
            writeMultipart((MultiValueMap<String, Object>) map, outputMessage);
        }
    }

    private boolean isMultipart(MultiValueMap<String, ?> map, MediaType contentType) {
        if (contentType != null) {
            return MediaType.MULTIPART_FORM_DATA.includes(contentType)
                || mixed.includes(contentType)
                || related.includes(contentType);
        }
        for (Map.Entry<String, ?> entiry : map.entrySet()) {
            for (Object value : map.get(entiry.getKey())) {
                if (value != null && !(value instanceof String)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void writeForm(MultiValueMap<String, String> form, MediaType contentType,
        HttpOutputMessage outputMessage) throws IOException {

        Charset charset;
        if (contentType != null) {
            outputMessage.getHeaders().setContentType(contentType);
            charset = contentType.getCharset() != null ? contentType.getCharset() : this.defaultCharset;
        } else {
            outputMessage.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            charset = this.defaultCharset;
        }
        StringBuilder builder = new StringBuilder();
        buildByNames(form, charset, builder);
        final byte[] bytes = builder.toString().getBytes(charset.name());
        outputMessage.getHeaders().setContentLength(bytes.length);

        if (outputMessage instanceof StreamingHttpOutputMessage) {
            StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
            streamingOutputMessage.setBody(outputStream -> StreamUtils.copy(bytes, outputStream));
        } else {
            StreamUtils.copy(bytes, outputMessage.getBody());
        }
    }

    private static void buildByNames(MultiValueMap<String, String> form, Charset charset, StringBuilder builder)
        throws UnsupportedEncodingException {
        for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext(); ) {
            String name = nameIterator.next();
            buildByValues(form, charset, builder, name);
            if (nameIterator.hasNext()) {
                builder.append('&');
            }
        }
    }

    private static void buildByValues(MultiValueMap<String, String> form, Charset charset, StringBuilder builder,
        String name) throws UnsupportedEncodingException {
        for (Iterator<String> valueIterator = form.get(name).iterator(); valueIterator.hasNext(); ) {
            String value = valueIterator.next();
            builder.append(URLEncoder.encode(name, charset.name()));
            if (value != null) {
                builder.append('=');
                builder.append(URLEncoder.encode(value, charset.name()));
                if (valueIterator.hasNext()) {
                    builder.append('&');
                }
            }
        }
    }

    private void writeMultipart(final MultiValueMap<String, Object> parts, HttpOutputMessage outputMessage)
        throws IOException {
        final byte[] boundary = generateMultipartBoundary();
        Map<String, String> parameters = Collections.singletonMap("boundary", new String(boundary, "US-ASCII"));

        MediaType contentType = new MediaType(mixed, parameters);
        HttpHeaders headers = outputMessage.getHeaders();
        headers.setContentType(contentType);

        if (outputMessage instanceof StreamingHttpOutputMessage) {
            StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
            streamingOutputMessage.setBody(outputStream -> {
                writeParts(outputStream, parts, boundary);
                writeEnd(outputStream, boundary);
            });
        } else {
            writeParts(outputMessage.getBody(), parts, boundary);
            writeEnd(outputMessage.getBody(), boundary);
        }
    }

    private void writeParts(OutputStream os, MultiValueMap<String, Object> parts, byte[] boundary) throws IOException {
        for (Map.Entry<String, List<Object>> entry : parts.entrySet()) {
            String name = entry.getKey();
            for (Object part : entry.getValue()) {
                if (part != null) {
                    writeBoundary(os, boundary);
                    writePart(name, getHttpEntity(part), os);
                    writeNewLine(os);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writePart(String name, HttpEntity<?> partEntity, OutputStream os) throws IOException {
        Object partBody = partEntity.getBody();
        Class<?> partType = partBody.getClass();
        HttpHeaders partHeaders = partEntity.getHeaders();
        MediaType partContentType = partHeaders.getContentType();
        for (HttpMessageConverter<?> messageConverter : this.partConverters) {
            if (messageConverter.canWrite(partType, partContentType)) {
                HttpOutputMessage multipartMessage = new MultipartHttpOutputMessage(os);
                multipartMessage.getHeaders().setContentDispositionFormData(name, null);
                if (!partHeaders.isEmpty()) {
                    multipartMessage.getHeaders().putAll(partHeaders);
                }
                ((HttpMessageConverter<Object>) messageConverter).write(partBody, partContentType, multipartMessage);
                return;
            }
        }
        throw new HttpMessageNotWritableException(
            "Could not write request: no suitable HttpMessageConverter found for request type [" + partType.getName()
                + "]");
    }

    /**
     * Generate a multipart boundary.
     *
     * <p>This implementation delegates to {@link MimeTypeUtils#generateMultipartBoundary()}.
     */
    private static byte[] generateMultipartBoundary() {
        return MimeTypeUtils.generateMultipartBoundary();
    }

    /**
     * Return an {@link HttpEntity} for the given part Object.
     *
     * @param part the part to return an {@link HttpEntity} for
     * @return the part Object itself it is an {@link HttpEntity}, or a newly built {@link HttpEntity} wrapper for that
     * part
     */
    private static HttpEntity<?> getHttpEntity(Object part) {
        if (part instanceof HttpEntity) {
            return (HttpEntity<?>) part;
        } else {
            return new HttpEntity<>(part);
        }
    }

    private static void writeBoundary(OutputStream os, byte[] boundary) throws IOException {
        os.write('-');
        os.write('-');
        os.write(boundary);
        writeNewLine(os);
    }

    private static void writeEnd(OutputStream os, byte[] boundary) throws IOException {
        os.write('-');
        os.write('-');
        os.write(boundary);
        os.write('-');
        os.write('-');
        writeNewLine(os);
    }

    private static void writeNewLine(OutputStream os) throws IOException {
        os.write('\r');
        os.write('\n');
    }


    /**
     * Implementation of {@link org.springframework.http.HttpOutputMessage} used to write a MIME multipart.
     */
    private static class MultipartHttpOutputMessage implements HttpOutputMessage {

        private final OutputStream outputStream;

        private final HttpHeaders headers = new HttpHeaders();

        private boolean headersWritten = false;

        MultipartHttpOutputMessage(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers;
        }

        @Override
        public OutputStream getBody() throws IOException {
            writeHeaders();
            return this.outputStream;
        }

        private void writeHeaders() throws IOException {
            if (!this.headersWritten) {
                for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
                    byte[] headerName = getAsciiBytes(entry.getKey());
                    for (String headerValueString : entry.getValue()) {
                        byte[] headerValue = getAsciiBytes(headerValueString);
                        this.outputStream.write(headerName);
                        this.outputStream.write(':');
                        this.outputStream.write(' ');
                        this.outputStream.write(headerValue);
                        writeNewLine(this.outputStream);
                    }
                }
                writeNewLine(this.outputStream);
                this.headersWritten = true;
            }
        }

        private static byte[] getAsciiBytes(String name) {
            try {
                return name.getBytes("US-ASCII");
            } catch (UnsupportedEncodingException ex) {
                // Should not happen - US-ASCII is always supported.
                throw new IllegalStateException(ex);
            }
        }
    }
}
