package com.icthh.xm.ms.entity.web.rest.util;

import static org.apache.commons.lang.StringUtils.EMPTY;

import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
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
    private static final String IDS_GET_PARAM = "&ids=";
    private static final String EMBED_GET_PARAM = "&embed=";

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

    @SneakyThrows
    public static HttpHeaders generateByIdsPaginationHttpHeaders(Set<Long> ids, Set<String> embed, Page page, String baseUrl) {
        String escapedIds = URLEncoder.encode(Objects.toString(StringUtils.join(ids, ","), EMPTY), "UTF-8");
        String escapedEmbed = URLEncoder.encode(Objects.toString(StringUtils.join(embed, ","), EMPTY), "UTF-8");

        String queryString = IDS_GET_PARAM + escapedIds + EMBED_GET_PARAM + escapedEmbed;

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        String link = "";
        if ((page.getNumber() + 1) < page.getTotalPages()) {
            link = "<" + generateUri(baseUrl, page.getNumber() + 1, page.getSize()) + queryString
                + ">; rel=\"next\",";
        }
        // prev link
        if ((page.getNumber()) > 0) {
            link += "<" + generateUri(baseUrl, page.getNumber() - 1, page.getSize()) + queryString
                + ">; rel=\"prev\",";
        }
        // last and first link
        int lastPage = 0;
        if (page.getTotalPages() > 0) {
            lastPage = page.getTotalPages() - 1;
        }
        link +=
            "<" + generateUri(baseUrl, lastPage, page.getSize()) + queryString + ">; rel=\"last\",";
        link += "<" + generateUri(baseUrl, 0, page.getSize()) + queryString + ">; rel=\"first\"";
        headers.add(HttpHeaders.LINK, link);
        return headers;
    }

    private static String generateUri(String baseUrl, int page, int size) {
        return UriComponentsBuilder.fromUriString(baseUrl).queryParam("page", page).queryParam("size", size)
            .toUriString();
    }

    @SneakyThrows
    public static HttpHeaders generateSearchPaginationHttpHeaders(String query, Page page, String baseUrl) {
        String escapedQuery = URLEncoder.encode(query, "UTF-8");

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        String link = "";
        if ((page.getNumber() + 1) < page.getTotalPages()) {
            link = "<" + generateUri(baseUrl, page.getNumber() + 1, page.getSize()) + QUERY_GET_PARAM + escapedQuery
                + ">; rel=\"next\",";
        }
        // prev link
        if ((page.getNumber()) > 0) {
            link += "<" + generateUri(baseUrl, page.getNumber() - 1, page.getSize()) + QUERY_GET_PARAM + escapedQuery
                + ">; rel=\"prev\",";
        }
        // last and first link
        int lastPage = 0;
        if (page.getTotalPages() > 0) {
            lastPage = page.getTotalPages() - 1;
        }
        link +=
            "<" + generateUri(baseUrl, lastPage, page.getSize()) + QUERY_GET_PARAM + escapedQuery + ">; rel=\"last\",";
        link += "<" + generateUri(baseUrl, 0, page.getSize()) + QUERY_GET_PARAM + escapedQuery + ">; rel=\"first\"";
        headers.add(HttpHeaders.LINK, link);
        return headers;
    }
}
