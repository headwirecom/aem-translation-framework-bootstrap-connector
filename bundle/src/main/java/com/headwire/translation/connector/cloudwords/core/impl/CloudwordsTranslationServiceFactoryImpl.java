package com.headwire.translation.connector.cloudwords.core.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.adobe.granite.translation.api.TranslationConfig;
import com.adobe.granite.translation.api.TranslationConstants.TranslationMethod;
import com.adobe.granite.translation.api.TranslationException;
import com.adobe.granite.translation.api.TranslationService;
import com.adobe.granite.translation.api.TranslationServiceFactory;
import com.adobe.granite.translation.core.TranslationCloudConfigUtil;
import com.adobe.granite.translation.core.common.AbstractTranslationServiceFactory;
//import com.adobe.granite.translation.bootstrap.tms.core.BootstrapTmsService;

import com.headwire.pageUploader.services.impl.PageUploaderImpl;
import com.headwire.translation.connector.cloudwords.core.CloudwordsTranslationCloudConfig;

@Service
@Component(label = "Cloudwords Translation Connector Factory", metatype = true, immediate = true)
@Properties(value = {
	    @Property(name = "service.description", value = "Cloudwords translation factory service"),
	    @Property(name=CloudwordsConstants.PREVIEW_ENABLED, label="Enable Preview", boolValue=false, description="Preview Enabled for Translation Objects"),
	    @Property(name = TranslationServiceFactory.PROPERTY_TRANSLATION_FACTORY, value = "cloudwords",
	            label = "Cloudwords Translation Factory Name", description = "Cloudwords"
	                    + "Translation Factory Connector"),
	    @Property(name=CloudwordsConstants.EXPORT_FORMAT_FIELD, label="Export Format",value="xml",description="Please specify the format for exporting translation jobs",options={
	    		@PropertyOption(name = CloudwordsConstants.EXPORT_FORMAT_XML, value = "XML"),
	    		@PropertyOption(name = CloudwordsConstants.EXPORT_FORMAT_XLIFF_1_2, value = "XLIFF 1.2"),
	    		@PropertyOption(name = CloudwordsConstants.EXPORT_FORMAT_XLIFF_2_0, value = "XLIFF 2.0")
	    })
	})
public class CloudwordsTranslationServiceFactoryImpl extends AbstractTranslationServiceFactory implements
    TranslationServiceFactory {
	
	protected Boolean isPreviewEnabled;
    
    protected String exportFormat;
    
    protected ResourceResolver rr;

    @Reference
    TranslationCloudConfigUtil cloudConfigUtil;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference 
    protected TranslationConfig translationConfig;
    
    @Reference 
    CryptoSupport cryptoSupport;
    
    @Reference
    CloudwordsTranslationCacheImpl cloudwordsTranslationCache;
    
    @Reference
    PageUploaderImpl pageUploaderImpl;
    
    @Reference
    private SlingRepository repository;

    //@Reference
    //BootstrapTmsService bootstrapTmsService;
        
    private List<TranslationMethod> supportedTranslationMethods;

    public CloudwordsTranslationServiceFactoryImpl() {
        supportedTranslationMethods = new ArrayList<TranslationMethod>();
        supportedTranslationMethods.add(TranslationMethod.HUMAN_TRANSLATION);
    } 

    private static final Logger log = LoggerFactory.getLogger(CloudwordsTranslationServiceFactoryImpl.class);

    @Override
    public TranslationService createTranslationService(TranslationMethod translationMethod, String cloudConfigPath)
        throws TranslationException {
        log.error("LQ == In function: getTranslationService()");
        log.trace(" "+translationMethod);
        log.trace(" "+cloudConfigPath);
        
        CloudwordsTranslationCloudConfig cloudwordsCloudConfg =
            (CloudwordsTranslationCloudConfig) cloudConfigUtil.getCloudConfigObjectFromPath(
                CloudwordsTranslationCloudConfig.class, cloudConfigPath);
        
        String strServiceLabel = "";
        String strServiceAttribute = "";
        String previewPath = "";
        
        if (cloudwordsCloudConfg != null) {
            strServiceLabel = cloudwordsCloudConfg.getServiceLabel();
            strServiceAttribute = cloudwordsCloudConfg.getServiceAttribution();
            previewPath = cloudwordsCloudConfg.getPreviewPath();
            cloudwordsCloudConfg.decryptSecret(cryptoSupport);
        }
        if(!rr.isLive()){
        	log.error("LQ == rr is not live");
        	rr.close();
        	getResourceResolver(resourceResolverFactory);
        }
        return new CloudwordsTranslationServiceImpl(null, null, factoryName, strServiceLabel, strServiceAttribute, previewPath, isPreviewEnabled, exportFormat, cloudConfigPath, cloudwordsCloudConfg, translationConfig, cloudwordsTranslationCache, pageUploaderImpl, rr);
    }

    @Override
    public List<TranslationMethod> getSupportedTranslationMethods() {
        return supportedTranslationMethods;
    }

    @Override
    public Class<?> getServiceCloudConfigClass() {
        return CloudwordsTranslationCloudConfig.class;
    }
    
    protected void bindTranslationConfig(TranslationConfig translationConfig)
    {
    	this.translationConfig = translationConfig;
    }
       
    protected void unbindTranslationConfig(TranslationConfig translationConfig)
    {
    	if (this.translationConfig == translationConfig) {
    		this.translationConfig = null;
    	}
    }
    
    @Activate
    protected void activate(final ComponentContext ctx) {
        log.error("LQ == Starting function: activate");
        final Dictionary<?, ?> properties = ctx.getProperties();

        factoryName = PropertiesUtil.toString(properties.get(TranslationServiceFactory.PROPERTY_TRANSLATION_FACTORY),"");

        isPreviewEnabled = PropertiesUtil.toBoolean(properties.get(CloudwordsConstants.PREVIEW_ENABLED), false);
        
        exportFormat = PropertiesUtil.toString(properties.get(CloudwordsConstants.EXPORT_FORMAT_FIELD), CloudwordsConstants.EXPORT_FORMAT_XML);
        
        rr = getResourceResolver(resourceResolverFactory);
        
        if (log.isTraceEnabled()) {
            log.trace("Activated TSF with the following:");
            log.trace("Factory Name: {}", factoryName);
            log.trace("Preview Enabled: {}",isPreviewEnabled);
            log.trace("Export Format: {}", exportFormat);
        }
    }
    
    
    protected ResourceResolver getResourceResolver(ResourceResolverFactory resourceResolverFactory){
    	log.error("LQ == Starting function: getResourceResolver");
		ResourceResolver resourceResolver = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put(ResourceResolverFactory.SUBSERVICE, "readService");
		param.put(ResourceResolverFactory.USER, "cloudwords-service");
		
			try {
				resourceResolver = resourceResolverFactory.getServiceResourceResolver(param);
				log.error("LQ == rr user id:" + resourceResolver.getUserID());
				Resource res = resourceResolver.getResource("/content/geometrixx");
	            log.error("LQ == Resource : " + res.getPath());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		return resourceResolver;
		}

}