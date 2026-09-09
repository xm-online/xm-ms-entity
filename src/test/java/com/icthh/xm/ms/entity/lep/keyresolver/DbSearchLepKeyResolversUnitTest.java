package com.icthh.xm.ms.entity.lep.keyresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import org.junit.jupiter.api.Test;

public class DbSearchLepKeyResolversUnitTest extends AbstractJupiterUnitTest {

    @Test
    public void requestTypeKeyResolver() {
        LepMethod method = mock(LepMethod.class);
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setTypeKey("ORDER");
        when(method.getParameter("request", XmEntityDbSearchRequest.class)).thenReturn(request);
        assertThat(new DbSearchRequestTypeKeyResolver().segments(method)).containsExactly("ORDER");

        when(method.getParameter("request", XmEntityDbSearchRequest.class)).thenReturn(new XmEntityDbSearchRequest());
        assertThat(new DbSearchRequestTypeKeyResolver().segments(method)).isEmpty();
    }

    @Test
    public void entityAndLinkTypeKeyResolver() {
        LepMethod method = mock(LepMethod.class);
        when(method.getParameter("entityTypeKey", String.class)).thenReturn("ORDER");
        when(method.getParameter("linkTypeKey", String.class)).thenReturn("ORDER.ITEM");
        assertThat(new EntityTypeKeyAndLinkTypeKeyResolver().segments(method)).containsExactly("ORDER", "ORDER.ITEM");
        assertThat(new LinkTypeKeyParamResolver().segments(method)).containsExactly("ORDER.ITEM");
    }
}
