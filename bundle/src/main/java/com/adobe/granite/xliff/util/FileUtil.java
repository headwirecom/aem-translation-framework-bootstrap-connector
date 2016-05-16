package com.adobe.granite.xliff.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Utility class that provides various methods to access a File.
 */
public class FileUtil {
	
	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);
	
	
    /** Gets the file base name from a file path.
	 *
	 * @param filePath the file path
	 * @return the file base name
	 */
	public static String getFileBaseName(String filePath){
		
		File file = new File(filePath);
		return FilenameUtils.getBaseName(file.getName());
			
	}
	
	/**
	 * Method that deletes a temp xliff file.
	 *
	 * @param file the file
	 */
	public static void deleteTempFile(File file){
		
		try {
			FileUtils.forceDelete(file);
		} catch (IOException e) {
			LOG.error("Can not delete temp file {}", e);
		}
		
	}
	
	
	/**
	 * Method that force create file.
	 *
	 * @param parentDir the parent dir of the file
	 * @param fileName the file name
	 * @return the file created
	 */
	public static File forceCreateFile(String parentDir, String fileName){
		
		File dir = new File(parentDir);
		dir.mkdirs();
		return new File(dir, fileName);

	}
	
	

}
