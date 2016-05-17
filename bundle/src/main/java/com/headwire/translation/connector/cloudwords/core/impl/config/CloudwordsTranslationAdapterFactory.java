package com.headwire.translation.connector.cloudwords.core.impl.config;

import com.adobe.granite.translation.api.TranslationException;
import com.headwire.translation.connector.cloudwords.core.CloudwordsTranslationCloudConfig;
import com.adobe.granite.translation.core.TranslationCloudConfigUtil;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;

@Component
@Service(value = AdapterFactory.class)
public class CloudwordsTranslationAdapterFactory implements AdapterFactory {

    @Reference
    TranslationCloudConfigUtil cloudConfigUtil;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final Class<Resource> RESOURCE_CLASS = Resource.class;
    private static final Class<Node> NODE_CLASS = Node.class;
    private static final Class<CloudwordsTranslationCloudConfig> CLOUDWORDS_TRANSLATION_CLOUD_CONFIG_CLASS =
        CloudwordsTranslationCloudConfig.class;

    @Property(name = "adapters")
    protected static final String[] ADAPTER_CLASSES = {CLOUDWORDS_TRANSLATION_CLOUD_CONFIG_CLASS.getName()};

    @Property(name = "adaptables")
    protected static final String[] ADAPTABLE_CLASSES = {RESOURCE_CLASS.getName(), NODE_CLASS.getName()};

    // ---------- AdapterFactory -----------------------------------------------

    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        log.trace("In function: getAdapter(Object,Claass<AdapterType>");
        if (adaptable instanceof Resource) {
            return getAdapter((Resource) adaptable, type);
        }
        if (adaptable instanceof Node) {
            return getAdapter((Node) adaptable, type);
        }
        log.debug("Unable to handle adaptable {}", adaptable.getClass().getName());
        return null;
    }

    public void setTranslationCloudConfigUtil(TranslationCloudConfigUtil configUtil) {
        cloudConfigUtil = configUtil;
    }

    /*
     * Adapter for Resource
     */
    @SuppressWarnings("unchecked")
    private <AdapterType> AdapterType getAdapter(Resource resource, Class<AdapterType> type) {
        log.trace("In function: getAdapter(Resource,Class<AdapterType>");
        if (type == CLOUDWORDS_TRANSLATION_CLOUD_CONFIG_CLASS
                && cloudConfigUtil.isCloudConfigAppliedOnImmediateResource(resource,
                    CloudwordsTranslationCloudConfig.RESOURCE_TYPE)) {
            try {
                return (AdapterType) new CloudwordsTranslationCloudConfigImpl(resource);
            } catch (TranslationException te) {
                log.error(te.getMessage(), te);
                return null;
            }
        }
        log.debug("Unable to adapt to resource of type {}", type.getName());
        return null;

    }
}