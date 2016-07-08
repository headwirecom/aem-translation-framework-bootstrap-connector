package com.headwire.translation.connector.cloudwords.core.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.comments.Comment;
import com.adobe.granite.comments.CommentCollection;
import com.adobe.granite.translation.api.TranslationConfig;
import com.adobe.granite.translation.api.TranslationConstants;
import com.adobe.granite.translation.api.TranslationConstants.TranslationMethod;
import com.adobe.granite.translation.api.TranslationConstants.TranslationStatus;
import com.adobe.granite.translation.api.TranslationException;
import com.adobe.granite.translation.api.TranslationException.ErrorCode;
import com.adobe.granite.translation.api.TranslationMetadata;
import com.adobe.granite.translation.api.TranslationObject;
import com.adobe.granite.translation.api.TranslationResult;
import com.adobe.granite.translation.api.TranslationScope;
import com.adobe.granite.translation.api.TranslationService;
import com.adobe.granite.translation.api.TranslationState;
import com.adobe.granite.translation.core.common.AbstractTranslationService;
import com.adobe.granite.translation.core.common.TranslationResultImpl;
import com.cloudwords.api.client.CloudwordsCustomerClient;
import com.cloudwords.api.client.exception.CloudwordsClientException;
import com.cloudwords.api.client.resources.CloudwordsFile;
import com.cloudwords.api.client.resources.Department;
import com.cloudwords.api.client.resources.IntendedUse;
import com.cloudwords.api.client.resources.Language;
import com.cloudwords.api.client.resources.Project;
import com.cloudwords.api.client.resources.SourceDocument;
import com.cloudwords.api.client.resources.TranslatedDocument;
import com.headwire.translation.connector.cloudwords.core.CloudwordsAware;
import com.headwire.translation.connector.cloudwords.core.CloudwordsTranslationCloudConfig;
import com.headwire.xliff.util.FileUtil;
import com.headwire.xliff.util.SearchAndReplaceInputStreamUtil;
import com.headwire.pageUploader.services.ZipDirectoryUtil;
import com.headwire.pageUploader.services.impl.PageUploaderImpl;

public class CloudwordsTranslationServiceImpl extends AbstractTranslationService implements TranslationService, CloudwordsAware {

    private static final Logger log = LoggerFactory.getLogger(CloudwordsTranslationServiceImpl.class);

    private CloudwordsTranslationCloudConfig cloudwordsTranslationCloudConfig;
    
    //private CloudwordsTranslationCacheImpl translationCache;
    
    private PageUploaderImpl pageUploaderImpl;
    
    private ResourceResolver rr;
    
    private static final String TAG_META_DATA_TITLE = "translation_tag_metadata";
    
    private static final String ASSET_META_DATA_TITLE = "translation_asset_metadata";
    
    private static final String EMPTY_TRANSLATION_OBJECT_ID = "empty";
    
    private static final String PROJECT_ID_PREFIX = "CW_";
    
    private static final String IN_REVIEW_STATUS = "in_review";
    
    private String exportFormat = CloudwordsConstants.EXPORT_FORMAT_XML;
    
    private String previewPath = "";
    
    private Boolean isPreviewEnabled = false;
    
    private String previewFormat = "";
    
    
    public CloudwordsTranslationServiceImpl(
			Map<String, String> availableLanguageMap,
			Map<String, String> availableCategoryMap, String name,
			String label, String attribution, String previewPath, Boolean isPreviewEnabled, String previewFormat,String exportFormat,
			String translationCloudConfigRootPath,
			CloudwordsTranslationCloudConfig cwtc, 
			TranslationConfig translationConfig,
			//CloudwordsTranslationCacheImpl cache,
			PageUploaderImpl pageUploader,
			ResourceResolver resourceResolver
			) {
		super(availableLanguageMap, 
				availableCategoryMap, 
				name, 
				label, 
				attribution,
				translationCloudConfigRootPath == null ? "/etc/cloudservices/cloudwords-translation" : translationCloudConfigRootPath, 
				TranslationMethod.HUMAN_TRANSLATION, 
				translationConfig
				);

//		log.trace("Starting Constructor for: cloudwordsTranslationServiceImpl");
//		log.trace("RR: ==== langs      : "+availableLanguageMap);
//		log.trace("RR: ==== categories : "+availableCategoryMap);
//		log.trace("RR: ==== name       : "+name);
//    	log.trace("RR: ==== label      : "+label);
//    	log.trace("RR: ==== attribution: "+attribution);
//    	log.trace("RR: ==== config path: "+translationCloudConfigRootPath);
        cloudwordsTranslationCloudConfig = cwtc;
        //translationCache = cache;
        pageUploaderImpl = pageUploader;
        this.previewPath = previewPath;
        this.isPreviewEnabled = isPreviewEnabled;
        this.previewFormat = previewFormat;
        this.exportFormat = exportFormat;
        this.rr = resourceResolver;
    }

    @Override
    public Map<String, String> supportedLanguages() {
    	//log.trace("RR: ===== In Function: supportedLanguages");
        
        CloudwordsCustomerClient client = getClient();
        try {
			HashMap<String, String> ret = new HashMap<String, String>();
			
			List<Language> sourceLanguages = client.getSourceLanguages();
			for (Language language : sourceLanguages) {
				ret.put(language.getLanguageCode(), language.getDisplay());
			}
			
			List<Language> targetLanguages = client.getTargetLanguages();
			for (Language language : targetLanguages) {
				ret.put(language.getLanguageCode(), language.getDisplay());
			}
			
	        return Collections.unmodifiableMap(ret);
		} catch (CloudwordsClientException e) {
			e.printStackTrace();
		}
        return Collections.unmodifiableMap(new HashMap<String, String>());
        
    }

    @Override
    public boolean isDirectionSupported(String sourceLanguage, String targetLanguage) throws TranslationException {

    	//log.trace("in isDirectionSupported: {} {}",sourceLanguage, targetLanguage);
    	
    	// Check if source is in source and target is in target  
    	if(getSourceLanguageCodes().contains(sourceLanguage) &&
    			getTargetLanguageCodes().contains(targetLanguage)){
    		return true;
    	} else{
    		return false;
    	}
    }
    
    @Override
    public String detectLanguage(String toDetectSource, TranslationConstants.ContentType contentType)
        throws TranslationException {
    	//log.trace("In Function: detectLanguage");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        String[] randomLang = {"de", "fr", "ja", "ko"};
        Random rand = new Random();
        return randomLang[rand.nextInt(randomLang.length)];
    }

    @Override
    public TranslationResult translateString(String sourceString, String sourceLanguage, String targetLanguage,
        TranslationConstants.ContentType contentType, String contentCategory) throws TranslationException {
        String translated = String.format("%s_%s_%s", targetLanguage, sourceString, targetLanguage);
        return new TranslationResultImpl(translated, sourceLanguage, targetLanguage, contentType, contentCategory,
            sourceString, TranslationResultImpl.UNKNOWN_RATING, null);
    }

    @Override
    public TranslationResult[] translateArray(String[] sourceStringArr, String sourceLanguage, String targetLanguage,
        TranslationConstants.ContentType contentType, String contentCategory) throws TranslationException {
        throw new TranslationException("This function is not implemented",
            TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
    }

    @Override
    public TranslationResult[] getAllStoredTranslations(String sourceString, String sourceLanguage,
        String targetLanguage, TranslationConstants.ContentType contentType, String contentCategory, String userId,
        int maxTranslations) throws TranslationException {

        throw new TranslationException("This function is not implemented",
            TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);

    }

    @Override
    public void storeTranslation(String[] originalText, String sourceLanguage, String targetLanguage,
        String[] updatedTranslation, TranslationConstants.ContentType contentType, String contentCategory,
        String userId, int rating, String path) throws TranslationException {

        throw new TranslationException("This function is not implemented",
            TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
    }

    @Override
    public void storeTranslation(String originalText, String sourceLanguage, String targetLanguage,
        String updatedTranslation, TranslationConstants.ContentType contentType, String contentCategory,
        String userId, int rating, String path) throws TranslationException {
        log.trace("Starting function:  updateTranslation");

        throw new TranslationException("This function is not implemented",
            TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);

    }

    @Override
    public String createTranslationJob(String name, String description, String strSourceLanguage,
        String strTargetLanguage, Date dueDate, TranslationState state, TranslationMetadata jobMetadata)
        throws TranslationException {

//    	log.debug("RR: ==== source lang: "+strSourceLanguage);
//    	log.debug("RR: ==== target lang: "+strTargetLanguage);
    	Project project = null;
		try {
			CloudwordsCustomerClient client = getClient();
			
			Project initialProject = createBaseProject(name, strSourceLanguage, strTargetLanguage, description, dueDate, client);
			project = client.createProject(initialProject);
		} catch (CloudwordsClientException e) {
            log.info("not able to create translation project: {}", e);
			// retry creating project without due date
			try {
				CloudwordsCustomerClient client = getClient();
				Project initialProject = createBaseProject(name, strSourceLanguage, strTargetLanguage, description, null, client);
				project = client.createProject(initialProject);
			} catch (CloudwordsClientException cwe) { 
				log.info("not able to create translation project: {}", cwe);
				throw new TranslationException("not able to create translation project", cwe, ErrorCode.GENERAL_EXCEPTION);
			}
		}
		log.trace("cloudwords project created with id {}", PROJECT_ID_PREFIX + project.getId());
		return ""+project.getId();
    }

    @Override
    public TranslationScope getFinalScope(String strTranslationJobID) {
    	// TODO get scope after bid is selected
//    	CloudwordsCustomerClient client = getClient();
//    	int id = client.getCurrentBidRequestForProject(1).getWinningBidId();
//    	client.getBid(1, id).getBidItems().get(0).getBidItemTasks().
//    	scope = new TranslationScope() {
//			
//			@Override
//			public int getWordCount() {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//			
//			@Override
//			public int getVideoCount() {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//			
//			@Override
//			public int getImageCount() {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//		};
    	return null;
    }

    @Override
    public TranslationStatus updateTranslationJobState(String strTranslationJobID, TranslationState state)
        throws TranslationException {
    	// TODO cluodwords to provide what methods to call in the transitions
    	log.info("in updateTranslationJobState: "+ PROJECT_ID_PREFIX + strTranslationJobID + " : " +state.getStatus().toString());
    	// if state is SCOPE_REQUESTED, throw exception
    	if(state.getStatus().equals(TranslationStatus.SCOPE_REQUESTED)){
    		throw new TranslationException("", ErrorCode.SERVICE_NOT_IMPLEMENTED); 
    	}
    	return null;
    }
    
    @Override
    public TranslationStatus getTranslationJobStatus(String cwProjectStatus) throws TranslationException {
    	
    	// Use CODES to translate
    	log.trace("in getTranslationJobStatus: {}",cwProjectStatus);
    	try {
			//Project project = getClient().getProject(getIntFromNullableString(strTranslationJobID));
			//String cwStatusCode = project.getStatus().getCode();
    		String cwStatusCode = cwProjectStatus;
			if(null != CODES.get(cwStatusCode)){
				return CODES.get(cwStatusCode);
			}else{
				// return UNKNOWN_STATE when no matching state is found from CODES map
				return TranslationStatus.UNKNOWN_STATE;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} 
    	return null;
    }

    @Override
    public CommentCollection<Comment> getTranslationJobCommentCollection(String strTranslationJobID) {
        // TODO not supported yet
        return null;
    }

    @Override
    public void addTranslationJobComment(String strTranslationJobID, Comment comment) throws TranslationException {
        // TODO not supported yet

    }

    @Override
    public InputStream getTranslatedObject(String strTranslationJobID, TranslationObject Object)
        throws TranslationException {
    	
    	// Get translated xliff from CW, and converts it to an InputStream
    	//log.error("LQ == getTranslatedObject("+PROJECT_ID_PREFIX+strTranslationJobID+", "+Object.getTitle()+")");
    	CloudwordsCustomerClient client = getClient();
    	
    	try {
			List<TranslatedDocument> translations = client.getTranslatedDocuments(getIntFromNullableString(strTranslationJobID), new Language(getProjectTargetLanguage(strTranslationJobID))); // Todo: target language needs to be passed in as Param
			for (TranslatedDocument doc : translations) {
			    // return translated doc object when its status = in_review
			    if ((doc.getStatus().getCode()).equalsIgnoreCase(IN_REVIEW_STATUS)) {
					CloudwordsFile file = doc.getXliff();
					if(file == null) file = doc.getFile();
					// image asset
					if(isBinaryObject(Object)){
						if(isTranslatedAsset(file,Object)){
							log.info(PROJECT_ID_PREFIX + strTranslationJobID + ": found translated asset...." + file.getFilename());
							return client.downloadFileFromMetadata(file);
						}
					}
					// page
					else{
					    if(isTranslatedXliff(file,Object)){
					    	log.info(PROJECT_ID_PREFIX + strTranslationJobID + ": found xliff...." + file.getFilename());
					    	String srcLang = getProjectSourceLanguage(strTranslationJobID);
					    	String targetLang = getProjectTargetLanguage(strTranslationJobID);
					    	//log.error("LQ == getTranslatedObject page: " + file.getFilename());
					    	//File tempFile = new File("c:/output/" + file.getFilename() + ".xml");
					    	//File temp2 = new File("c:/output/" + file.getFilename() + "_orig.xml"); 
					    	//copyInputStreamToFile(getInputStream(Object.getTranslationObjectInputStream(),".null","." + targetLang), temp2);
					    	//copyInputStreamToFile(XliffImporter.buildTranslatedObjectStream(getInputStream(Object.getTranslationObjectInputStream(),".null","." + targetLang),client.downloadFileFromMetadata(file),srcLang,targetLang),tempFile);
					    	if(exportFormat.equalsIgnoreCase(CloudwordsConstants.EXPORT_FORMAT_XML)){
					    		return XliffImporter.buildTranslatedObjectStream(getInputStream(Object.getTranslationObjectXMLInputStream(),".null","." + targetLang),client.downloadFileFromMetadata(file),srcLang,targetLang);
					    	} else{
					    		return XliffImporter.buildTranslatedObjectStream(getInputStream(Object.getTranslationObjectXMLInputStream(),".null","." + targetLang),client.downloadFileFromMetadata(file),srcLang,targetLang);
					    		// todo, return cloudwords translated xliff file when adobe side implementation is ready
					    		//return client.downloadFileFromMetadata(file);
					    	}
					    }
					}
				}
			}
		} catch (NumberFormatException e) {
			log.info(PROJECT_ID_PREFIX + strTranslationJobID + " get translated object NumberFormatException: {}", e);
			log.error("NumberFormatException: {}", e);
		} catch (CloudwordsClientException e) {
			log.info(PROJECT_ID_PREFIX + strTranslationJobID + " get translated object CloudwordsClientException: {}", e);
			log.error("CloudwordsClientException: {}", e);
		} catch (IllegalStateException e) {
			log.info(PROJECT_ID_PREFIX + strTranslationJobID + " get translated object IllegalStateException: {}", e);
			log.error("IllegalStateException: {}", e);
		} catch (IOException e) {
			log.info(PROJECT_ID_PREFIX + strTranslationJobID + " get translated object error: {}", e);
			log.error("IOException: {}", e);
		} 
    	    	
        return null;
    }
    
    @Override
    public String uploadTranslationObject(String strTranslationJobID, TranslationObject translationObject)
        throws TranslationException {
    	
    	// need to determine if it's a content that we need to convert from the adobe format to xliff or not, then upload it
        String tempFolder = FileUtil.getSystemTempFolder(); 
        InputStream is = null;
        if(exportFormat.equalsIgnoreCase(CloudwordsConstants.EXPORT_FORMAT_XML)){
        	is = translationObject.getTranslationObjectXMLInputStream();
        }
        else if(exportFormat.equalsIgnoreCase(CloudwordsConstants.EXPORT_FORMAT_XLIFF_1_2)){
        	is = translationObject.getTranslationObjectXLIFFInputStream("1.2");
        }
        else if (exportFormat.equalsIgnoreCase(CloudwordsConstants.EXPORT_FORMAT_XLIFF_2_0)){
        	// todo, need to pass in xliff version dynamically when 2.0 is supported by cloudwords
        	is = translationObject.getTranslationObjectXLIFFInputStream("1.2");
        }
    	    	
    	String sourcePath = getNonEmptySourcePath(translationObject);
    	
    	// Handle binary asset
    	try {
    		log.trace("path: {}, {}", sourcePath, translationObject.getMimeType());
    		if(isBinaryObject(translationObject)){
    			String srcPath = translationObject.getTranslationObjectSourcePath();
    			String imgName = toCWFileName(srcPath);
    			File imgFile = new File(tempFolder,imgName);
    			copyInputStreamToFile(is, imgFile);
    			SourceDocument source = getClient().addSourceDocument(getIntFromNullableString(strTranslationJobID), imgFile);
    			FileUtil.deleteTempFile(imgFile);
    			log.trace("srcPath is:" + srcPath);
    			log.trace("image name is:" + imgName);
    			log.trace("source document file name is:" + source.getFile().getFilename());
    			log.trace(PROJECT_ID_PREFIX + strTranslationJobID + " Binary file uploaded: " + tempFolder + imgName);
    			return "" + source.getId();
    		}
    			
		} catch (CloudwordsClientException e) {
			log.info(PROJECT_ID_PREFIX + strTranslationJobID + " Binary file upload error: {}", e);
		}
    	
    	// Handle page
    	if( sourcePath != null) {
    		String pagePath = sourcePath;
    		String pageName = sourcePath.replaceAll("/","_") + ".xml";	
    		sourcePath = tempFolder + pageName;
    		File xliffFile = null;
    		
			if(exportFormat.equalsIgnoreCase(CloudwordsConstants.EXPORT_FORMAT_XML)){
				xliffFile = XliffExporter.convertXmlToXliff(is, sourcePath, getProjectSourceLanguage(strTranslationJobID), getProjectTargetLanguage(strTranslationJobID));
			} else{
				xliffFile = new File(sourcePath.replace(".xml", ".xlf"));
				OutputStream os;
				try {
					os = new FileOutputStream(xliffFile);
					IOUtils.copy(is, os);
					os.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}catch (IOException e) {
					e.printStackTrace();
				} 
			}
		
			// Do not upload xliff File if it's null, this happens when a tag metadata xliff doesn't have any trans units
	    	if(null == xliffFile) { 
	    		log.debug("Xliff file is empty, don't upload to cloudwords"); 
	    		return EMPTY_TRANSLATION_OBJECT_ID;
	    	}
	    	
	    	String unzippedPath = previewPath + File.separator + strTranslationJobID + File.separator + pageName.replaceAll(".xml", "");
	    	
	    	// add xliff file to cloudwords project
	    	try {
				SourceDocument source = getClient().addSourceDocument(getIntFromNullableString(strTranslationJobID), xliffFile);
				// Remove temp xliffFile
		    	FileUtil.deleteTempFile(xliffFile);
		    	log.info(PROJECT_ID_PREFIX + strTranslationJobID + " Xliff file uploaded: " + xliffFile.getAbsolutePath());
		    	
		    	// LQ: now upload a page preview package to cloudwords
		    	if(isPreviewEnabled && (!translationObject.getTitle().equals("ASSETMETADATA")) && (!translationObject.getTitle().equals("TAGMETADATA"))) {
		    		if(previewFormat.equals(CloudwordsConstants.PREVIEW_FORMAT_HEADWIRE)){
		    			pageUploaderImpl.uploadSourcePage(rr, getIntFromNullableString(strTranslationJobID), pageName, pagePath + ".html", getClient());
		    		}
		    		else if(previewFormat.equals(CloudwordsConstants.PREVIEW_FORMAT_ADOBE)){
		    			uploadPreviewZip(getIntFromNullableString(strTranslationJobID),pageName, unzippedPath);
		    		}
		    	}
				return ""+source.getId();
				
			} catch (NumberFormatException e) {
				log.info(PROJECT_ID_PREFIX + strTranslationJobID + " Xliff file upload error: {}", e);
				log.error("NumberFormatException: {}", e);
			} catch (CloudwordsClientException e) {
				log.info(PROJECT_ID_PREFIX + strTranslationJobID + " Xliff file upload error: {}", e);
				log.error("CloudwordsClientException: {}", e);
			}
	    	
	    	// Remove temp xliffFile
	    	FileUtil.deleteTempFile(xliffFile);
    	}
    	
    	return null;
    }
    
    @Override
    public TranslationStatus updateTranslationObjectState(String strTranslationJobID,
        TranslationObject translationObject, TranslationState state) throws TranslationException {
    	
    	log.trace("LQ == in updateTranslationObjectState: "+strTranslationJobID+","+translationObject+","+state);
    	
    	if (translationObject.getId().equals(EMPTY_TRANSLATION_OBJECT_ID)) {
    		return TranslationConstants.TranslationStatus.READY_FOR_REVIEW;
        }
        
    	// TODO update state if approved
        return null;
    }

    @Override
    public TranslationStatus getTranslationObjectStatus(String strTranslationJobID,
        TranslationObject translationObject) throws TranslationException {
    	log.trace("getTranslationObjectState: "+strTranslationJobID+","+translationObject);
    	
    	CloudwordsCustomerClient client = getClient();
    	        
        if (translationObject.getId().equals(EMPTY_TRANSLATION_OBJECT_ID)) {
        	return TranslationConstants.TranslationStatus.READY_FOR_REVIEW;
        }
        
        try {
			List<TranslatedDocument> translations = client.getTranslatedDocuments(getIntFromNullableString(strTranslationJobID), new Language(getProjectTargetLanguage(strTranslationJobID))); // Todo: target language needs to be passed in as Param
			for (TranslatedDocument doc : translations) {
				CloudwordsFile file = doc.getXliff();
				// in case the file is not xliff format
				if(file == null) file = doc.getFile();
				//log.debug("doc file name is:" + file.getFilename());
				if(isTranslatedXliff(file, translationObject) || isTranslatedAsset(file, translationObject)){
					return translationObjectCodes.get(doc.getStatus().getCode());
				}
			}
			// When no match found, let's return project status instead, only "in_translation" will return
			log.trace("now return project status:");
			//String cwProjectStatus = translationCache.getProjectStatusCache(getClient(), strTranslationJobID);
			String cwProjectStatus = client.getProject(Integer.parseInt(strTranslationJobID)).getStatus().getCode();
			if(getTranslationJobStatus(cwProjectStatus).equals(TranslationStatus.TRANSLATION_IN_PROGRESS)){
				//log.trace("LQ==: project status is: " + getTranslationJobStatus(cwProjectStatus));
				return getTranslationJobStatus(cwProjectStatus);
			}
		} catch (NumberFormatException e) {
			log.error("NumberFormatException: {}", e );
		} catch (IllegalStateException e) {
			log.error("IllegalStateException: {}", e);
		} catch (CloudwordsClientException e) {
			log.error("CloudwordsClientException: {}", e);
		} 
        return null;
    }
    
    @Override
    public TranslationStatus[] updateTranslationObjectsState(String strTranslationJobID,
        TranslationObject[] translationObjects, TranslationState[] states) throws TranslationException {
        // TODO this won't work until updateTranslationObjectState is completed
    	List<TranslationStatus> statusList = new ArrayList<TranslationStatus>();
    	int i = 0;
    	for(TranslationObject object : translationObjects){
    		statusList.add(updateTranslationObjectState(strTranslationJobID, object, states[i]));
    		i++;
    	}
        return (TranslationStatus[]) statusList.toArray();
    }

    @Override
    public TranslationStatus[] getTranslationObjectsStatus(String strTranslationJobID,
        TranslationObject[] translationObjects) throws TranslationException {
    	List<TranslationStatus> statusList = new ArrayList<TranslationStatus>();
    	for(TranslationObject object : translationObjects){
    		//log.trace("status: " + getTranslationObjectStatus(strTranslationJobID, object));
    		//log.trace("object type: " +getTranslationObjectStatus(strTranslationJobID, object).getClass().getName());
    		statusList.add(getTranslationObjectStatus(strTranslationJobID, object));
    	}
        return statusList.toArray(new TranslationStatus[statusList.size()]);
    }

    @Override
    public CommentCollection<Comment> getTranslationObjectCommentCollection(String strTranslationJobID,
        TranslationObject translationObject) throws TranslationException {
        // not supported yet
        return null;
    }

    @Override
    public void addTranslationObjectComment(String strTranslationJobID, TranslationObject translationObject,
        Comment comment) throws TranslationException {
        // not supported yet
    }

    @Override
    public void updateTranslationJobMetadata(String strTranslationJobID, TranslationMetadata jobMetadata,
        TranslationMethod translationMethod) throws TranslationException {
        // TODO ignore for now
    }

	public CloudwordsCustomerClient getClient() {
		log.trace("creating cloudwords client");
		
		String apiKey = null;
		String endPoint = null;
		
		if(cloudwordsTranslationCloudConfig != null) {
			apiKey = cloudwordsTranslationCloudConfig.getApiKey();
			endPoint = cloudwordsTranslationCloudConfig.getEndpoint();
		}
		log.trace("apikey  : {}", apiKey);
		log.trace("endpoint: {}", endPoint);
		
		CloudwordsCustomerClient client = new CloudwordsCustomerClient(endPoint, "1.20", apiKey);

		return client;
    }
	
	private Project createBaseProject(String name, String strSourceLanguage,
			String strTargetLanguage, String description, Date dueDate, CloudwordsCustomerClient client) throws CloudwordsClientException {
		Project project = new Project();
    	project.setName(name);
    	project.setIntendedUse(getIntendedUse(client));
    	project.setSourceLanguage(new Language(getCwLanguageCode(strSourceLanguage)));
    	List<Language> targetLanguages = createListFromLanguage(getCwLanguageCode(strTargetLanguage));
    	project.setTargetLanguages(targetLanguages);
    	
//    	client.requestBidsForProject(arg0, arg1, arg2, arg3)
    	int tmp = getIntFromNullableString(cloudwordsTranslationCloudConfig.getDefaultBidDeadline()); 
    	if( tmp > -1) {
    		project.setBidDueDate(getDaysFromNow(tmp));
    	}
    	tmp = getIntFromNullableString(cloudwordsTranslationCloudConfig.getDefaultInitialTranslationDeadline()); 
    	if( tmp > -1) {
    		project.setDeliveryDueDate(getDaysFromNow(tmp));
    	}
    	if( dueDate != null) {
    		project.setDeliveryDueDate(dueDate);
    	}
    	
    	String desc = getFirst(description, cloudwordsTranslationCloudConfig.getDefaultProjectDescription());
    	if( desc != null) {
    		project.setDescription(desc);
    	}
    	
    	//TODO allow configuration for departments
    	// For now, choose either "marketing" or first department
    	Department dep = null;
    	List<Department> departments = client.getDepartments();
    	if(departments != null) {
    		for(Department d : departments) {
    			if(dep == null) {
    				// set dep as first department
    				dep = d;
    			}
    			else {
    				// for later ones, only set if its "Marketing"
    				if(d.getName().equals("Marketing")) {
    					dep = d;
    				}
    			}
    		}
    	}
    	// set department if its not null
    	if(dep != null) {
    		project.setDepartment(dep);
    	}
    	
    	//The project content type identifies the originating system of the project in Cloudwords
        project.setProjectContentType("aem");    	
		
		Map<String, Boolean> uiFeatures = new HashMap<String, Boolean>();
        uiFeatures.put("change_source_language", Boolean.FALSE);
        uiFeatures.put("change_target_languages", Boolean.FALSE);
        uiFeatures.put("change_source_material", Boolean.FALSE);
        uiFeatures.put("clone_project", Boolean.FALSE);
        project.setUiFeatures(uiFeatures);
        
        log.info("Base project created with name: {}", name);
		
        return project;

	}
	
	protected String getFirst( String... args ) {
		
		if( args == null || args.length ==0) {
			return null;
		}
		
		for( String s : args ) {
			if( s != null && s.trim().length() > 0 ) {
				return s;
			}
		}
		return null;
	}
	
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
	
	private Date getDaysFromNow(int days) {
		Calendar cal = Calendar.getInstance();
    	cal.add(Calendar.DATE, days);
    	return cal.getTime();
	}

	protected IntendedUse getIntendedUse(CloudwordsCustomerClient client)
			throws CloudwordsClientException {
		List<IntendedUse> uses = client.getIntendedUses();
    	IntendedUse thisIntendedUse = null;
    	for( IntendedUse use : uses) {
    		if( use.getName().equalsIgnoreCase("website")) {
    			thisIntendedUse = use;
    			break;
    		}
    	}
		return thisIntendedUse;
	}

	private List<Language> createListFromLanguage(String language) {
		List<Language> languages = new ArrayList<Language>();
    	languages.add(new Language(language));
		return languages;
	}
	
	private String getProjectSourceLanguage(String strTranslationJobID){
		
		CloudwordsCustomerClient client = getClient();
		try {
			Project project = client.getProject(getIntFromNullableString(strTranslationJobID));
			return project.getSourceLanguage().getLanguageCode();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (CloudwordsClientException e) {
			e.printStackTrace();
		}
		return null;
	}
	
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
	
	private boolean isBinaryObject (TranslationObject object){
		
		String contentType = object.getMimeType();
		if(contentType.equalsIgnoreCase("text/html") || contentType.equalsIgnoreCase("text/xml")){
			return false;
		} else{
			return true;
		}
	}
		
	// Translation Project status mapping between cloudwords and AEM
    private static HashMap<String, TranslationStatus> CODES = new HashMap<String, TranslationConstants.TranslationStatus>();
    {
    	CODES.put("configured_project_name", TranslationStatus.DRAFT);
    	CODES.put("configured_project_details", TranslationStatus.DRAFT);
    	CODES.put("uploaded_source_materials", TranslationStatus.DRAFT);
    	CODES.put("configured_bid_options", TranslationStatus.SUBMITTED);
    	CODES.put("submitted_for_bids", TranslationStatus.SUBMITTED);
    	CODES.put("waiting_for_bid_selection", TranslationStatus.SUBMITTED);
    	CODES.put("bid_selection_expired", TranslationStatus.ERROR_UPDATE);
    	CODES.put("in_translation", TranslationStatus.TRANSLATION_IN_PROGRESS);
    	CODES.put("in_review", TranslationStatus.TRANSLATED);
    	CODES.put("change_order_requested", TranslationStatus.REJECTED);
    	CODES.put("all_languages_approved", TranslationStatus.APPROVED);
    	CODES.put("in_cancellation_waiting_for_vendor", TranslationStatus.CANCEL);
    	CODES.put("in_cancellation_waiting_for_customer", TranslationStatus.CANCEL);
    	CODES.put("project_closed", TranslationStatus.COMPLETE);
    	CODES.put("cancelled", TranslationStatus.CANCEL);
    }
	
    private Set<String> getSourceLanguageCodes(){
    	CloudwordsCustomerClient client = getClient();
    	Set<String> sourceLanguageCodes = new HashSet<String>();
    	List<Language> sourceLanguages;
		try {
			sourceLanguages = client.getSourceLanguages();
			for (Language language : sourceLanguages) {
				sourceLanguageCodes.add(language.getLanguageCode());
			}
		} catch (CloudwordsClientException e) {
			log.error("problem checking languages", e);
		}
		return sourceLanguageCodes;
    }
    
    private Set<String> getTargetLanguageCodes(){
    	CloudwordsCustomerClient client = getClient();
    	Set<String> targetLanguageCodes = new HashSet<String>();
    	List<Language> targetLanguages;
		try {
			targetLanguages = client.getTargetLanguages();
			for (Language language : targetLanguages) {
				targetLanguageCodes.add(language.getLanguageCode());
			}
		} catch (CloudwordsClientException e) {
			e.printStackTrace();
		}
		return targetLanguageCodes;
    }
    
    // TranslationObject status mapping between cloudwords and AEM
    private static HashMap<String, TranslationStatus> translationObjectCodes = new HashMap<String, TranslationConstants.TranslationStatus>();
    {
    	translationObjectCodes.put("syncing", TranslationStatus.TRANSLATION_IN_PROGRESS);
    	translationObjectCodes.put("in_translation", TranslationStatus.TRANSLATION_IN_PROGRESS);
    	translationObjectCodes.put("in_revision", TranslationStatus.TRANSLATION_IN_PROGRESS);
    	//translationObjectCodes.put("delivered", TranslationStatus.TRANSLATED);
    	translationObjectCodes.put("in_review", TranslationStatus.TRANSLATED);
    }
    
    private void uploadPreviewZip(int projectId, String pageName, String pageFilePath){
    	
    	// Step 1: Zip the entire folder
		String folderPath = pageFilePath;
		File fileToZip = new File(folderPath);
		// create zip file
		String zipFileName = System.getProperty("java.io.tmpdir")  + projectId + "_" + pageName.replaceAll(".xml", "") + ".zip";
		//log.error("zip location: " + zipFileName);
		File zipFile = new File(zipFileName);
		//log.error("LQ== zip file path is:" + zipFile.getPath());
		try {
			ZipDirectoryUtil.zipFile(folderPath, zipFileName, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Step 2: Upload Zip file to cloudwords site
		uploadZip(zipFile, projectId, pageName);
		
		// Step 3: Remove zip file
		ZipDirectoryUtil.deleteTempFiles(fileToZip, zipFile);
		
	}
    
    private void uploadZip(File zipFile, int projectId, String pageName){
    		
    	try {
									
			// match xliff name
			List<SourceDocument> docs = getClient().getSourceDocuments(projectId);
			for(SourceDocument doc : docs){
				//log.error("LQ== cw doc name is:" + doc.getXliff().getFilename());
				if(doc.getXliff().getFilename().equals(pageName)){
					//log.error("find match, uploading zip now");
					getClient().addSourceDocumentPreview(projectId, doc.getId(), zipFile);
				}else{
					log.warn("not match, from cw: " + doc.getXliff().getFilename() + "  , from cq: " +  pageName);
					continue;
				}
			}
			log.debug("Page zip uploaded to CW");
		} catch (CloudwordsClientException e) {
			log.error("CloudwordsClientException: {}", e);
		}
		
	} 
	
    private String toCWFileName(String assetName){
    	String cwFileName = null;
    	//String cwFileName = assetName.replaceAll("\\s", "\\{\\{whiteSpace\\}\\}");
    	try {
			cwFileName = java.net.URLEncoder.encode(assetName,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        return cwFileName;
    }
    
    private String toAemFileName(String assetName){
    	String aemFileName = null;
    	//aemFileName = assetName.replaceAll("\\{\\{whiteSpace\\}\\}", " ");
    	try {
			aemFileName = java.net.URLDecoder.decode(assetName,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	return aemFileName;
    }
    
    private InputStream getInputStream(InputStream inputStream, String pattern, String replacement) {
    	return SearchAndReplaceInputStreamUtil.searchAndReplace(inputStream, pattern, replacement);
    }
    
    private String getNonEmptySourcePath (TranslationObject translationObject){
    	
    	if(translationObject.getTranslationObjectSourcePath()!= null && !translationObject.getTranslationObjectSourcePath().isEmpty()){
    		return  translationObject.getTranslationObjectSourcePath();
    	}
    	else if(translationObject.getTitle().equals("TAGMETADATA")){
    		return TAG_META_DATA_TITLE;
    	}
    	else if(translationObject.getTitle().equals("ASSETMETADATA")){
    		return ASSET_META_DATA_TITLE;
    	}
    	
    	return null;
    	
    	
    }
    
    private void copyInputStreamToFile( InputStream in, File file ) {
        try {
        	log.trace("in copyInputStreamToFile start");
        	
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
            log.trace("in copyInputStreamToFile end");
        } catch (Exception e) {
            log.error("Exception when copying inputstream to file: {}", e);
        }
    }
    
    private boolean isTranslatedXliff(CloudwordsFile file, TranslationObject object){
    	String sourcePath = getNonEmptySourcePath(object);
    	// page and tag meta data
    	if(file != null && file.getFilename().equals(sourcePath.replaceAll("/","_") + ".xlf")){
			return true;
		} else{
			return false;
		}
    	
    }
    
    private boolean isTranslatedAsset(CloudwordsFile file, TranslationObject object){
    	String srcPath = object.getTranslationObjectSourcePath();
    	//if(toAemFileName(file.getFilename()).equals(srcPath.substring(srcPath.lastIndexOf("/")+1, srcPath.length()))){
    	if(file!= null && toAemFileName(file.getFilename()).equals(srcPath)){
			return true;
		}else{
			return false;
		}
    	
    }
    
    private String getCwLanguageCode(String strLanguage){
		
		String languageCode = strLanguage.replaceAll("_", "-");
		return languageCode;
	
	}
    
    private static void unzipFileFromStream(ZipInputStream zipInputStream, String targetPath) throws IOException {
		File dirFile = new File(targetPath + File.separator);
		if (!dirFile.exists()) {
			dirFile.mkdirs();
			log.trace("Created directory: {}",dirFile);
		}

		ZipEntry zipEntry = null;
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			String zipFileName = zipEntry.getName();
			if (zipEntry.isDirectory()) {
				File zipFolder = new File(targetPath + File.separator + zipFileName);
				if (!zipFolder.exists()) {
					zipFolder.mkdirs();
					log.trace("Created directory: {}",zipFolder);
				}
			} else {
				File file = new File(targetPath + File.separator + zipFileName);

				File parent = file.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}

				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(file);
				} catch (FileNotFoundException e) {
					log.error(e.getLocalizedMessage(),e);
				}
				int readLen = 0;
				byte buffer[] = new byte[1024];
				while (-1 != (readLen = zipInputStream.read(buffer))) {
					fos.write(buffer, 0, readLen);
				}
				fos.close();
			}
		}
		zipInputStream.close();
	}
    
}
