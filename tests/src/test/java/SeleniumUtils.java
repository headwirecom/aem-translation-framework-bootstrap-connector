package test;

import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;

/**
 * The Class SeleniumUtils.
 */
public class SeleniumUtils {
	
	/**
	 * Local login.
	 */
	public static void localLogin(WebDriver driver, String user, String password) {
		driver.findElement(By.id("username")).clear();
    	driver.findElement(By.id("username")).sendKeys(user);
    	driver.findElement(By.id("password")).clear();
    	driver.findElement(By.id("password")).sendKeys(password);
    	driver.findElement(By.id("submit-button")).click();
	}
	
		
	/**
	 * Robot to insert an attachment, given a wait time interval
	 */
	public static void fileAttach(String filePath, long millis) throws Exception{
		
		// robot to mimic file upload
		StringSelection ss = new StringSelection(filePath);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
					
		Robot robot = new Robot();
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_V);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		threadSleep(millis);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		
	}
	
	/**
	 * Method to pause execution for a given time interval
	 * 
	 */
	public static void threadSleep(long millis) throws Exception{
		Thread.sleep(millis);
	}
	
}