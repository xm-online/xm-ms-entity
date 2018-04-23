package com.icthh.xm.ms.entity.web.rest.util;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility class for handling pagination.
 *
 * <p> Pagination uses the same principles as the <a href="https://developer.github.com/v3/#pagination">Github API</a>,
 * and follow <a href="http://tools.ietf.org/html/rfc5988">RFC 5988 (Link header)</a>.
 */
public final class PaginationUtil {

    private static final String QUERY_GET_PARAM = "&query=";
    private static final String TEMPLATE_GET_PARAM = "&template=";
    private static final String TEMPLATE_PARAMS_GET_PARAM = "&templateParams=";

    private PaginationUtil() {
    }

    public static HttpHeaders generatePaginationHttpHeaders(Page page, String baseUrl) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        String link = "";
        if ((page.getNumber() + 1) < page.getTotalPages()) {
            link = "<" + generateUri(baseUrl, page.getNumber() + 1, page.getSize()) + ">; rel=\"next\",";
        }
        // prev link
        if ((page.getNumber()) > 0) {
            link += "<" + generateUri(baseUrl, page.getNumber() - 1, page.getSize()) + ">; rel=\"prev\",";
        }
        // last and first link
        int lastPage = 0;
        if (page.getTotalPages() > 0) {
            lastPage = page.getTotalPages() - 1;
        }
        link += "<" + generateUri(baseUrl, lastPage, page.getSize()) + ">; rel=\"last\",";
        link += "<" + generateUri(baseUrl, 0, page.getSize()) + ">; rel=\"first\"";
        headers.add(HttpHeaders.LINK, link);
        return headers;
    }

    private static String generateUri(String baseUrl, int page, int size) {
        return UriComponentsBuilder.fromUriString(baseUrl).queryParam("page", page).queryParam("size", size)
            .toUriString();
    }

    @SneakyThrows
    public static HttpHeaders generateSearchPaginationHttpHeaders(String query, String template, String[] templateParams, Page page, String baseUrl) {
        String escapedQuery = URLEncoder.encode(Objects.toString(query, EMPTY), "UTF-8");
        String escapedTemplate = URLEncoder.encode(Objects.toString(template, EMPTY), "UTF-8");
        String escapedTemplateParams = URLEncoder.encode(Objects.toString(StringUtils.join(templateParams, ","), EMPTY), "UTF-8");

        String queryString = QUERY_GET_PARAM + escapedQuery
            + TEMPLATE_GET_PARAM + escapedTemplate
            + TEMPLATE_PARAMS_GET_PARAM + escapedTemplateParams;

        return generatePagination(queryString, page, baseUrl);
    }

    @SneakyThrows
    public static HttpHeaders generateSearchPaginationHttpHeaders(String query, Page page, String baseUrl) {
        String escapedQuery = URLEncoder.encode(Objects.toString(query, EMPTY), "UTF-8");
        String queryString = QUERY_GET_PARAM + escapedQuery;

        return generatePagination(queryString, page, baseUrl);
    }

    private static HttpHeaders generatePagination(String query, Page page, String baseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        String link = "";
        if ((page.getNumber() + 1) < page.getTotalPages()) {
            link = "<" + generateUri(baseUrl, page.getNumber() + 1, page.getSize()) + query
                + ">; rel=\"next\",";
        }
        // prev link
        if ((page.getNumber()) > 0) {
            link += "<" + generateUri(baseUrl, page.getNumber() - 1, page.getSize()) + query
                + ">; rel=\"prev\",";
        }
        // last and first link
        int lastPage = 0;
        if (page.getTotalPages() > 0) {
            lastPage = page.getTotalPages() - 1;
        }
        link +=
            "<" + generateUri(baseUrl, lastPage, page.getSize()) + query + ">; rel=\"last\",";
        link += "<" + generateUri(baseUrl, 0, page.getSize()) + query + ">; rel=\"first\"";
        headers.add(HttpHeaders.LINK, link);
        return headers;
    }
}
