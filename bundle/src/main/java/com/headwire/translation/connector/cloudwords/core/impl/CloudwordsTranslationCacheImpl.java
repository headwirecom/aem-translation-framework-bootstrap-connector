package com.headwire.translation.connector.cloudwords.core.impl;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudwords.api.client.CloudwordsCustomerClient;
import com.cloudwords.api.client.resources.Language;
import com.cloudwords.api.client.resources.TranslatedDocument;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Service(CloudwordsTranslationCacheImpl.class)
@Component(label = "Cloudwords Translation Cache Service", metatype = true, immediate = true)
@Properties(value = {
	    @Property(name = "service.description", value = "cloudwords cache")})
public class CloudwordsTranslationCacheImpl{
	
	private static final Logger LOG = LoggerFactory.getLogger(CloudwordsTranslationCacheImpl.class);
	
	private LoadingCache<String, String> projectStatusCache = null;
	
	private LoadingCache<String, List<TranslatedDocument>> translatedDocumentsCache = null;
	
	
	public String getProjectStatusCache(final CloudwordsCustomerClient client, String projectId) {
		LOG.trace("in getProjectStatusCache");
		if(null == projectStatusCache){
			LOG.trace("in getProjectStatusCache, cache is null");
			projectStatusCache = CacheBuilder.newBuilder()
				.maximumSize(100)
			    .expireAfterWrite(1, TimeUnit.MINUTES)
			    .build(
			    		new CacheLoader<String, String>() {
			    			public String load(String key) throws Exception {
			    				LOG.debug("in getProjectStatusCache, build cache now");
			    				return client.getProject(Integer.parseInt(key)).getStatus().getCode();
			    			}
			    		});
		}
		try {
			LOG.trace("in getProjectStatusCache, return cache");
			return projectStatusCache.get(projectId);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
		
	}

	
	public List<TranslatedDocument> getTranslatedDocumentsCache(final CloudwordsCustomerClient client, String projectId, String languageCode) {
		LOG.trace("in getTranslatedDocumentsCache");
		if(null == translatedDocumentsCache){
			LOG.trace("in getTranslatedDocumentsCache, cache is null");
			translatedDocumentsCache = CacheBuilder.newBuilder()
				.maximumSize(10000)
			    .expireAfterWrite(1, TimeUnit.MINUTES)
			    .build(
			    		new CacheLoader<String, List<TranslatedDocument>>() {
			    			public List<TranslatedDocument> load(String key) throws Exception {
			    				String[] keys = key.split("\\|");
			    				LOG.trace("in getTranslatedDocumentsCache, building cache");
			    				return client.getTranslatedDocuments(Integer.parseInt(keys[0]),new Language(keys[1]));
			    			}
			    		});
		}
		try {
			LOG.trace("in getTranslatedDocumentsCache, return cache");
			return translatedDocumentsCache.get(projectId + "|" + languageCode);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return null;
		}
		
	}


}
