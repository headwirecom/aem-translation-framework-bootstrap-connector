package com.headwire.translation.connector.cloudwords.core.impl.config;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.adobe.granite.translation.api.TranslationException;
import com.headwire.translation.connector.cloudwords.core.CloudwordsTranslationCloudConfig;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudwordsTranslationCloudConfigImpl implements CloudwordsTranslationCloudConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudwordsTranslationCloudConfigImpl.class);

    private String translationServiceAttribution;
    private String translationServiceLabel;
	private String endpoint;
	private String apiKey;
	private String bidDeadline;
	private String bidinitialTranslationDeadline;
	private String projectDescription;
	private String previewPath;


    public CloudwordsTranslationCloudConfigImpl(Resource translationConfigResource) throws TranslationException {
        log.trace("Starting constructor: CloudwordsTranslationCloudConfigImpl");
        Resource configContent;
        if (JcrConstants.JCR_CONTENT.equals(translationConfigResource.getName())) {
            configContent = translationConfigResource;
        } else {
            configContent = translationConfigResource.getChild(JcrConstants.JCR_CONTENT);
        }

        if (configContent != null) {
            ValueMap properties = configContent.adaptTo(ValueMap.class);

            this.translationServiceAttribution = properties.get(PROPERTY_TRANSLATION_SERVICE_ATTRIBUTION, "");
            this.translationServiceLabel = properties.get(PROPERTY_TRANSLATION_SERVICE_LABEL, "");
            this.endpoint = properties.get(PROPERTY_TRANSLATION_END_POINT, "");
            this.apiKey = properties.get(PROPERTY_TRANSLATION_API_KEY, "");
            this.bidDeadline = properties.get(PROPERTY_TRANSLATION_BID_DEADLINE, "7");
            this.bidinitialTranslationDeadline = properties.get(PROPERTY_TRANSLATION_INITIAL_TRANSLATION_DEADLINE, "14");
            this.projectDescription = properties.get(PROPERTY_TRANSLATION_PROJECT_DESCRIPTION, "");
            this.previewPath = properties.get(PROPERTY_TRANSLATION_PREVIEW_PATH,"");

            if (log.isDebugEnabled()) {
                log.debug("Created Cloudwords Cloud Config with the following:");
                log.debug("translationServiceAttribution: {}", translationServiceAttribution);
                log.debug("translationServiceLabel: {}", translationServiceLabel);
                log.debug("previewPath: {}", previewPath);
            }
        } else {
            throw new TranslationException("Error getting Cloud Config credentials",
                TranslationException.ErrorCode.MISSING_CREDENTIALS);
        }
    }

    public String getServiceAttribution() {
        log.trace("In function: getServiceAttribution");
        return translationServiceAttribution;
    }

    public String getServiceLabel() {
        log.trace("In function: getServiceLabel");
        return translationServiceLabel;
    }

	@Override
	public String getEndpoint() {
		return this.endpoint;
	}

	@Override
	public String getApiKey() {
		return this.apiKey;
	}

	@Override
	public String getDefaultBidDeadline() {
		return this.bidDeadline;
	}
	
	@Override
	public String getDefaultInitialTranslationDeadline() {
		return bidinitialTranslationDeadline;
	}

	@Override
	public String getDefaultProjectDescription() {
		return this.projectDescription;
	}
	
	@Override
	public String getPreviewPath(){
        log.trace("gePreviewPath");
        return previewPath;
    }
	
	@Override
	public void decryptSecret(CryptoSupport helper)	{
		log.trace("decrypt api key...");
		try {
			this.apiKey = helper.unprotect(this.apiKey);
		} catch (CryptoException e) {
			log.error("CryptoException:{}", e);
		}
	}
    
    
}