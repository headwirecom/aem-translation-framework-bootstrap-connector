package test;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;

public class SiteProjectCreationTest {
	
	private WebDriver driver;
	private boolean acceptNextAlert = true;
	private StringBuffer verificationErrors = new StringBuffer();
	
	private String user;
	private String password;
	private String loginUrl;
	private String sitesUrl;
	private String projectsUrl;
	private boolean isLocalTest;
	private long shortPause;
	
	
	@Before
	public void setUp() throws Exception {
		
		// get system properties
		user = System.getProperty("user");
		password = System.getProperty("pwd");
		loginUrl = System.getProperty("loginurl");
		sitesUrl = System.getProperty("sitesurl");
		projectsUrl = System.getProperty("projectsurl");
		isLocalTest = Boolean.valueOf(System.getProperty("islocaltest"));
		shortPause = Long.valueOf(System.getProperty("shortpause")).longValue();
						
		System.out.println("user: " + user);
		System.out.println("password: " + password);
		System.out.println("loginUrl: " + loginUrl);
		System.out.println("sitesUrl: " + sitesUrl);
		System.out.println("projectsUrl: " + projectsUrl);
		System.out.println("isLocalTest: " + isLocalTest);
				
		driver = new FirefoxDriver();
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	}

	@Test
	public void testSiteProjectCreationTest() throws Exception {
	  
		// login to AEM	  
        driver.get(loginUrl);
        
        if(isLocalTest){
        	// local login
        	SeleniumUtils.localLogin(driver, user, password);
        } 
        
        		
		// assertion
		//assertTrue("test", "test");
        
	}

	@After
	public void tearDown() throws Exception {
		driver.quit();
		String verificationErrorString = verificationErrors.toString();
		if (!"".equals(verificationErrorString)) {
			fail(verificationErrorString);
		}
	}

	private boolean isElementPresent(By by) {
		try {
			driver.findElement(by);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	private boolean isAlertPresent() {
		try {
			driver.switchTo().alert();
			return true;
		} catch (NoAlertPresentException e) {
			return false;
		}
	}

	private String closeAlertAndGetItsText() {
		try {
			Alert alert = driver.switchTo().alert();
			String alertText = alert.getText();
			if (acceptNextAlert) {
				alert.accept();
			} else {
				alert.dismiss();
			}
			return alertText;
		} finally {
			acceptNextAlert = true;
		}
	}
}
