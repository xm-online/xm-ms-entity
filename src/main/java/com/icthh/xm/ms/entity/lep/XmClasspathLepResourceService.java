package com.icthh.xm.ms.entity.lep;

import com.icthh.lep.api.ContextsHolder;
import com.icthh.lep.api.LepKey;
import com.icthh.lep.api.LepResource;
import com.icthh.lep.api.LepResourceDescriptor;
import com.icthh.lep.api.LepResourceKey;
import com.icthh.lep.api.LepResourceService;
import com.icthh.lep.api.LepResourceType;
import com.icthh.lep.api.Version;
import com.icthh.lep.commons.DefaultLepResourceDescriptor;
import com.icthh.lep.commons.UrlLepResourceKey;
import com.icthh.lep.script.InputStreamSupplier;
import com.icthh.lep.script.ScriptLepResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.icthh.xm.ms.entity.lep.XmLepResourceSubType.DEFAULT;

/**
 * The {@link XmClasspathLepResourceService} class.
 */
@Slf4j
public class XmClasspathLepResourceService implements LepResourceService, ResourceLoaderAware {

    private static final Pattern SCRIPT_TYPE_PATTERN = Pattern
        .compile("^.*\\Q" + XmLepConstants.SCRIPT_NAME_SEPARATOR + "\\E(.*?)\\Q"
                     + XmLepConstants.FILE_EXTENSION_GROOVY + "\\E$");

    private final ContextsHolder lepContextsHolder;
    private ResourceLoader resourceLoader;

    public XmClasspathLepResourceService(ContextsHolder lepContextsHolder) {
        this.lepContextsHolder = Objects.requireNonNull(lepContextsHolder,
                                                        "lepContextsHolder can't be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResourceExists(LepResourceKey resourceKey) {
        return getScriptResource(resourceKey).exists();
    }

    // lep:/some/group/<script_name>$<entityType>$<from_state_name>$<to_state_name>$<script_type>.groovy
    // lep:/com/icthh/lep/<script_name>$<entityType>$<state>$<script_type>.groovy

    /**
     * {@inheritDoc}
     */
    @Override
    public LepResourceDescriptor getResourceDescriptor(LepResourceKey resourceKey) {
        Objects.requireNonNull(resourceKey, "resourceKey can't be null");
        Resource scriptResource = getScriptResource(resourceKey);
        if (!scriptResource.exists()) {
            log.debug("No LEP resource for key {}", resourceKey);
            return null;
        }
        return getLepResourceDescriptor(resourceKey, scriptResource);
    }

    private LepResourceDescriptor getLepResourceDescriptor(
        LepResourceKey resourceKey,
        Resource scriptResource) {
        // get script modification time
        Instant modificationTime;
        try {
            modificationTime = Instant.ofEpochMilli(scriptResource.lastModified());
        } catch (IOException e) {
            throw new IllegalStateException(
                "Error while getting script resource modification time: "
                    + e.getMessage(),
                e);
        }

        // build descriptor
        return new DefaultLepResourceDescriptor(getResourceType(resourceKey), resourceKey,
                                                Instant.EPOCH, modificationTime);
    }

    private static LepResourceType getResourceType(LepResourceKey resourceKey) {
        String id = resourceKey.getId();
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Resource key id cant be blank");
        }

        if (id.endsWith(XmLepConstants.SCRIPT_EXTENSION_GROOVY)) {
            return XmLepResourceType.GROOVY;
        }

        throw new IllegalStateException(
            "Unsupported LEP resource script type for key: " + resourceKey.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LepResource getResource(LepResourceKey resourceKey) {
        Objects.requireNonNull(resourceKey, "resourceKey can't be null");

        log.debug("Getting LEP resource for key {}", resourceKey);

        final Resource scriptResource = getScriptResource(resourceKey);
        if (!scriptResource.exists()) {
            log.debug("No LEP resource for key {}", resourceKey);
            return null;
        }

        // build descriptor
        LepResourceDescriptor descriptor = getLepResourceDescriptor(resourceKey, scriptResource);
        log.debug("LEP resource for key {} found, descriptor: {}", resourceKey, descriptor);

        return new ScriptLepResource(descriptor, ScriptLepResource.DEFAULT_ENCODING,
                                     new InputStreamSupplier() {

                                         /**
                                          * {@inheritDoc}
                                          */
                                         @Override
                                         public InputStream getInputStream() throws IOException {
                                             return scriptResource.getInputStream();
                                         }

                                     });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LepResource saveResource(LepKey extensionKey, LepResource resource) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Version> getResourceVersions(LepResourceKey resourceKey) {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private String getResourceClassPathLocation(LepResourceKey resourceKey) {
        if (!(resourceKey instanceof UrlLepResourceKey)) {
            throw new IllegalArgumentException("Unsupported LEP resource key type: "
                                                   + resourceKey.getClass().getCanonicalName());
        }

        UrlLepResourceKey urlKey = UrlLepResourceKey.class.cast(resourceKey);
        String path = urlKey.getUrlResourcePath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // get script type ? '$default.groovy'
        Matcher matcher = SCRIPT_TYPE_PATTERN.matcher(path);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                "Can't find script type in LEP resource key: " + resourceKey);
        }
        String type = matcher.group(1);

        // is default script
        if (DEFAULT.getName().equals(type)) {
            // exclude type
            int beforeTypeIndex = path.lastIndexOf(XmLepConstants.SCRIPT_NAME_SEPARATOR);
            int scriptExtIndex = path.lastIndexOf(XmLepConstants.SCRIPT_EXTENSION_SEPARATOR);
            String pathForDefault = path.substring(0, beforeTypeIndex)
                + path.substring(scriptExtIndex);
            return "classpath:/lep/default" + pathForDefault;
        } else {
            String tenantName = LepContextUtils.getTenantName(lepContextsHolder);
            return "classpath:/lep/custom/" + tenantName.toLowerCase() + path;
        }

    }

    private Resource getScriptResource(LepResourceKey resourceKey) {
        return resourceLoader.getResource(getResourceClassPathLocation(resourceKey));
    }

}
