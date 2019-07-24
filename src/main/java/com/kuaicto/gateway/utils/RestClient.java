package com.kuaicto.gateway.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;

public class RestClient {
	private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

	@Autowired
	RestTemplate restTemplate;
	
	public String requestQuietly(ServerWebExchange exchange, String url) {
		try {
			return this.request(exchange, url);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return null;
	}
	public String request(ServerWebExchange exchange, String url) {
		final boolean isLB = url.startsWith("lb://");
		if (isLB) {
			url = "http" + url.substring(2);
		}

		String result = null;
		if (isLB) {
			result = getProfileWithRestTemplate(exchange, url);
		} else {
			result = getProfileWithHttpClient(exchange, url);
		}

		return result;
	}
	
	private String getProfileWithRestTemplate(ServerWebExchange exchange, String apiUrl) {
		logger.debug("Requesting with RestTemplate: {}", apiUrl);
		
		HttpHeaders requestHeaders = new HttpHeaders();
		
		requestHeaders.addAll(exchange.getRequest().getHeaders());
		
		// comment below due to already add all headers above
//		String cookieString = getCookieString(exchange.getRequest().getCookies());
//		requestHeaders.add("Cookie", cookieString);
		
		HttpEntity<String> requestEntity = new HttpEntity<String>(requestHeaders);
		logger.debug("HttpEntity: {}", requestEntity.toString());
		
		ResponseEntity<String> response = restTemplate.exchange(
		    apiUrl,
		    HttpMethod.GET,
		    requestEntity,
		    String.class);
		
		logger.debug("Response: {}", response);
		
		if (HttpStatus.OK == response.getStatusCode()) {
			String body = response.getBody();
			return body;
		}
		
		return null;
	}
	private String getProfileWithHttpClient(ServerWebExchange exchange, String apiUrl) {
		logger.debug("Requesting with HttpClient: {}", apiUrl);

		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(apiUrl);

		// headers
		final HttpHeaders headers = exchange.getRequest().getHeaders();
		headers.keySet().forEach(name -> {
			if (name.startsWith("X-SGW-")) {
	        	httpGet.addHeader(name, headers.getFirst(name));
			}
		});
		
		// cookie
		HttpCookie httpCookie = exchange.getAttribute("X-SGW-COOKIE");
		if (httpCookie != null) {
		    httpGet.setHeader("Cookie", httpCookie.getName() + "=" + httpCookie.getValue());
		} else {
	        final MultiValueMap<String, HttpCookie> cookies = exchange.getRequest().getCookies();
	        if (cookies != null) {
	            httpGet.setHeader("Cookie", getCookieString(cookies));
	        }
		}

    	CloseableHttpResponse response = null;
    	try {
			logger.debug("executing request: {}", httpGet);
			response = httpClient.execute(httpGet);
			if (200 == response.getStatusLine().getStatusCode()) {
				String body = EntityUtils.toString(response.getEntity());
				return body;
			}			
		} catch (Exception e) {
			logger.error("ERROR", e.getMessage(), e);
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
			try {
				httpClient.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

		
		return null;
	}
	private String getCookieString(final MultiValueMap<String, HttpCookie> cookies) {
		StringBuilder sb = new StringBuilder();
		Set<Entry<String, List<HttpCookie>>> entrySet = cookies.entrySet();
		for (Entry<String, List<HttpCookie>> entry : entrySet) {
			for (HttpCookie cookie : entry.getValue()) {
				sb.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
			}
		}
		String string = sb.toString();
		return string;
	}

}
