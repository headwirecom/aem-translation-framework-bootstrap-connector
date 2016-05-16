package com.adobe.granite.xliff.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;

public class SearchAndReplaceInputStreamUtil {
	
	public static InputStream searchAndReplace(InputStream is, String search, String replace){
		
		String content = convertStreamToString(is);
		content = content.replaceAll(search, replace);
		return convertStringToStream(content);
		
		
	}
	private static String convertStreamToString(InputStream is) {
		try{
	    Scanner s = new Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
		} finally{
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    
	}
	
	private static InputStream convertStringToStream (String string){
		try {
			return IOUtils.toInputStream(string, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
