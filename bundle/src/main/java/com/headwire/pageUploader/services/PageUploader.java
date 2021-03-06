package com.headwire.pageUploader.services;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import com.cloudwords.api.client.CloudwordsCustomerClient;

public interface PageUploader {
	
	public void uploadPage(ResourceResolver rr, int projectId, String targetLanguageCode, String translationDataEntry, String pagePath, CloudwordsCustomerClient customerClient);
	
	public void uploadSourcePage(ResourceResolver rr, int projectId, String translationDataEntry, String pagePath, CloudwordsCustomerClient customerClient);
	
	public String getPageHtml(ResourceResolver rr, ResourceResolverFactory resourceResolverFactory, String pageUrl);
	
	/**
	 * Gets the named property of this service.
	 *
	 * @param <T> the generic type
	 * @param key the key
	 * @param defaultValue the default value
	 * @return the property
	 */
	public <T> T getProperty(String key, T defaultValue);
	
	/**
	 * Gets the named property of this service.
	 *
	 * @param key the key
	 * @return the property
	 */
	public Object getProperty(String key);

}
