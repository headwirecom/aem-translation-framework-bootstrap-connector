package com.headwire.translation.connector.cloudwords.core.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
    @Property(name = "service.description", value = "cloudwords translation service"),
    @Property(name = TranslationServiceFactory.PROPERTY_TRANSLATION_FACTORY, value = "cloudwords",
            label = "Cloudwords Translation Factory Name", description = "The Unique ID associated with this "
                    + "Translation Factory Connector")})
public class CloudwordsTranslationServiceFactoryImpl extends AbstractTranslationServiceFactory implements
    TranslationServiceFactory {

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
        return new CloudwordsTranslationServiceImpl(null, null, factoryName, strServiceLabel, strServiceAttribute, previewPath, cloudConfigPath, cloudwordsCloudConfg, translationConfig, cloudwordsTranslationCache);
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