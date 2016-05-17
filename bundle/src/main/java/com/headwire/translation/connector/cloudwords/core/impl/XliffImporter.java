package com.headwire.translation.connector.cloudwords.core.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.steps.xliffkit.reader.TextUnitMerger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import com.cloudwords.api.client.CloudwordsCustomerClient;
import com.cloudwords.api.client.exception.CloudwordsClientException;
import com.cloudwords.api.client.resources.CloudwordsFile;
import com.cloudwords.api.client.resources.Language;
import com.cloudwords.api.client.resources.Project;
import com.cloudwords.api.client.resources.TranslatedDocument;
import com.cloudwords.org.apache.commons.lang3.StringEscapeUtils;



/**
 * The Class XliffImporter that:
 * 1. Reads translated content from an Xliff file
 * 2. Generates target xml, and put back translated content
 * 3. put back html content
 */
public class XliffImporter {
	
	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(XliffImporter.class);
	
	/** The Constant ORIGINAL_SOURCE_PATH_KEY. */
	private static final String ORIGINAL_SOURCE_PATH_KEY = "originalSourcePath";
	
	//private static final String ORIGINAL_FILE_TYPE_KEY = "originalFileType";
	
	/** The Constant TEMP_FOLDER. */
	//private static final String TEMP_FOLDER = System.getProperty("java.io.tmpdir");

	/**
	 * The main method for testing.
	 * Args[0] needs to be a valid cloudwords project ID
	 *
	 * @param args the arguments
	 * @throws CloudwordsClientException the cloudwords client exception
	 */
	public static void main(String args[]) throws CloudwordsClientException {
		
		getTranslatedDocumentBySourcePath(getClient(), args[0], "de", args[1]);
		getProjectStatus(getClient(), args[0]);
		
	}
	
	/**
	 * Method that Gets the CloudwordsCustomerClient, for testing, to be removed.
	 *
	 * @return the client
	 */
	private static CloudwordsCustomerClient getClient() {
		LOG.trace("creating cloudwords client");
		
        // should be set to null instead of a default key (disable these lines and add the ones above)
		String apiKey 	= "a1305993ec5b510f5fc92219956cda03f4cb5c7645eabdcb712c81e789205e2b";
		String endPoint = "https://api-sandbox.cloudwords.com";

		CloudwordsCustomerClient client = new CloudwordsCustomerClient(endPoint, "1.15", apiKey);

		return client;
    }
	
	/**
	 * Gets the project status, for testing, to be removed
	 *
	 * @param client the client
	 * @param strTranslationJobID the str translation job id
	 * @return the project status
	 */
	private static String getProjectStatus(CloudwordsCustomerClient client, String strTranslationJobID){
		
		try {
			Project project = getClient().getProject(Integer.parseInt(strTranslationJobID));
			LOG.debug("code:" + project.getStatus().getCode());
			LOG.debug("display:" + project.getStatus().getDisplay());
			//LOG.debug("TranslationStatus:" + TranslationStatus.fromString("APPROVED"));
			return project.getStatus().getCode();
		} catch (NumberFormatException e) {
			LOG.error("not able to convert id {}",e);
		} catch (CloudwordsClientException e) {
			LOG.error("not able to get cloudwords client {}",e);
		}
		
		return null;
	}
	
	
	/**
	 * Method gets the translated document by source path, for testing, to be removed.
	 *
	 * @param client the client
	 * @param strTranslationJobID the str translation job id
	 * @param targetLanguage the target language
	 * @param sourcePath the source path
	 * @return the translated document by source path
	 */
	private static InputStream getTranslatedDocumentBySourcePath(CloudwordsCustomerClient client, String strTranslationJobID, String targetLanguage, String sourcePath){
		
		try {
			List<TranslatedDocument> translations = getClient().getTranslatedDocuments(Integer.parseInt(strTranslationJobID), new Language(targetLanguage));
			for (TranslatedDocument doc : translations) {
				//if ("delivered".equals(doc.getStatus().getCode())) {
				    CloudwordsFile file = doc.getXliff();
					if(file == null) file = doc.getFile();
					//if(file.getFilename().startsWith(sourcePath.replaceAll("/","_"))){
						LOG.debug("found translated document from CW {}", file.getFilename());
						LOG.debug("document status: {}", doc.getStatus().getCode());
						//return getClient().downloadFileFromMetadata(file);
					//}
				//}
			} 
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (CloudwordsClientException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} 
		
		return null;
		
	}
	
	/**
	 * Method that gets the translated documents, for testing, to be removed.
	 *
	 * @param client the client
	 * @param strTranslationJobID the str translation job id
	 * @param targetLanguage the target language
	 * @return the translated document
	 */
	/*private static List<String> getTranslatedDocuments(CloudwordsCustomerClient client, String strTranslationJobID, String targetLanguage){
    	
		try{
    		
            List<TranslatedDocument> translations = getClient().getTranslatedDocuments(Integer.parseInt(strTranslationJobID), new Language(targetLanguage));
            List<String> xliffList = new ArrayList<String>();
            
    		for (TranslatedDocument doc : translations) {
    			if ("delivered".equals(doc.getStatus().getCode())) {
    				CloudwordsFile file = doc.getXliff();
    				if(file == null){file = doc.getFile();}
    				LOG.debug("file name is:" + file.getFilename());
    				/*File dirPath = new File(TEMP_FOLDER);
    				FileUtils.forceMkdir(dirPath);
    				OutputStream os = new FileOutputStream(new File(dirPath, file.getFilename()));
    				InputStream is = getClient().downloadFileFromMetadata(file);*/
    				//LOG.info("downloading xliff: " + file.getFilename() + " to " + TEMP_FOLDER);
    				//use utility method to download file
    				/*IOUtils.copy(is, os);
    				xliffList.add(TEMP_FOLDER + file.getFilename());
    				is.close();
    				os.close();
               }
           }
    	   return xliffList;
    	} catch(Exception e){
    		LOG.debug(e);
    	}
		return null;
    }*/
	
	
	/**
	 * Method that builds the translated object input stream.
	 *
	 * @param translationObjectInputStream the translation object input stream
	 * @param translatedXliffInputStream the translated xliff input stream
	 * @param srcLang the src lang
	 * @param targetLang the target lang
	 * @return the input stream
	 */
	public static InputStream buildTranslatedObjectStream(InputStream translationObjectInputStream, InputStream translatedXliffInputStream, String srcLang, String targetLang){
		
		InputStream xliffStream = translatedXliffInputStream;
		InputStream objectStream = translationObjectInputStream;
		Map<String,String> translatedContentMap = new HashMap<String,String>();
		Map<String, Element> origXmlProperties = new HashMap<String, Element>();
		String fileType = null;
		
		// get translation data from xliff file
	    try {
	    	origXmlProperties = getSourceXmlPropertyMap(objectStream);
	        translatedContentMap = getTranslationContentMap(xliffStream, origXmlProperties,srcLang, targetLang);
	        LOG.trace("translations: {}", translatedContentMap);
	        objectStream.reset();
	        fileType = getFileType(objectStream);
            xliffStream.close(); 
            objectStream.close();
	    } catch (IOException e) {
	    	LOG.error("I/O exception: {}", e);
	    }
	    
	    // build translated xml file inputstream from xliffData
	    return buildXml(translatedContentMap, origXmlProperties, fileType);
		
		
	}
	
	/*
	 * Method that gets fileType value from original xml file
	 * 
	 * 
	 */
	private static String getFileType(InputStream objectStream){
		
		String fileType = null;
		
		try{
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(objectStream);
			NodeList nList = doc.getElementsByTagName("translationObjectFile");
			
			if(nList != null && nList.getLength() >0){
				Node rootNode = nList.item(0);
				if (rootNode.getNodeType() == Node.ELEMENT_NODE) {
		            Element rootElement = (Element) rootNode;
		            fileType = rootElement.getAttribute("fileType");
		            LOG.debug("file type is: {}", fileType);
				}
			}
		} catch (Exception e){
			LOG.error("Parsing source xml exception: {}", e);
		}
		
		return fileType;
	}
	
	/**
	 * Method that 
	 * 1. traverses a given xliff stream, retrieves all translated content.
	 * 2. combine the translated content with orginal html content
	 * 3. returns a map of {srcPath,textContent}
	 *
	 * @param translatedXliffStream the translated xliff stream
	 * @param origXmlProperties the orig xml properties
	 * @param sourceLanguage the source language
	 * @param targetLanguage the target language
	 * @return the map
	 */
	private static Map<String,String> getTranslationContentMap(InputStream translatedXliffStream, Map<String,Element> origXmlProperties,
			String sourceLanguage, String targetLanguage){
		
		// make a map for HtmlFilters
		Map<String,HtmlFilter> htmlFilterMap = new HashMap<String,HtmlFilter>();
		// make a map for the output streams
		Map<String,ByteArrayOutputStream> outputStreamMap = new HashMap<String,ByteArrayOutputStream>();
		// Get a nodepath,element map from orginal xml file
		Map<String,Element> propertyMap = origXmlProperties;
		
		// make a srcPath,segment map
		Map<String,String> translatedContentMap = new HashMap<String,String>();
		
		XLIFFFilter xlfFilter = new XLIFFFilter();
		
		try{
			xlfFilter.open(new RawDocument(translatedXliffStream, "UTF-8", LocaleId.fromString(sourceLanguage), LocaleId.fromString(targetLanguage)));
			int eventCount = 0;
			String originalSourcePath = null;
			// iterate through translated xliff stream
			while(xlfFilter.hasNext()) {
				Event copyFromEvent = xlfFilter.next();
				eventCount++;
				LOG.debug("Event#"+eventCount+": "+copyFromEvent.getEventType().name());
				
				if(EventType.START_SUBDOCUMENT.equals(copyFromEvent.getEventType())) {
                    // If Start Sub Document has an Original Path then use that one instead of the provided root resource
                    originalSourcePath = copyFromEvent.getStartSubDocument().getName();
                    LOG.debug( "Original Source Path: '{}'", originalSourcePath );
                } else if(EventType.TEXT_UNIT.equals(copyFromEvent.getEventType())) {
					TextUnit copyFromTu = (TextUnit)copyFromEvent.getResource();
                    LOG.debug( "Got Text Unit with Name: '{}'", copyFromTu.getName() );
					if(copyFromTu.isTranslatable()) {
						// find source property
						String srcPath = copyFromTu.getName();
						String srcValue = getProperty(propertyMap,srcPath);
						
						if(srcValue == null) {
							// the source wasn't found
							LOG.debug( "Source value for given path '{}' was not found. Possible content mis-match?", srcPath);
							continue;
						}
						
                        LOG.debug( "Text Unit Source Path: '{}' and Value: '{}'", srcPath, srcValue );
						if(StringUtils.isEmpty(srcValue)) {
							// nothing to do here
							LOG.debug("The value of "+srcPath+" on resource "+srcPath+" was empty.");
							continue;
						}
						
						if(!htmlFilterMap.containsKey(srcPath)) {
                            LOG.debug( "HTML Filter Map Didn't Contain Path: '{}'", srcPath );
							HtmlFilter reverseFilter = new HtmlFilter();
							IFilterWriter genericWriter = reverseFilter.createFilterWriter();
							genericWriter.setOptions(LocaleId.fromString(targetLanguage), "UTF-8");
							
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							genericWriter.setOutput(outputStream);
							
							reverseFilter.open(new RawDocument(srcValue, LocaleId.fromString(sourceLanguage)));
							
							htmlFilterMap.put(srcPath, reverseFilter);
							outputStreamMap.put(srcPath, outputStream);
						}
						
						HtmlFilter htmlFilter = htmlFilterMap.get(srcPath);
						IFilterWriter writer = htmlFilter.getFilterWriter();
						
						TextUnitMerger tuMerger = new TextUnitMerger();
                        tuMerger.setTrgLoc(LocaleId.fromString(targetLanguage));
                        
                        //skip to next text unit 
                        while(htmlFilter.hasNext()) {
                            Event copyToEvent = htmlFilter.next();
                            if(EventType.TEXT_UNIT.equals(copyToEvent.getEventType())) {
                                TextUnit copyToTu = (TextUnit)copyToEvent.getResource();
                                copyToTu.setId(copyFromTu.getId());
                                tuMerger.mergeTargets(copyToTu, copyFromTu);                            
                                writer.handleEvent(copyToEvent);
                                break;
                            } else {
                                writer.handleEvent(copyToEvent);
                            }
                        } 
					}
				}
				
			}
			
			for(Map.Entry<String, HtmlFilter> entry : htmlFilterMap.entrySet()) {
                HtmlFilter filter = entry.getValue();
                IFilterWriter writer = filter.getFilterWriter();
                String srcPath = entry.getKey();
                
                // Finish the remaining events if any
                while(filter.hasNext()) {
                    Event copyToEvent = filter.next();
                    writer.handleEvent(copyToEvent);
                }
                
                filter.close();
                writer.close();
                
                String segment = outputStreamMap.get(srcPath).toString("UTF-8");
                
                translatedContentMap.put(srcPath, segment);
                //LOG.error("srcPath: " + srcPath);
                //LOG.error( "Property Value: '{}'", segment);
                
            }
			translatedContentMap.put(ORIGINAL_SOURCE_PATH_KEY, originalSourcePath);
			
		}catch(Exception e){
			LOG.error("apply translation exception: {}", e);
		}
		
		return translatedContentMap;
		
	}
	
    /**
     * Method that gets the text content of an xml element,
     * by matching nodePath.
     *
     * @param propertyMap the property map
     * @param nodePath the node path
     * @return the text content of an Element node
     */
    private static String getProperty(Map<String, Element> propertyMap, String nodePath){
		
		if(propertyMap.containsKey(nodePath)){
			Element element = propertyMap.get(nodePath);
			return element.getTextContent();
		}
		else{
			return null;
		}
		
	}
    
    private static boolean isAssetMetaData(Document doc){
		
		NodeList rootList = doc.getElementsByTagName("translationObjectFile");
		if(rootList != null){
			Node rootNode = rootList.item(0);
			Element rootElement = (Element) rootNode;
			if(rootElement.hasAttribute("fileType") && rootElement.getAttribute("fileType").equals("ASSETMETADATA")){
				LOG.debug("fileType is ASSETMETADATA");
				return true;
			}
		}
		
		return false;
		
	}
	
	/**
	 * Method that gets the source xml property map.
	 *
	 * @param objectStream the object stream
	 * @return the source xml property map
	 */
	private static Map<String, Element> getSourceXmlPropertyMap(InputStream objectStream){
		
		Map<String, Element> propertyMap = new  LinkedHashMap<String, Element>();
		
		try{
			//File fXmlFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(objectStream);
			NodeList nList = doc.getElementsByTagName("property");
			
			boolean isAssetMetaData = isAssetMetaData(doc);
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
		        if (nNode.getNodeType() == Node.ELEMENT_NODE) {
		            Element eElement = (Element) nNode;
		            //LOG.error("from getSourceXmlPropertyMap:" + eElement.getAttribute("propertyName"));
		            String key = isAssetMetaData? eElement.getAttribute("nodePath") + "/" + eElement.getAttribute("propertyName") : eElement.getAttribute("nodePath");
		            propertyMap.put(key, eElement);
				}
			}
			
			return propertyMap;
		} catch (Exception e){
			LOG.error("Parsing source xml exception: {}", e);
		}
		return null;
	}
	
	
	/**
	 * Method that Builds the target xml file by
	 * combining original xml Element with updated text content
	 * from translatedContentMap.
	 *
	 * @param translatedContentMap the translated content map
	 * @param propertyMap the property map
	 * @return the input stream
	 */
	public static InputStream buildXml(Map<String,String> translatedContentMap, Map<String, Element> propertyMap, String fileType){
		
		try {
			 
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("translationObjectFile");
			doc.appendChild(rootElement);
			
			// set attribute to root element
			Attr attr = doc.createAttribute("sourcePath");
			attr.setValue(translatedContentMap.get(ORIGINAL_SOURCE_PATH_KEY));
			//attr.setValue("/content/geometrixx/en/products/triangle");
			rootElement.setAttributeNode(attr);
			
			// set 2nd attribute to root element
			Attr attr2 = doc.createAttribute("fileType");
			// possible values: PAGE, TAGMETADATA, ASSETMETADATA
			attr2.setValue(fileType);
			rootElement.setAttributeNode(attr2);
	 
			// properties element
			Element properties = doc.createElement("translationObjectProperties");
			rootElement.appendChild(properties);
	 
			// property elements
			for (Map.Entry<String, Element> entry : propertyMap.entrySet()) {
				String srcPath = entry.getKey();
				Element origElement = entry.getValue();
				
				Element property = doc.createElement("property");
				property.setAttribute("isMultiValue", origElement.getAttribute("isMultiValue"));
				property.setAttribute("propertyName", origElement.getAttribute("propertyName"));
				//LOG.error("from buildXml:" + origElement.getAttribute("propertyName"));
				property.setAttribute("nodePath", origElement.getAttribute("nodePath"));
				property.appendChild(doc.createTextNode(StringEscapeUtils.unescapeXml(translatedContentMap.get(srcPath))));

				properties.appendChild(property);
			}
	        
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			Source xmlSource = new DOMSource(doc);
			Result outputTarget = new StreamResult(outputStream);

//			System.out.println("");
//			System.out.println("converted file: ");
//			TransformerFactory.newInstance().newTransformer().transform(xmlSource, new StreamResult(System.out));
//			System.out.println("");

			
			TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
			InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
			
			return is;
		  	 
		  } catch (ParserConfigurationException pce) {
			LOG.error("ParserConfigurationException: {}", pce);
		  } catch (TransformerException tfe) {
			LOG.error("TransformerException: {}", tfe);
		  } 
		
		return null;
	}

}
