package com.headwire.pageUploader.services.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.crypto.CryptoSupport;
import com.adobe.granite.translation.core.TranslationCloudConfigUtil;
import com.cloudwords.api.client.CloudwordsCustomerClient;
import com.cloudwords.api.client.exception.CloudwordsClientException;
import com.cloudwords.api.client.resources.Language;
import com.cloudwords.api.client.resources.Project;
import com.headwire.translation.connector.cloudwords.core.CloudwordsTranslationCloudConfig;


/**
 * OSGi event handler that watches for changes to the AEM translation job status and uploads translated document preview zip file to cloudwords
 */
@Component(immediate = true, metatype = true)
@Service(value = EventHandler.class)
@Properties({
    @Property(name = EventConstants.EVENT_TOPIC, value = {SlingConstants.TOPIC_RESOURCE_CHANGED}),
    @Property(name = EventConstants.EVENT_FILTER, value = "(path=/content/projects/*)")
})
public class TranslationCoreEventListener implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(TranslationCoreEventListener.class);
	
	@Reference 
	private ResourceResolverFactory resourceResolverFactory; 
	
	@Reference
    private PageUploaderImpl pageUploaderImpl;
	
	@Reference
    TranslationCloudConfigUtil cloudConfigUtil;
	
	@Reference 
    CryptoSupport cryptoSupport;
	
	CloudwordsTranslationCloudConfig cloudwordsCloudConfg;
	
	private static final String RESOURCE_TYPE = "resourceType";
	private static final String RESOURCE_PATH = "path";
	private static final String TRANSLATION_OBJECT_ID = "translationObjectID";
	private static final String TRANSLATION_OBJECT = "cq/gui/components/projects/admin/card/translation_object";
	private static final String TRANSLATION_FILE_TYPE = "translationFileType";
	private static final String TRANSLATION_FILE_TYPE_PAGE = "PAGE";
	private static final String TRNSLATION_STATUS = "translationStatus";
	private static final String TRNSLATION_STATUS_READY = "READY_FOR_REVIEW";
	private static final String TRANSLATION_PAGE_PATH = "sourcePath";
	private static final String CLOUD_CONFIG_PATH = "/etc/cloudservices/cloudwords-translation/cloudwordsconfig";


    @Override
    public void handleEvent(final Event event) {
    	
    	log.trace("LQ == now in handle event: " + event.getTopic());
    	
    	// cloud_config_path
    	cloudwordsCloudConfg = (CloudwordsTranslationCloudConfig) cloudConfigUtil.getCloudConfigObjectFromPath(CloudwordsTranslationCloudConfig.class, CLOUD_CONFIG_PATH);
        
        if (cloudwordsCloudConfg != null) {
               cloudwordsCloudConfg.decryptSecret(cryptoSupport);
        } 
    	
    	// 1. Find node that having resourceType = translation_object
    	if(event.getProperty(RESOURCE_TYPE) != null && event.getProperty(RESOURCE_TYPE).equals(TRANSLATION_OBJECT)){
    		    		
    		// 2. Find node that is a page and translationStatus = ready_for_review
    		String resourcePath = (String)event.getProperty(RESOURCE_PATH);
    		ResourceResolver rr = getResourceResolver(resourceResolverFactory);
    		Resource resource = rr.resolve(resourcePath);
    		
    		if(resource != null){
    			// find translationStatus property
    			ValueMap property = resource.getValueMap();
    			String fileType =  property.get(TRANSLATION_FILE_TYPE, "");
    			String translationStatus = property.get(TRNSLATION_STATUS, "");
    			String pagePath = property.get(TRANSLATION_PAGE_PATH, "");
    			String pageName = pagePath.replaceAll("/","_") + ".xml";
    			   			
    			// 3. If translationStatus is ready_for_review, let's upload a page preview copy
    			if(fileType.equals(TRANSLATION_FILE_TYPE_PAGE) && translationStatus.equals(TRNSLATION_STATUS_READY)){
    				// let's get translation project id
    				Resource translationJob = resource.getParent().getParent();
    				//log.error("translationJob path is: " + translationJob.getPath());
    				ValueMap parentProperty = translationJob.getValueMap();
    				String cwTranslationJobId = parentProperty.get(TRANSLATION_OBJECT_ID, "");
    				//log.error("translation object id is: " + cwTranslationJobId);
    				log.trace("now uploading preview zip...");
    				pageUploaderImpl.uploadPage(rr, getIntFromNullableString(cwTranslationJobId), new Language(getProjectTargetLanguage(cwTranslationJobId)).getLanguageCode(), pageName, pagePath + ".html", getClient());
    			}
    			
    		}
    		
    		// Close resource resolver
    		closeResourceResolver(rr);
    	}
    }
    
    /*
     * Get resource resolver
     */
    private synchronized ResourceResolver getResourceResolver(ResourceResolverFactory resourceResolverFactory){
    	log.trace("LQ == Starting function: getResourceResolver");
		ResourceResolver resourceResolver = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put(ResourceResolverFactory.SUBSERVICE, "readService");
		param.put(ResourceResolverFactory.USER, "cloudwords-service");
		
		try {
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(param);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return resourceResolver;
	}
    
    /*
     * Close resource resolver
     */
    private synchronized void closeResourceResolver(ResourceResolver resourceResolver){ 
        if(null!=resourceResolver && resourceResolver.isLive()){ 
        	resourceResolver.close(); 
        } 
    } 
    
    /*
     * Get project target language
     */
    private String getProjectTargetLanguage(String strTranslationJobID){
		
		CloudwordsCustomerClient client = getClient();
		try {
			Project project = client.getProject(getIntFromNullableString(strTranslationJobID));
			return project.getTargetLanguages().get(0).getLanguageCode();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (CloudwordsClientException e) {
			e.printStackTrace();
		}
		return null;
	} 
    
    /*
     * Get cloudwords client
     */
    public CloudwordsCustomerClient getClient() {
		log.trace("creating cloudwords client");
		
		String apiKey = null;
		String endPoint = null;
		
		if(cloudwordsCloudConfg != null) {
			apiKey = cloudwordsCloudConfg.getApiKey();
			endPoint = cloudwordsCloudConfg.getEndpoint();
		}
		log.trace("apikey  : {}", apiKey);
		log.trace("endpoint: {}", endPoint);
		
		CloudwordsCustomerClient client = new CloudwordsCustomerClient(endPoint, "1.20", apiKey);

		return client;
    }
    
    /*
     * Get cloudwords project id
     */
    private int getIntFromNullableString(String value) {
		if( value == null || value.trim().length() == 0) {
			return -1;
		}
		
		try {
			return Integer.parseInt(value);
		}
		catch( Exception quiet) {
			
		}
		
		return -1;
	}
}

