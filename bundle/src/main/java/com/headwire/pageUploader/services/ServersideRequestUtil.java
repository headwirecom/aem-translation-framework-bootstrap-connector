package com.headwire.pageUploader.services;

import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.jackrabbit.util.Text; 
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static utility class used to make server side requests that include authentication.
 */
public class ServersideRequestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ServersideRequestUtil.class);
	private ServersideRequestUtil() {};
		
	/**
	 * Does a get request on url and returns body as a string. Be careful about large requests.
	 *
	 * @param resolver The current ResourceResolver
	 * @param resolverFactory The current ResourceResolverFactory service
	 * @param url The url to request
	 * @return The response body as a string
	 */
	public static String doRequestAsString(ResourceResolver resolver, ResourceResolverFactory resolverFactory, String url) throws Exception {
		return doRequestAsString(resolver, resolverFactory, url, -1);
	}

	/**
	 * Does a get request on url and returns body as a string. Be careful about large requests.
	 *
	 * @param resolver The current ResourceResolver
	 * @param resolverFactory The current ResourceResolverFactory service
	 * @param url The url to request
	 * @param timeout The timeout length in milliseconds
	 * @return The response body as a string
	 */
	public static String doRequestAsString(ResourceResolver resolver, ResourceResolverFactory resolverFactory, String url, int timeout) throws Exception {
		return doRequestHelper(resolver, resolverFactory, HttpConstants.METHOD_GET, url, null, timeout).getResponseBodyAsString();
	}

	/**
	 * Does a get request on url and returns body as a stream.
	 *
	 * @param resolver The current ResourceResolver
	 * @param resolverFactory The current ResourceResolverFactory service
	 * @param url The url to request
	 * @return The response body as a stream
	 */
	public static InputStream doRequestAsStream(ResourceResolver resolver, ResourceResolverFactory resolverFactory, String url) throws Exception {
		return doRequestAsStream(resolver, resolverFactory, url, -1);
	}

	/**
	 * Does a get request on url and returns body as a stream.
	 *
	 * @param resolver The current ResourceResolver
	 * @param resolverFactory The current ResourceResolverFactory service
	 * @param url The url to request
	 * @param timeout The timeout length in milliseconds
	 * @return The response body as a stream
	 */
	public static InputStream doRequestAsStream(ResourceResolver resolver, ResourceResolverFactory resolverFactory, String url, int timeout) throws Exception {
		HttpMethod request = doRequestHelper(resolver, resolverFactory, HttpConstants.METHOD_GET, url, null, timeout);
		InputStream is = request.getResponseBodyAsStream();
		if(request.getResponseHeader("Content-Encoding") != null &&
				request.getResponseHeader("Content-Encoding").getValue().equals("gzip"))
		{
			return new GZIPInputStream(is);
		}
		else{
			return is;
		}
	}

	/**
	 * Does a get request on url and returns the complete response detail with status code, body...etc .
	 *
	 * @param resolver The current ResourceResolver
	 * @param resolverFactory The current ResourceResolverFactory service
	 * @param url The url to request
	 * @param timeout The timeout length in milliseconds
	 * @return
	 */
	public static HttpMethod doGetRequest(ResourceResolver resolver, ResourceResolverFactory resolverFactory, String url, int timeout) throws Exception {
		return doRequestHelper(resolver, resolverFactory, HttpConstants.METHOD_GET, url, null, timeout);
	}

	/**
	 * Does a POST request on url and returns the complete response detail with status code, body...etc .
	 *
	 * @param resolver The current ResourceResolver
	 * @param resolverFactory The current ResourceResolverFactory service
	 * @param url The url to request
	 * @param parameters The parameters for the POST request
	 * @param timeout The timeout length in milliseconds
	 * @return
	 */
	public static HttpMethod doPostRequest(ResourceResolver resolver, ResourceResolverFactory resolverFactory, String url, RequestEntity requestEntity, int timeout) throws Exception {
		return doRequestHelper(resolver, resolverFactory, HttpConstants.METHOD_POST, url, requestEntity, timeout);
	}
	
	
	private static String getToken(ResourceResolver resolver, ResourceResolverFactory resolverFactory){
		String token = null;
//		if(token == null || (token != null && (System.currentTimeMillis() - tokenCreatedTime > 60000))){
			// update token
			Session jcrSession = resolver.adaptTo(Session.class);
			Session adminSession = jcrSession;
			try {
				if (resolverFactory != null) {
					Map<String, Object> param = new HashMap<String, Object>();
					param.put(ResourceResolverFactory.SUBSERVICE, "readService");
					param.put(ResourceResolverFactory.USER, "cloudwords-service");
					adminSession = resolverFactory.getServiceResourceResolver(param).adaptTo(Session.class);
				}
			} catch (Exception e) {
	            e.printStackTrace();
	        }
			
			SimpleCredentials credentials = new SimpleCredentials("cloudwords-internal", new char[0]);
			credentials.setAttribute(".token", "");
			String repositoryId = adminSession.getRepository().getDescriptor("crx.cluster.id");
			if (repositoryId == null)
				repositoryId = adminSession.getRepository().getDescriptor("crx.repository.systemid");
			String workspaceId = adminSession.getWorkspace().getName();
			Session session2 = null;
			
			try {
				session2 = adminSession.impersonate(credentials);
			} catch (Exception e) {
			} finally {
				try {
					String value = Text.escape(String.format("%s:%s:%s", new Object[] {repositoryId, credentials.getAttribute(".token"), workspaceId}));
					value = String.format("login-token=%s", new Object[] { value });
					token = value;
				} catch (Exception e) {
	                e.printStackTrace();
	            }

				if (session2 != null)
					session2.logout();
				if (adminSession != jcrSession)
					adminSession.logout();
				
			}
			
			
		return token;
		
	} 

	private static HttpMethod doRequestHelper(ResourceResolver resolver, ResourceResolverFactory resolverFactory, String requestMethod, String url, RequestEntity requestEntity, int timeout)
			throws Exception {

		MultiThreadedHttpConnectionManager conMgr = new MultiThreadedHttpConnectionManager();
		if (timeout > 0) {
			HttpConnectionManagerParams params = new HttpConnectionManagerParams();
			params.setConnectionTimeout(timeout);
			conMgr.setParams(params);
		}

		HttpClient httpClient = new HttpClient(conMgr);
		HttpMethod httpMethod = buildHttpMethod(requestMethod, url, requestEntity);
		//httpMethod.setRequestHeader("Authorization", "Basic Y2xvdWR3b3Jkcy1zZXJ2aWNlOg=="); token for admin
		httpMethod.setRequestHeader("Authorization", "Basic Y2xvdWR3b3Jkcy1pbnRlcm5hbDo="); /* token for cloudwords-internal user  */
		httpMethod.setRequestHeader("Cookie", getToken(resolver, resolverFactory));
		httpClient.executeMethod(httpMethod);
			
        return httpMethod;
	}

	private static HttpMethod buildHttpMethod(String requestMethod, String url, RequestEntity requestEntity) {

		if (HttpConstants.METHOD_POST.equals(requestMethod)) {
			PostMethod postMethod = new PostMethod(url);
			if (requestEntity != null) {
				postMethod.setRequestEntity(requestEntity);
			}
			return postMethod;
		} else {
			GetMethod getMethod = new GetMethod(url);
			getMethod.setFollowRedirects(true);
			return getMethod;
		}
	}
}
