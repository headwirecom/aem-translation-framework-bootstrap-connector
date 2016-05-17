package com.headwire.translation.connector.cloudwords.core.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.filterwriter.XLIFFWriterParameters;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.filters.html.HtmlFilter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.headwire.xliff.util.FileUtil;
import com.headwire.xliff.util.XliffUtil;


/**
 * The Class XliffExporter that reads in source xml file, 
 * converts it to xliff format file, and returns the File object of
 * xliff file.
 */
public class XliffExporter {
	
	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(XliffExporter.class);
	
	/** The content event types. */
	private static Set<EventType> contentEventTypes = EnumSet.of(EventType.TEXT_UNIT, EventType.START_GROUP, EventType.END_GROUP, EventType.DOCUMENT_PART);
	
	/** The sourcePath attribute name. */
	private static String SOURCE_PATH = "sourcePath";
	
	/**
	 * Method that converts xml file to xliff file, returns a File object of xliff file.
	 *
	 * @param filePath the file path
	 * @param sourceLanguage the source language
	 * @param targetLanguage the target language
	 * @return the file
	 */
	public static File convertXmlToXliff(InputStream is, String filePath, String sourceLanguage, String targetLanguage){
		
		// Read source xml file
		Document doc = readXmlFile(is);
		
		// Return null if source xml doc doesn't have any <property> element for translation
		if( isSourceDocEmpty(doc)){
			return null;
		};
					    		        	    
		// Generate xliff file
		if(null != doc){
			return exportXliff(doc, FileUtil.getFileBaseName(filePath), sourceLanguage, targetLanguage);
		} else{
			LOG.error("Source XML file not exists exception");
			return null;
		}
		
	}
	
	private static boolean isSourceDocEmpty(Document doc){
		
		if(doc.getElementsByTagName("property").getLength() == 0){
			return true;
		}else{
			return false;
		}
	}
	
	
	/**
	 * Method that reads xml file and returns a Document Object.
	 *
	 * @param filePath the file path to the xml file
	 * @return the document object that represents the xml file
	 */
	private static Document readXmlFile(InputStream is){
		
		try {
			//File xmlFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    	Document doc = dBuilder.parse(is);
	    	//optional, but recommended
	    	doc.getDocumentElement().normalize();
	    	return doc;
		} catch (ParserConfigurationException e) {
			LOG.error("Read Xml ParserConfigurationException: {}", e);
		} catch (SAXException e) {
			LOG.error("Read Xml SAXException: {}", e);
		} catch (IOException e) {
			LOG.error("Read Xml IOException: {}", e);
		}
		return null;
		
	}
	
	
	/**
	 * Method that creates xliff file from given xml Document object.
	 *
	 * @param doc the docs
	 * @param sourceLanguage the source language
	 * @param targetLanguage the target language
	 * @return Generated xliff file object
	 */
	private static File exportXliff(Document doc, String xliffName, String sourceLanguage, String targetLanguage) {
		
		HtmlFilter inputFilter = new HtmlFilter();
		String fakeFile = "";
		File tempFile = null;
		try {
			//tempFile = File.createTempFile(xliffName, ".xlf");
			tempFile = new File(System.getProperty("java.io.tmpdir") + xliffName + ".xlf");
			fakeFile = tempFile.getAbsolutePath();
			LOG.debug("fakefile:" + fakeFile);
		} catch(Exception e) {
			LOG.error("Failed to create temp file for cloudwords", e);
		}
		
		// make XliffWriter
		XLIFFWriter writer = new XLIFFWriter();
		XLIFFWriterParameters params = new XLIFFWriterParameters();
        params.setIncludeCodeAttrs(true);
        writer.setParameters(params);
        writer.setOutput(fakeFile);
        writer.setOptions(LocaleId.fromString(targetLanguage), "UTF-8");
        
        LocaleId sourceLocaleId = LocaleId.fromString(sourceLanguage);
        LocaleId targetLocaleId = LocaleId.fromString(targetLanguage);
        
        LOG.debug("Source language is {} and target language is {}",sourceLanguage,targetLanguage);
        		
        // Start xliff document
        writer.create(fakeFile, null, sourceLocaleId, targetLocaleId, 
        		MimeTypeMapper.HTML_MIME_TYPE, getRootPath(doc), null);
        
        // pass along ID so that all trans-units in a file have unique IDs
        long globalId = 1;
        outputTransUnits(doc, sourceLanguage, targetLanguage, "",writer,inputFilter,globalId);
        
        // cleanup, close html input filter and the file output
        inputFilter.close();
        writer.close();
        
        // return Xliff File object        
		return tempFile;
	}
	
	
	/**
	 * Gets the sourcePath attribute from source xml root element, <translationObjectFile>.
	 *
	 * @param doc the doc
	 * @return the root path
	 */
	private static String getRootPath(Document doc){
		Element docRoot = doc.getDocumentElement();
		return docRoot.getAttribute(SOURCE_PATH);
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
	 * Output trans units.
	 *
	 * @param doc the doc
	 * @param sourceLanguage the source language
	 * @param targetLanguage the target language
	 * @param prefix the prefix
	 * @param writer the writer
	 * @param inputFilter the input filter
	 * @param globalId the global id
	 * @return the long
	 */
	private static long outputTransUnits(Document doc, String sourceLanguage, String targetLanguage,
			String prefix, XLIFFWriter writer, HtmlFilter inputFilter, long globalId) {
		
		// asset meta data needs special treat, nodepath is not unique, we need to use node path:propertyName
		boolean isAssetMetaData = isAssetMetaData(doc);
				
		// now, start going through sub component content nodes: calling outputTransUnit
		NodeList nList = doc.getElementsByTagName("property");
		for (int temp = 0; temp < nList.getLength(); temp++) {
	        Node nNode = nList.item(temp);
	        LOG.debug("\nCurrent Element :" + nNode.getNodeName());
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	            Element eElement = (Element) nNode;
	            LOG.debug("Content : " + eElement.getTextContent());
	            String id = isAssetMetaData ? eElement.getAttribute("nodePath")+ "/" + eElement.getAttribute("propertyName") : eElement.getAttribute("nodePath");
				globalId = outputTransUnit(id, eElement.getTextContent(), sourceLanguage, targetLanguage, writer, inputFilter, globalId);
			}
		}
		
		return globalId;
	} 
	
	
	/**
	 * Output trans unit.
	 *
	 * @param id the id
	 * @param value the value
	 * @param sourceLanguage the source language
	 * @param targetLanguage the target language
	 * @param writer the writer
	 * @param inputFilter the input filter
	 * @param globalId the global id
	 * @return the long
	 */
	private static long outputTransUnit(String id, String value,
			String sourceLanguage, String targetLanguage,
			XLIFFWriter writer, HtmlFilter inputFilter, long globalId) {
		
		// now we output this trans-unit
		if(StringUtils.isNotBlank(value) && XliffUtil.isTranslatable(value)) {
			LOG.debug(id+": "+value);
			
			// strip out any messed up characters
			value = XliffUtil.stripInvalidCharacters(value);
			
			inputFilter.open(new RawDocument(value, LocaleId.fromString(sourceLanguage)));
			
			int amt = 0;
			//long tuId = 1;
			
			while(inputFilter.hasNext()) {
				Event event = inputFilter.next();
				amt++;
				LOG.debug("Event#" + amt + ": " + event.getEventType().name());
				if(contentEventTypes.contains(event.getEventType())) {
					if(EventType.TEXT_UNIT.equals(event.getEventType())) {
						TextUnit tu = (TextUnit)event.getResource();
						tu.setId(globalId+"");
						tu.setName(id);
						globalId++;
						LOG.debug("\t"+TextContainer.contentToString(tu.getSource()));
					}
					writer.handleEvent(event);
				}
			}
		}
		// return our updated ID
		return globalId;
	}


}
