package com.headwire.xliff.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The util class used to zip a page folder and all its content.
 */
public class ZipDirectoryUtil {
	
	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(ZipDirectoryUtil.class);
	
	/** The Constant DEFAULT_BUFFER_SIZE. */
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	
	
	/**
	 * Method that zip a folder.
	 *
	 * @param fileToZip the file to zip
	 * @param zipFile the zip file that generated
	 * @param excludeContainingFolder the exclude containing folder
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void zipFile(String fileToZip, String zipFile, boolean excludeContainingFolder)
		    throws IOException {        
		    
		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));    

		File srcFile = new File(fileToZip);
		if(excludeContainingFolder && srcFile.isDirectory()) {
		    for(String fileName : srcFile.list()) {
		      addToZip("", fileToZip + "/" + fileName, zipOut);
		    }
		} else {
		    addToZip("", fileToZip, zipOut);
		}

		zipOut.flush();
		zipOut.close();

		LOG.debug("Successfully created " + zipFile);
		
	}

	/**
	 * Method that add file/directory to zip.
	 *
	 * @param path the path
	 * @param srcFile the src file
	 * @param zipOut the zip out
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void addToZip(String path, String srcFile, ZipOutputStream zipOut)
	    throws IOException {        
	
		File file = new File(srcFile);
		String filePath = "".equals(path) ? file.getName() : path + "/" + file.getName();
		if (file.isDirectory()) {
		   for (String fileName : file.list()) {             
			   addToZip(filePath, srcFile + "/" + fileName, zipOut);
		   }
		   } else {
		       zipOut.putNextEntry(new ZipEntry(filePath));
		       FileInputStream in = new FileInputStream(srcFile);

		       byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		       int len;
		       while ((len = in.read(buffer)) != -1) {
		    	   zipOut.write(buffer, 0, len);
		       }

		       in.close();
		   }
    }
	
	/**
	 * Method that downloads zip.
	 *
	 * @param response the response
	 * @param zipFile the zip file
	 * @param pageFolderName the page folder name
	 */
	public static void downloadZip(SlingHttpServletResponse response, File zipFile, String pageFolderName){
		
		response.setContentType("application/zip");   
        response.setContentLength((int)zipFile.length());   
        response.setHeader("Content-Disposition","attachment;filename=\"" +  pageFolderName.replaceAll("/", "_") + ".zip" + "\"");  
        byte[] arBytes = new byte[(int)zipFile.length()];   
        FileInputStream is;
		try {
			is = new FileInputStream(zipFile);
			is.read(arBytes);   
	        ServletOutputStream op = response.getOutputStream();   
	        op.write(arBytes);   
	        op.flush(); 
	        is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}   
        
        
	}
	
	
	/**
	 * Method that deletes temp files.
	 *
	 * @param directoryToZip the directory to zip
	 * @param zipFile the zip file
	 */
	public static void deleteTempFiles(File directoryToZip, File zipFile){
		
		try{
			FileUtils.deleteDirectory(directoryToZip);
			FileUtils.forceDelete(zipFile);
		}catch(Exception e){
			LOG.error("delete file error:" + e);
		}
	}


}
