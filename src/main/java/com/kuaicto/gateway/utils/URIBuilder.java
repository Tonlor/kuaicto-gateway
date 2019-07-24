package com.kuaicto.gateway.utils;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;

public abstract class URIBuilder {
	/**
	 * 合并URI
	 * @param base
	 * @param optional
	 * @return
	 */
	public static URI MergeURI(final URI base, final String optional) {
		try {
			return MergeURI(base, new URI(optional));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	public static URI MergeURI(final URI base, final URI optional) {
		StringBuilder uriBuilder = new StringBuilder()
			.append(StringUtils.isNotBlank(optional.getScheme()) ? optional.getScheme() : base.getScheme())
			.append("://")
			.append(StringUtils.isNotBlank(optional.getHost()) ? optional.getHost() : base.getHost())
			.append(":")
			.append(optional.getPort() != -1 ? optional.getPort() : base.getPort());
		
		// Path
		if (StringUtils.isNotBlank(optional.getRawPath())) {
			uriBuilder.append(optional.getRawPath());
		}
		else if (StringUtils.isNotBlank(base.getRawPath())) {
			uriBuilder.append(base.getRawPath());
		}
		
		// Query
		if (StringUtils.isNotBlank(optional.getRawQuery())) {
			uriBuilder
			.append("?")
			.append(optional.getRawQuery());
		}
		else if (StringUtils.isNotBlank(base.getRawQuery())) {
			uriBuilder
			.append("?")
			.append(base.getRawQuery());
		}
		
		// Fragment 
		if (StringUtils.isNotBlank(optional.getFragment())) {
			uriBuilder
			.append("#")
			.append(optional.getFragment());
		}
		else if (StringUtils.isNotBlank(base.getFragment())) {
			uriBuilder
			.append("#")
			.append(base.getFragment());
		}
		
		
		try {
			URI newURI = new URI(uriBuilder.toString());
			return newURI;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
