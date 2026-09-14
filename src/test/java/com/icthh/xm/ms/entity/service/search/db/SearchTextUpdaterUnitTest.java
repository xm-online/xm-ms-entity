package com.icthh.xm.ms.entity.service.search.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecUpdatedEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class SearchTextUpdaterUnitTest extends AbstractJupiterUnitTest {

    private final XmEntitySpecService xmEntitySpecService = mock(XmEntitySpecService.class);
    private final SearchTextUpdater updater = new SearchTextUpdater(xmEntitySpecService);

    private static XmEntity entity(Map<String, Object> data) {
        return new XmEntity().typeKey("T").name("Alpha order").description("Big <b>one</b>").data(new HashMap<>(data));
    }

    /** The spec of the entity type drives the text, so the tests set it and read what refresh stored. */
    private String searchText(TypeSpec spec, XmEntity entity) {
        when(xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(any())).thenReturn(Optional.ofNullable(spec));
        updater.refresh(entity);
        return entity.getSearchText();
    }

    @Test
    public void returnsNullWhenFlagOffOrSpecMissing() {
        assertThat(searchText(null, entity(Map.of()))).isNull();
        assertThat(searchText(TypeSpec.builder().key("T").build(), entity(Map.of()))).isNull();
        assertThat(searchText(TypeSpec.builder().key("T").fullTextSearch(false).build(), entity(Map.of()))).isNull();
    }

    @Test
    public void joinsNameDescriptionAndConfiguredDataFields() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true)
            .fullTextSearchDataFields(List.of("data.orderNo", "customer.city", "data.tags", "data.customer", "data.missing"))
            .build();
        XmEntity e = entity(Map.of(
            "orderNo", 42,
            "customer", Map.of("city", "Kyiv"),
            "tags", List.of("vip", "urgent")));

        // lists and objects are rendered with String.valueOf; missing paths are skipped
        assertThat(searchText(spec, e)).isEqualTo("Alpha order\nBig <b>one</b>\n42\nKyiv\n[vip, urgent]\n{city=Kyiv}");
    }

    @Test
    public void dataFieldsAreSpelExpressions() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true)
            .fullTextSearchDataFields(List.of(
                "data.lines[0].sku",          // index into a list of objects
                "data.customer?.city",        // safe navigation: customer missing → skipped
                "data.total * 2"))            // arithmetic
            .build();
        XmEntity e = entity(Map.of(
            "lines", List.of(Map.of("sku", "SKU-1"), Map.of("sku", "SKU-2")),
            "total", 21,
            "tags", List.of("a", "b")));

        assertThat(searchText(spec, e)).isEqualTo("Alpha order\nBig <b>one</b>\nSKU-1\n42");
    }

    @Test
    public void nullAndBlankDataFieldEntriesAreSkipped() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true)
            .fullTextSearchDataFields(java.util.Arrays.asList(null, " ", "data.orderNo"))
            .build();
        assertThat(searchText(spec, entity(Map.of("orderNo", 7)))).isEqualTo("Alpha order\nBig <b>one</b>\n7");
    }

    @Test
    public void specUpdateParsesTheConfiguredExpressions() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true)
            .fullTextSearchDataFields(List.of("data.orderNo", "customer.city"))
            .build();

        updater.onSpecUpdated(new XmEntitySpecUpdatedEvent("TEST", Map.of("T", spec)));

        assertThat(searchText(spec, entity(Map.of("orderNo", 42, "customer", Map.of("city", "Kyiv")))))
            .isEqualTo("Alpha order\nBig <b>one</b>\n42\nKyiv");
    }

    @Test
    public void malformedExpressionIsReportedOnSpecUpdateAndSkippedOnSave() {
        assertThatThrownBy(() -> new SpelExpressionParser().parseExpression("data.("))
            .isInstanceOf(ParseException.class);

        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true)
            .fullTextSearchDataFields(List.of("data.orderNo", "data.(", "customer.city"))
            .build();

        // a broken entry in tenant config must not break the refresh
        updater.onSpecUpdated(new XmEntitySpecUpdatedEvent("TEST", Map.of("T", spec)));

        // nor the save: the other fields are still indexed
        assertThat(searchText(spec, entity(Map.of("orderNo", 42, "customer", Map.of("city", "Kyiv")))))
            .isEqualTo("Alpha order\nBig <b>one</b>\n42\nKyiv");
    }

    @Test
    public void nameAndDescriptionOnlyWhenNoDataFields() {
        TypeSpec spec = TypeSpec.builder().key("T").fullTextSearch(true).build();
        assertThat(searchText(spec, entity(Map.of("x", 1)))).isEqualTo("Alpha order\nBig <b>one</b>");
        assertThat(searchText(spec, new XmEntity().typeKey("T").name("only"))).isEqualTo("only");
    }
}
