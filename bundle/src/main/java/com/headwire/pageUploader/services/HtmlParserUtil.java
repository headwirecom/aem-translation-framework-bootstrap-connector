package com.headwire.pageUploader.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The util class that parses an html page, retrieving embedded documents:
 * css, js, image
 */
public class HtmlParserUtil {
	
	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(HtmlParserUtil.class);
	
	/**
	 * Gets the css files.
	 *
	 * @param rr the ResourceResolver
	 * @param content the content
	 * @param pageFolderName the page folder name
	 * @return the Element that represents a css file
	 */
	public static Element getCssFiles(ResourceResolver rr, ResourceResolverFactory rrf, Element content, String pageFolderName, String serverUrl, String tempFolder){
			
		Elements links = content.getElementsByTag("link");
        for (Element link : links) {
          String linkHref = link.attr("href");
          String linkType = link.attr("type");
          if(linkType.equals("text/css")){
        	  try {
        		LOG.debug("link: " + linkHref + " linkType: " + linkType);
				//String cssString = ServersideRequestUtil.doRequestAsString(rr, rrf, serverUrl + linkHref);
        		InputStream is = ServersideRequestUtil.doRequestAsStream(rr, rrf, serverUrl + linkHref);
        		  
				StringWriter writer = new StringWriter();
				IOUtils.copy(is, writer, "UTF-8");
				String cssString = writer.toString();
				// Todo, parse Css string, find all url link
				getUrlInCss(rr, rrf, linkHref.substring(0, linkHref.lastIndexOf("/")+1),cssString, pageFolderName, serverUrl, tempFolder);
				saveTextFile(cssString, linkHref.substring(0,linkHref.lastIndexOf("/")),linkHref.substring(linkHref.lastIndexOf("/"),linkHref.length()), pageFolderName, tempFolder);
				// rewrite href, insert "assets" before url
				link.attr("href", "assets" + linkHref);
			} catch (Exception e) {
				e.printStackTrace();
			}
        	  
          }
        }
        return content;    
		
	}
	
	public static String getUrlInCss(ResourceResolver rr, ResourceResolverFactory rrf,String cssPath, String cssString, String pageFolderName, String serverUrl, String tempFolder){
		
		Pattern p = Pattern.compile("url\\(\\s*(['\"]?+)(.*?)\\1\\s*\\)");
		Matcher m = p.matcher(cssString);
		String imgSrc = null;
		while (m.find()) {
		  imgSrc = m.group(2);
		  try {
			saveImgFile(ServersideRequestUtil.doRequestAsStream(rr, rrf, serverUrl + cssPath + imgSrc), cssPath + imgSrc.substring(0, imgSrc.lastIndexOf("/")),imgSrc.substring(imgSrc.lastIndexOf("/"),imgSrc.length()), pageFolderName, tempFolder);
		} catch (Exception e) {
			e.printStackTrace();
		}
		}
		return imgSrc;
	}
	
	/**
	 * Gets the JS files.
	 *
	 * @param rr the rr
	 * @param content the content
	 * @param pageFolderName the page folder name
	 * @return the Element object that represents a JS file
	 */
	 public static Element getJSFiles(ResourceResolver rr, ResourceResolverFactory rrf, Element content, String pageFolderName, String serverUrl, String tempFolder){
		
		Elements scripts = content.getElementsByTag("script");
        for (Element script : scripts) {
          String scriptSrc = script.attr("src");
          String scriptType = script.attr("type");
          if(scriptType.equals("text/javascript") && ! "".equals(scriptSrc)){
        	  try{
        		  
        		  LOG.trace("script src: " + scriptSrc + " script Type: " + scriptType);
        		  InputStream is = ServersideRequestUtil.doRequestAsStream(rr, rrf, serverUrl + scriptSrc);
        		          		  
        		  StringWriter writer = new StringWriter();
        		  IOUtils.copy(is, writer, "UTF-8");
        		  String jsString = writer.toString();
        		  
        		  // if the script is: assets/etc/clientlibs/browsermap.js, need to comment out newUrl function
        		  if(scriptSrc.endsWith("browsermap.js")){
        			  jsString = jsString.replace("window.location = newURL;", "//window.location = newURL;");
        		  }
        		  
        		  // If script type is html, need to get embedded image in html src
        		  if(scriptSrc.endsWith(".html")){
        			  jsString = rewriteHtmlPage(rr, jsString, pageFolderName);
        			  LOG.trace("find embedded html page...");
        		  }
        		  
        		  saveTextFile(jsString, scriptSrc.substring(0, scriptSrc.lastIndexOf("/")),scriptSrc.substring(scriptSrc.lastIndexOf("/"),scriptSrc.length()), pageFolderName, tempFolder);
        		  // rewrite href, insert "assets" before url
  				  script.attr("src", "assets" + scriptSrc);
        	  }catch (Exception e) {
  				  LOG.error("get js file error:" + e);
  			}
          }
          
        }
        return content;
	}
	
	public static String rewriteHtmlPage(ResourceResolver rr, String htmlString, String pageFolderName){
		
		String html = null;
		
		String pattern="(<img src=\")(/[^\"\n]+)(\")";
    	Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
    	Matcher m = p.matcher(htmlString);
    	if(m.find()){ 
    		html = m.replaceAll("$1assets$2$3");
    	}
		
		return html;
		
	}
	
	
    /**
     * Gets the client lib script files.
     *
     * @param rr the ResourceResolver
     * @param content the content
     * @param pageFolderName the page folder name
     * @return the page html content with updated client lib scripts
     */
    public static String getClientLibScriptFiles(ResourceResolver rr, ResourceResolverFactory rrf, String content, String pageFolderName, String serverUrl, String tempFolder){
		
    	String htmlString = null;
    	
    	String pattern="(\"p\":\")(/[^\"\n]+)(\")";
    	Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
    	Matcher m = p.matcher(content);
    	while(m.find()){ 
    		try {
    			LOG.trace("saving: " + m.group(2));
        		String scriptSrc = m.group(2);
        		InputStream is = ServersideRequestUtil.doRequestAsStream(rr, rrf, serverUrl + scriptSrc);
        		StringWriter writer = new StringWriter();
      		    IOUtils.copy(is, writer, "UTF-8");
      		    String scriptString = writer.toString();
				
      		    // Get embed images from css file
      		    if(scriptSrc.substring(scriptSrc.lastIndexOf("/")).endsWith(".css")){
      		    	getUrlInCss(rr, rrf, scriptSrc.substring(0, scriptSrc.lastIndexOf("/")+1),scriptString, pageFolderName, serverUrl, tempFolder);
      		    }
      		    
				saveTextFile(scriptString, scriptSrc.substring(0, scriptSrc.lastIndexOf("/")),scriptSrc.substring(scriptSrc.lastIndexOf("/"),scriptSrc.length()), pageFolderName, tempFolder);
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	htmlString = m.replaceAll("$1assets$2$3");
		return htmlString;
	}
	
	/**
	 * Gets the img files.
	 *
	 * @param rr the rr
	 * @param content the content
	 * @param pageFolderName the page folder name
	 * @return the Element that represents img file
	 */
	public static Element getImgFiles(ResourceResolver rr, ResourceResolverFactory rrf, Element content, String pageFolderName, String serverUrl, String tempFolder){
		
		Elements images = content.getElementsByTag("img");
        for (Element img : images) {
          String imgSrc = img.attr("src");
          try {
        	LOG.trace("image src: " + imgSrc);
			saveImgFile(ServersideRequestUtil.doRequestAsStream(rr, rrf, serverUrl + imgSrc), imgSrc.substring(0, imgSrc.lastIndexOf("/")),imgSrc.substring(imgSrc.lastIndexOf("/"),imgSrc.length()), pageFolderName, tempFolder);
			// rewrite href, insert "assets" before url
			img.attr("src", "assets" + imgSrc);
			// todo, need to get image from style attribute
			String styleSrc = img.attr("style");
			if(! "".equals(styleSrc)){
				String imgUrl = getUrlInCss(rr, rrf, "", styleSrc, pageFolderName, serverUrl, tempFolder);
				img.attr("style", styleSrc.replace(imgUrl, "assets" + imgUrl));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
          
        }
        return content;
		
	}
	
	/**
	 * Save text based file.
	 *
	 * @param text the text
	 * @param path the path
	 * @param fileName the file name
	 * @param pageFolderName the page folder name
	 */
	public static void saveTextFile(String text, String path, String fileName, String pageFolderName, String tempFolder){
		try {
			String filePath = null;
			if(path.equals("")) {filePath = tempFolder + "/" + pageFolderName + "/";}
			else {filePath = tempFolder + "/" + pageFolderName + "/assets" + path;}
			FileUtils.forceMkdir(new File(filePath));
			FileUtils.writeStringToFile(new File(filePath + fileName), text, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Save img file.
	 *
	 * @param inputStream the input stream
	 * @param path the path
	 * @param fileName the file name
	 * @param pageFolderName the page folder name
	 */
	public static void saveImgFile(InputStream inputStream, String path, String fileName, String pageFolderName, String tempFolder){
		LOG.trace("image path:" + path + " image name:" + fileName);
		OutputStream outputStream = null;
		
		try {
			String filePath = tempFolder + "/" + pageFolderName + "/assets" + path;
			//String filePath = pageFolderName + "/assets" + path;
			FileUtils.forceMkdir(new File(filePath));
			
			// write the inputStream to a FileOutputStream
			outputStream = new FileOutputStream(new File(filePath + fileName));
	 
			byte[] b = new byte[2048];
			int length;

			while ((length = inputStream.read(b)) != -1) {
				outputStream.write(b, 0, length);
			}
	 
			 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	 
			}
		}

		
	}

}
