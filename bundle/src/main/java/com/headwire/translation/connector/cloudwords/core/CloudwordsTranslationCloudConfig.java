package com.headwire.translation.connector.cloudwords.core;

import com.adobe.granite.crypto.CryptoSupport;

public interface CloudwordsTranslationCloudConfig {

    public static final String PROPERTY_TRANSLATION_SERVICE_ATTRIBUTION = "serviceattribution";
    public static final String PROPERTY_TRANSLATION_SERVICE_LABEL = "servicelabel";
    public static final String PROPERTY_TRANSLATION_END_POINT = "endpoint";
    public static final String PROPERTY_TRANSLATION_API_KEY= "apiKey";
    public static final String PROPERTY_TRANSLATION_BID_DEADLINE= "bidDeadline";
    public static final String PROPERTY_TRANSLATION_INITIAL_TRANSLATION_DEADLINE= "initialTranslationDeadline";
    public static final String PROPERTY_TRANSLATION_PROJECT_DESCRIPTION= "projectDescription";

    public static final String RESOURCE_TYPE = "cloudwords-connector/components/cloudwords-connector-cloudconfig";
    

    String getServiceAttribution();

    String getServiceLabel();
    
    String getEndpoint();
    
    String getApiKey();
    
    String getDefaultBidDeadline();
    
    String getDefaultInitialTranslationDeadline();

    String getDefaultProjectDescription();
    
    void decryptSecret(CryptoSupport helper);
	
}