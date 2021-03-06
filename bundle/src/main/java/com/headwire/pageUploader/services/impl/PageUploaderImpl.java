package com.headwire.pageUploader.services.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudwords.api.client.CloudwordsCustomerAPI;
import com.cloudwords.api.client.CloudwordsCustomerClient;
import com.cloudwords.api.client.exception.CloudwordsClientException;
import com.cloudwords.api.client.resources.Language;
import com.cloudwords.api.client.resources.SourceDocument;
import com.cloudwords.api.client.resources.TranslatedDocument;
import com.headwire.pageUploader.services.PageUploader;
import com.headwire.pageUploader.services.PageUtil;
import com.headwire.pageUploader.services.ServersideRequestUtil;
import com.headwire.pageUploader.services.ZipDirectoryUtil;
import com.headwire.translation.connector.cloudwords.core.impl.CloudwordsConstants;
import com.headwire.xliff.util.FileUtil;


@org.apache.felix.scr.annotations.Component(metatype = true, label = "PageUploader Implementation", description = "Implements the PageUploader service")
@Service(PageUploaderImpl.class)
@Properties({
	@Property(name = Constants.SERVICE_VENDOR, value = "Headwire.com, Inc."),
	@Property(name = Constants.SERVICE_DESCRIPTION, value = "PageUploader service"),
	@Property(name = CloudwordsConstants.AEM_BASE_URL, value = "http://localhost:4502", label = "AEM Base Url", description = "Put AEM base url with port here.")
})
public class PageUploaderImpl 
	implements PageUploader{
	
	private static final Logger LOG = LoggerFactory.getLogger(PageUploaderImpl.class);
	
	/** The temp folder. */
	private static String tempFolder = FileUtil.getSystemTempFolder();
	
	@Reference(policy=ReferencePolicy.STATIC)
	protected ResourceResolverFactory resourceResolverFactory;

	@Override
	public void uploadPage(ResourceResolver rr, int projectId, String targetLanguageCode, String translationDataEntry, String pagePath, CloudwordsCustomerClient customerClient) {
		
		LOG.trace("LQ: Start uploading translated page zip to CW....");
		LOG.trace("LQ: lang code : " + targetLanguageCode + " translationDataEntry : " + translationDataEntry + " pagePath : " + pagePath);
				
		String pageName = pagePath.substring(pagePath.lastIndexOf("/")+1, pagePath.length());
        String pageFolderName = pagePath.replaceAll("/", "_");
        
        LOG.trace("LQ: pageName : " + pageName + " pageFolderName : " + pageFolderName);
        String serverUrl = getProperty(CloudwordsConstants.AEM_BASE_URL,"");
                
        // Retrieve html content of a page
        String htmlString = getPageHtml(rr, resourceResolverFactory, serverUrl + pagePath + "?wcmmode=disabled");
        		
        // check if 404 status returned
    	if (PageUtil.is404Page(htmlString)){
    		LOG.error("404 error, page not found exception: {}", pagePath);
    	}
    	else {
            // Step 1: Save page and all related scripts, images to a folder
    		PageUtil.savePage(rr, resourceResolverFactory, htmlString, pageName, pageFolderName, tempFolder, serverUrl);
        
    		// Step 2: Zip the entire folder
    		String zipName = pageFolderName.replaceAll("/", "_");
    		File folderFile = new File(tempFolder,zipName);
    		File zipFile = new File(tempFolder,zipName + ".zip");
    		try {
				ZipDirectoryUtil.zipFile(folderFile.getAbsolutePath(), zipFile.getAbsolutePath(), true);
			} catch (IOException e) {
				LOG.error("Caught exception creating page upload zip file at "+zipFile.getAbsolutePath(),e);
				//e.printStackTrace();
			}
    		
    		// Step 3: Todo: Upload Zip file to cloudwords site
    		uploadZip(zipFile, projectId, targetLanguageCode, translationDataEntry, customerClient);
            
    		// Step 4: Delete downloaded file and zip file from temp folder
    		ZipDirectoryUtil.deleteTempFiles(folderFile, zipFile);
    		
    	}
		
		
	}
	
	public void uploadSourcePage(ResourceResolver rr, int projectId, String translationDataEntry, String pagePath, CloudwordsCustomerClient customerClient){
		
		//LOG.trace("LQ: Start uploading page zip to CW...");
		
		String pageName = pagePath.substring(pagePath.lastIndexOf("/")+1, pagePath.length());
        String pageFolderName = pagePath.replaceAll("/", "_");
        String serverUrl = getProperty(CloudwordsConstants.AEM_BASE_URL,"");
        
        //LOG.error("LQ==: Start uploading page zip to CW... page path:" + serverUrl + pagePath + "?wcmmode=disabled");
        // Retrieve html content of a page
        String htmlString = getPageHtml(rr, resourceResolverFactory, serverUrl + pagePath + "?wcmmode=disabled");
        //LOG.error("LQ: html string: " + htmlString);		
        // check if 404 status returned
    	if (PageUtil.is404Page(htmlString)){
    		LOG.error("404 error, page not found exception: {}", pagePath);
    	}
    	else {
            // Step 1: Save page and all related scripts, images to a folder
    		PageUtil.savePage(rr, resourceResolverFactory, htmlString, pageName, pageFolderName, tempFolder, serverUrl);
        
    		// Step 2: Zip the entire folder
    		String zipName = pageFolderName.replaceAll("/", "_");
    		File folderFile = new File(tempFolder,zipName);
    		File zipFile = new File(tempFolder,zipName + ".zip");
    		try {
				ZipDirectoryUtil.zipFile(folderFile.getAbsolutePath(), zipFile.getAbsolutePath(), true);
			} catch (IOException e) {
				LOG.error("Caught exception creating source page upload zip file at "+zipFile.getAbsolutePath(),e);
			}
    		
    		// Step 3: Todo: Upload Zip file to cloudwords site
    		uploadSourceZip(zipFile, projectId, translationDataEntry, customerClient);
            
    		// Step 4: Delete downloaded file and zip file from temp folder
    		//LOG.error("LQ== folderFile: " + folderFile + " zipFile: " + zipFile);
    		//LOG.error("LQ== tempFolder: " + tempFolder);
    		ZipDirectoryUtil.deleteTempFiles(folderFile, zipFile);
    		
    	}
		
	}

	@Override
	public String getPageHtml(ResourceResolver rr, ResourceResolverFactory resourceResolverFactory, String pageUrl) {

		String htmlString = null;
		try {
        	//htmlString = ServersideRequestUtil.doRequestAsString(rr, resourceResolverFactory, pageUrl);
    		InputStream is = ServersideRequestUtil.doRequestAsStream(rr, resourceResolverFactory, pageUrl);
    		StringWriter writer = new StringWriter();
			IOUtils.copy(is, writer, "UTF-8");
			htmlString = writer.toString();
        } catch (Exception e) {
        	e.printStackTrace();
		}
		return htmlString;
	}
	
    private void uploadZip(File zipFile, int projectId, String targetLanguageCode, String translationDataEntry, CloudwordsCustomerClient customerClient){
		
		String cqPageName = translationDataEntry.replace(".xml", ".xlf");
		try {
			
			Language language = new Language(targetLanguageCode);
			
			// todo, match xliff name
			List<TranslatedDocument> docs = customerClient.getTranslatedDocuments(projectId, language);
			//LOG.error("LQ == my docs size is:" + docs.size());
			for(TranslatedDocument doc : docs){
				//LOG.error("LQ == doc file name is:" + doc.getXliff().getFilename());
				//LOG.error("LQ == data entry is: " + cqPageName);
				if(doc.getXliff()!= null) LOG.error("LQ == cw xlif name is:" + doc.getXliff().getFilename());
				if(doc.getXliff() != null && doc.getXliff().getFilename().equals(cqPageName)){
					LOG.trace("find match, uploading zip now");
					//LOG.error("LQ == project id:" + projectId + " language: " + language + " doc id:" + doc.getId() );
					customerClient.addTranslatedDocumentPreview(projectId, language, doc.getId(), zipFile);
				}else{
					LOG.warn("not match, from cw: " + doc.getSourceDocumentId() + "  , from cq: " +  translationDataEntry);
					continue;
				}
			}
			
		    LOG.debug("Page zip uploaded to CW");
		} catch (CloudwordsClientException e) {
			LOG.error("CloudwordsClientException: {}", e);
		}
		
	} 
    
    private void uploadSourceZip(File zipFile, int projectId, String pageName, CloudwordsCustomerClient customerClient){
    	    	
    	String cqPageName = pageName.replace(".xml", ".xlf");
		try {
									
			// match xliff name
			List<SourceDocument> docs = customerClient.getSourceDocuments(projectId);
			//LOG.error("LQ == doc size is: " + docs.size());
			for(SourceDocument doc : docs){
				//if(doc.getXliff()!= null) LOG.error("LQ == cw xliff name is:" + doc.getXliff().getFilename());
				//LOG.error("LQ== cw doc name is:" + doc.getXliff().getFilename());
				if(doc.getXliff()!= null && doc.getXliff().getFilename().equals(cqPageName)){
					//LOG.error("find match, uploading zip now");
					//LOG.error("LQ == project id:" + projectId + " doc id:" + doc.getId() );
					customerClient.addSourceDocumentPreview(projectId, doc.getId(), zipFile);
				}else{
					LOG.warn("not match, from cw: " + doc.getId() + "  , from cq: " +  pageName);
					continue;
				}
			}
			
			LOG.debug("Page zip uploaded to CW");
		} catch (CloudwordsClientException e) {
			LOG.error("CloudwordsClientException: {}", e);
		}
    }
    
    //  private ServiceReference mServiceReference;
    private Map<String,String> properties = new HashMap<String,String>();

    @Activate
    protected void activate( ComponentContext pComponentContext ) {
    	setup( pComponentContext );
    }

    @Modified
    protected void modified( ComponentContext pComponentContext ) {
    	setup( pComponentContext );
    }

    @Deactivate
    protected void deactivate() {
    	properties.clear();
    	//      mServiceReference = null;
    }

    protected void setup( ComponentContext pComponentContext ) {
    	//      mServiceReference = pComponentContext.getServiceReference();
    	Dictionary dictionary = pComponentContext.getProperties();
    	Enumeration<String> e = dictionary.keys();
    	while( e.hasMoreElements() ) {
    		String key = e.nextElement();
    		Object temp = dictionary.get( key );
    		properties.put( key, temp == null ? "" : temp.toString() );
    	}
    }
    
    @Override
	@SuppressWarnings("unchecked")
	public <T> T getProperty(String key, T defaultValue) {
		Object obj = getProperty(key);
		if(obj == null) {
			return defaultValue;
		}
		return (T) obj;
	}
    
    @Override
	public Object getProperty(String key) {
        return properties.get( key );
//        if( mServiceReference != null ) {
//	    	return mServiceReference.getProperty(key);
//        } else {
//            return null;
//        }
	}

}
