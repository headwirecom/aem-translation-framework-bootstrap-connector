package com.headwire.translation.connector.cloudwords.core.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
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

    //@Reference
    //BootstrapTmsService bootstrapTmsService;
    /*
    private Map<String,String> properties = new HashMap<String,String>();
	@Activate
	protected void activate(ComponentContext componentContext) {setup(componentContext);}
	@Modified
	protected void modified(ComponentContext componentContext) {setup(componentContext);}
	@Deactivate
	protected void deactivate() {properties.clear();}

	protected void setup(ComponentContext componentContext) {
		Dictionary dictionary = componentContext.getProperties();
		Enumeration<String> e = dictionary.keys();
		while(e.hasMoreElements()) {
			String key = e.nextElement();
			Object temp = dictionary.get(key);
			properties.put(key, temp == null ? "" : temp.toString());
		}
	} */
    
    private List<TranslationMethod> supportedTranslationMethods;

    public CloudwordsTranslationServiceFactoryImpl() {
        supportedTranslationMethods = new ArrayList<TranslationMethod>();
        supportedTranslationMethods.add(TranslationMethod.HUMAN_TRANSLATION);
    } 

    private static final Logger log = LoggerFactory.getLogger(CloudwordsTranslationServiceFactoryImpl.class);

    @Override
    public TranslationService createTranslationService(TranslationMethod translationMethod, String cloudConfigPath)
        throws TranslationException {
        log.trace("In function: getTranslationService()");
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
        
        //isPreviewEnabled = Boolean.valueOf(properties.get(CloudwordsConstants.PREVIEW_ENABLED));
        //exportFormat = properties.get(CloudwordsConstants.EXPORT_FORMAT_FIELD);
        
        return new CloudwordsTranslationServiceImpl(null, null, factoryName, strServiceLabel, strServiceAttribute, previewPath, isPreviewEnabled, exportFormat, cloudConfigPath, cloudwordsCloudConfg, translationConfig, cloudwordsTranslationCache);
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

}