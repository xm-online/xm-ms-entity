package com.icthh.xm.ms.entity.service.api;

import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.annotation.Tenant;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.domain.XmFunction;
import com.icthh.xm.ms.entity.service.XmFunctionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * The {@link XmFunctionServiceResolver} class.
 */
@Component
public class XmFunctionServiceResolver implements XmFunctionService {

    private final XmFunctionService defaultService;

    public XmFunctionServiceResolver(@Qualifier("functionService") XmFunctionService defaultService) {
        this.defaultService = defaultService;
    }

    XmFunctionService getService() {
        String tenant = TenantContext.getCurrent().getTenant();
        return defaultService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmFunction save(XmFunction xmFunction) {
        return getService().save(xmFunction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<XmFunction> findAll() {
        return getService().findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmFunction findOne(Long id) {
        return getService().findOne(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Long id) {
        getService().delete(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<XmFunction> search(String query) {
        return getService().search(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmFunction execute(String functionKey, Map<String, Object> context) {
        return getService().execute(functionKey, context);
    }

}
