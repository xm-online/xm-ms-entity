package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import java.util.List;
import org.springframework.stereotype.Component;

/** LEP key from {@code request.typeKey} of a DB search request. */
@Component
public class DbSearchRequestTypeKeyResolver implements LepKeyResolver {

    @Override
    public List<String> segments(LepMethod method) {
        XmEntityDbSearchRequest request = method.getParameter("request", XmEntityDbSearchRequest.class);
        return request != null && request.getTypeKey() != null ? List.of(request.getTypeKey()) : List.of();
    }
}
