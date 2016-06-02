package com.headwire.pageUploader.services.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ResourceResolverFactory;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * OSGi event handler that watches for changes to the AEM translation job status and uploads translated document preview zip file to cloudwords
 */
@Component(immediate = true, metatype = true)
@Service(value = EventHandler.class)
@Properties({
    @Property(name = EventConstants.EVENT_TOPIC, value = {SlingConstants.TOPIC_RESOURCE_ADDED, SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED}),
    @Property(name = EventConstants.EVENT_FILTER, value = "(path=/content/projects/*)")
})
public class TranslationCoreEventListener implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(TranslationCoreEventListener.class);
	
	@Reference 
	private ResourceResolverFactory resourceResolverFactory; 


    @Override
    public void handleEvent(final Event event) {
    	// to do, use resourceResolver since certain attributes are not available in event object
    	//log.error("LQ == now in handle event: " + event.getTopic());
    	//log.error("event class is :" + event.getClass().getName());
    	/*if(event.getProperty("resourceType") != null && event.getProperty("resourceType").equals("cq/gui/components/projects/admin/card/translation_object")){
    		for(String propName : event.getPropertyNames()){
        		log.error("prop name: " + propName);
        		log.error("prop value: " + event.getProperty(propName));
        		if(propName.equals("resourceAddedAttributes")){
        			//log.error("prop value: " + Arrays.toString(event.getProperty(propName)));
        			for(String attr : (String[])event.getProperty(propName)){
        				log.error("attr is: " + attr);
        				
        			}
        		}
        	}
    	}*/
    }
}

