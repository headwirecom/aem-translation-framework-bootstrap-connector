package com.adobe.granite.xliff.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class XliffUtil.
 */
public class XliffUtil {
	
	private static final Logger LOG = LoggerFactory.getLogger(XliffUtil.class);

	public static final String INVALID_XML_PATTERN = "[^"
            + "\u0009\r\n"
            + "\u0020-\uD7FF"
            + "\uE000-\uFFFD"
            + "\ud800\udc00-\udbff\udfff"
            + "]";
	
	public static final String JCR_PATH_REGEX = 
			"^(/[+~%.\\w-_]+)+$";
	
	public static final String URL_REGEX = ""
			+ "^https?\\://"					// grab http or https
			+ "([\\-_\\w.]+)"					// match a domain (no checking for validity)
			+ "(/[+~%.\\w-_]*)*"				// get the full path
			+ "\\??(?:[\\-\\+=&;%@\\.\\w]*)"	// match a possible query string
			+ "#?(?:[\\.\\!\\/\\w\\\\]*)$";		// match a possible anchor
	
	private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
	private static final Pattern JCR_PATTERN = Pattern.compile(JCR_PATH_REGEX);
	
	/**
	 * Xliff escape html.
	 *
	 * @param val the val
	 * @return the string
	 */
	public static String xliffEscapeHtml(String val) {
		StringBuilder esc = new StringBuilder();
		
		int idx = 0;
		int lt = -1;
		while((lt = val.indexOf('<',idx)) > -1) {
			// move all text before this tag into the builder
			esc.append(val.substring(idx,lt));
			int gt = val.indexOf('>',lt);
			if(gt > -1) {
				// add the bpt tag
				esc.append("<bpt>");
				String tagToEscape = val.substring(lt,gt+1);
				// do html escape on this text
				esc.append(tagToEscape);
				esc.append("</bpt>");
			}
			else {
				// no end of the tag found, not sure what to do
			}
		}
		
		return esc.toString();
	}
	
	/**
	 * Helper function to determine if a string should be translated.
	 * Currently, it will return false if the String meets any of the
	 * following conditions:
	 * 1. If it is empty after calling .trim();
	 * 2. If it can be parsed into an Integer or Double
	 * 3. If it is a jcr path
	 * 4. If it is an absolute URL (http, or https only)
	 * 
	 * In all other cases this will return true.
	 * 
	 * @param s The string to check
	 * @return true if it doesn't meet any safeguards, false otherwise
	 */
	public static boolean isTranslatable(String s) {
		boolean b = isTranslatableHelper(s);
		if(LOG.isDebugEnabled() && !b) {
			LOG.debug("not translatable: "+s);
		}
		return b;
	}
	
	private static boolean isTranslatableHelper(String s) {
		if(s == null || s.equals("")) {
			return false;
		}
		String str = s.trim();
		if(str.equals("")) {
			return false;
		}
		// check for a number
		try {
			Double d = Double.valueOf(str);
			if(d != null) {
				// we have a number, so don't translate
				return false;
			}
		} catch(Exception e) {
			// this doesn't matter, move on
		}
		// check for a JCR path
		Matcher jcr = JCR_PATTERN.matcher(str);
		if(jcr.find()) {
			if(jcr.group().equals(str)) {
				// it found a match and it was the whole string, so its a path
				return false;
			}
		}
		Matcher url = URL_PATTERN.matcher(str);
		if(url.find()) {
			if(url.group().equals(str)) {
				// it found a match and it was the whole string, so its a url
				return false;
			}
		}
		return true;
	}
	
	/**
	 * This function removes any characters that aren't valid in XML 1.0.
	 * Sometimes they creep into editors and get saved in the repository
	 * and are never noticed until we try to send the text off for
	 * translation.
	 * 
	 * These characters are:
	 * \u0009, \u0020-\uD7FF, \uE000-\uFFFD, \ud800\udc00-\udbff\udfff
	 * 
	 * @param input String to strip
	 * @return the input String with any invalid characters removed
	 */
	public static String stripInvalidCharacters(String input) {
		return input.replaceAll(INVALID_XML_PATTERN, "");
	}
	
	/**
	 * Gets the entity defs.
	 *
	 * @return the entity defs
	 */
	public static String getEntityDefs() {
		StringBuilder sb = new StringBuilder();
		for(String[] entity : ENTITIES) {
			sb.append("<!ENTITY ");
			sb.append(entity[1]);
			sb.append("  \"");
			sb.append(entity[0]);
			sb.append("\">\n");
		}
		return sb.toString();
	}
	
	/**
	 * The Enum State.
	 */
	enum State {
		
		/** The intext. */
		INTEXT, 
 /** The intag. */
 INTAG
	}
	
	/** The entities. */
	public static String[][] ENTITIES = {
		{"&#160;","&nbsp;"},
		{"&#161;","&iexcl;"},
		{"&#162;","&cent;"},
		{"&#163;","&pound;"},
		{"&#164;","&curren;"},
		{"&#165;","&yen;"},
		{"&#166;","&brvbar;"},
		{"&#167;","&sect;"},
		{"&#168;","&uml;"},
		{"&#169;","&copy;"},
		{"&#170;","&ordf;"},
		{"&#171;","&laquo;"},
		{"&#172;","&not;"},
		{"&#173;","&shy;"},
		{"&#174;","&reg;"},
		{"&#175;","&macr;"},
		{"&#176;","&deg;"},
		{"&#177;","&plusmn;"},
		{"&#178;","&sup2;"},
		{"&#179;","&sup3;"},
		{"&#180;","&acute;"},
		{"&#181;","&micro;"},
		{"&#182;","&para;"},
		{"&#183;","&middot;"},
		{"&#184;","&cedil;"},
		{"&#185;","&sup1;"},
		{"&#186;","&ordm;"},
		{"&#187;","&raquo;"},
		{"&#188;","&frac14;"},
		{"&#189;","&frac12;"},
		{"&#190;","&frac34;"},
		{"&#191;","&iquest;"},
		{"&#192;","&Agrave;"},
		{"&#193;","&Aacute;"},
		{"&#194;","&Acirc;"},
		{"&#195;","&Atilde;"},
		{"&#196;","&Auml;"},
		{"&#197;","&Aring;"},
		{"&#198;","&AElig;"},
		{"&#199;","&Ccedil;"},
		{"&#200;","&Egrave;"},
		{"&#201;","&Eacute;"},
		{"&#202;","&Ecirc;"},
		{"&#203;","&Euml;"},
		{"&#204;","&Igrave;"},
		{"&#205;","&Iacute;"},
		{"&#206;","&Icirc;"},
		{"&#207;","&Iuml;"},
		{"&#208;","&ETH;"},
		{"&#209;","&Ntilde;"},
		{"&#210;","&Ograve;"},
		{"&#211;","&Oacute;"},
		{"&#212;","&Ocirc;"},
		{"&#213;","&Otilde;"},
		{"&#214;","&Ouml;"},
		{"&#215;","&times;"},
		{"&#216;","&Oslash;"},
		{"&#217;","&Ugrave;"},
		{"&#218;","&Uacute;"},
		{"&#219;","&Ucirc;"},
		{"&#220;","&Uuml;"},
		{"&#221;","&Yacute;"},
		{"&#222;","&THORN;"},
		{"&#223;","&szlig;"},
		{"&#224;","&agrave;"},
		{"&#225;","&aacute;"},
		{"&#226;","&acirc;"},
		{"&#227;","&atilde;"},
		{"&#228;","&auml;"},
		{"&#229;","&aring;"},
		{"&#230;","&aelig;"},
		{"&#231;","&ccedil;"},
		{"&#232;","&egrave;"},
		{"&#233;","&eacute;"},
		{"&#234;","&ecirc;"},
		{"&#235;","&euml;"},
		{"&#236;","&igrave;"},
		{"&#237;","&iacute;"},
		{"&#238;","&icirc;"},
		{"&#239;","&iuml;"},
		{"&#240;","&eth;"},
		{"&#241;","&ntilde;"},
		{"&#242;","&ograve;"},
		{"&#243;","&oacute;"},
		{"&#244;","&ocirc;"},
		{"&#245;","&otilde;"},
		{"&#246;","&ouml;"},
		{"&#247;","&divide;"},
		{"&#248;","&oslash;"},
		{"&#249;","&ugrave;"},
		{"&#250;","&uacute;"},
		{"&#251;","&ucirc;"},
		{"&#252;","&uuml;"},
		{"&#253;","&yacute;"},
		{"&#254;","&thorn;"},
		{"&#255;","&yuml;"}
	};

}
