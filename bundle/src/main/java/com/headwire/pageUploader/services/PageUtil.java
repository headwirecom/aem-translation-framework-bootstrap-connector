package com.headwire.pageUploader.services;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class PageUtil {
	
	/**
	 * Checks if a page is 404 page.
	 *
	 * @param htmlString the html string
	 * @return true, if is 404 page
	 */
	public static boolean is404Page(String htmlString){
		
		Document doc = Jsoup.parse(htmlString);
		if(doc.getElementsByTag("head") != null && doc.getElementsByTag("head").get(0).getElementsByTag("title") != null){
			for (Element title : doc.getElementsByTag("head").get(0).getElementsByTag("title")) {
				if(title.ownText().equals("404 No resource found")){
					return true;
				}
			}
		}
		
		return false;
		
	}
	
	/**
	 * Method that saves a static page with all its dependencies.
	 *
	 * @param rr the rr
	 * @param htmlString the html string
	 * @param pageName the page name
	 * @param pageFolderName the page folder name
	 */
	public static void savePage(ResourceResolver rr, ResourceResolverFactory rrf, String htmlString, String pageName, String pageFolderName, String tempFolder, String serverUrl){
		
		//original html page
		//HtmlParserUtil.saveTextFile(htmlString,"","orig_" + pageName, pageFolderName, tempFolder);
 			
		// Get embeded files: css, js, image
		String html = null;
		
		try {
			InputStream is;
			is = new ByteArrayInputStream(htmlString.getBytes("UTF-8"));
			BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"), 4*1024);
			StringBuilder total = new StringBuilder();
			String line = "";
			while ((line = r.readLine()) != null) {
			     total.append(line);
			}
			r.close();
			is.close();
			html = total.toString();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Document doc = Jsoup.parse(html);
		
		Element htmlContent = doc.getElementsByTag("html").get(0);
		Element revisedContent1 = HtmlParserUtil.getCssFiles(rr, rrf, htmlContent, pageFolderName, serverUrl, tempFolder);
		Element revisedContent2 = HtmlParserUtil.getJSFiles(rr, rrf, revisedContent1, pageFolderName, serverUrl, tempFolder);
		Element revisedContent3 = HtmlParserUtil.getImgFiles(rr, rrf, revisedContent2, pageFolderName, serverUrl, tempFolder);
		String updatedHtmlString = HtmlParserUtil.getClientLibScriptFiles(rr, rrf, revisedContent3.html(), pageFolderName, serverUrl, tempFolder);
		
		// save updated html page, with css, js path rewritten
		HtmlParserUtil.saveTextFile(updatedHtmlString,"",pageName, pageFolderName, tempFolder);
	}

}
