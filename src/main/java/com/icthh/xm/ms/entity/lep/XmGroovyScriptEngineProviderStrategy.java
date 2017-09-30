package com.icthh.xm.ms.entity.lep;

import com.icthh.lep.groovy.LazyGroovyScriptEngineProviderStrategy;
import com.icthh.lep.groovy.ScriptNameLepResourceKeyMapper;
import org.springframework.beans.factory.BeanClassLoaderAware;

/**
 * The {@link XmGroovyScriptEngineProviderStrategy} class.
 */
public class XmGroovyScriptEngineProviderStrategy extends LazyGroovyScriptEngineProviderStrategy
    implements BeanClassLoaderAware {

    private ClassLoader springClassLoader;

    public XmGroovyScriptEngineProviderStrategy(ScriptNameLepResourceKeyMapper resourceKeyMapper) {
        super(resourceKeyMapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ClassLoader getParentClassLoader() {
        return springClassLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.springClassLoader = classLoader;
    }

}
