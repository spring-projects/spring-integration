/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Default {@link HeaderMapper} implementation for HTTP.
 *
 * @author Mark Fisher
 * @author Jeremy Grelle
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class DefaultHttpHeaderMapper implements HeaderMapper<HttpHeaders>, BeanFactoryAware, InitializingBean{

	private static final Log logger = LogFactory.getLog(DefaultHttpHeaderMapper.class);

	private volatile ConversionService conversionService;

	private volatile BeanFactory beanFactory;

	private static final String ACCEPT = "Accept";

	private static final String ACCEPT_CHARSET = "Accept-Charset";

	private static final String ACCEPT_ENCODING = "Accept-Encoding";

	private static final String ACCEPT_LANGUAGE = "Accept-Language";

	private static final String ACCEPT_RANGES = "Accept-Ranges";

	private static final String AGE = "Age";

	private static final String ALLOW = "Allow";

	private static final String AUTHORIZATION = "Authorization";

	private static final String CACHE_CONTROL = "Cache-Control";

	private static final String CONNECTION = "Connection";

	private static final String CONTENT_ENCODING = "Content-Encoding";

	private static final String CONTENT_LANGUAGE = "Content-Language";

	private static final String CONTENT_LENGTH = "Content-Length";

	private static final String CONTENT_LOCATION = "Content-Location";

	private static final String CONTENT_MD5 = "Content-MD5";

	private static final String CONTENT_RANGE = "Content-Range";

	private static final String CONTENT_TYPE = "Content-Type";

	public  static final String COOKIE = "Cookie";

	private static final String DATE = "Date";

	private static final String ETAG = "ETag";

	private static final String EXPECT = "Expect";

	private static final String EXPIRES = "Expires";

	private static final String FROM = "From";

	private static final String HOST = "Host";

	private static final String IF_MATCH = "If-Match";

	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";

	private static final String IF_NONE_MATCH = "If-None-Match";

	private static final String IF_RANGE = "If-Range";

	private static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

	private static final String LAST_MODIFIED = "Last-Modified";

	private static final String LOCATION = "Location";

	private static final String MAX_FORWARDS = "Max-Forwards";

	private static final String PRAGMA = "Pragma";

	private static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

	private static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

	private static final String RANGE = "Range";

	private static final String REFERER = "Referer";

	private static final String REFRESH = "Refresh";

	private static final String RETRY_AFTER = "Retry-After";

	private static final String SERVER = "Server";

	public  static final String SET_COOKIE = "Set-Cookie";

	private static final String TE = "TE";

	private static final String TRAILER = "Trailer";

	private static final String UPGRADE = "Upgrade";

	private static final String USER_AGENT = "User-Agent";

	private static final String VARY = "Vary";

	private static final String VIA = "Via";

	private static final String WARNING = "Warning";

	private static final String WWW_AUTHENTICATE = "WWW-Authenticate";

	private static final String[] HTTP_REQUEST_HEADER_NAMES = new String[] {
			ACCEPT,
			ACCEPT_CHARSET,
			ACCEPT_ENCODING,
			ACCEPT_LANGUAGE,
			ACCEPT_RANGES,
			AUTHORIZATION,
			CACHE_CONTROL,
			CONNECTION,
			CONTENT_LENGTH,
			CONTENT_TYPE,
			COOKIE,
			DATE,
			EXPECT,
			FROM,
			HOST,
			IF_MATCH,
			IF_MODIFIED_SINCE,
			IF_NONE_MATCH,
			IF_RANGE,
			IF_UNMODIFIED_SINCE,
			MAX_FORWARDS,
			PRAGMA,
			PROXY_AUTHORIZATION,
			RANGE,
			REFERER,
			TE,
			UPGRADE,
			USER_AGENT,
			VIA,
			WARNING
	};

	private static String[] HTTP_RESPONSE_HEADER_NAMES = new String[] {
			ACCEPT_RANGES,
			AGE,
			ALLOW,
			CACHE_CONTROL,
			CONNECTION,
			CONTENT_ENCODING,
			CONTENT_LANGUAGE,
			CONTENT_LENGTH,
			CONTENT_LOCATION,
			CONTENT_MD5,
			CONTENT_RANGE,
			CONTENT_TYPE,
			DATE,
			ETAG,
			EXPIRES,
			LAST_MODIFIED,
			LOCATION,
			PRAGMA,
			PROXY_AUTHENTICATE,
			REFRESH,
			RETRY_AFTER,
			SERVER,
			SET_COOKIE,
			TRAILER,
			VARY,
			VIA,
			WARNING,
			WWW_AUTHENTICATE
	};

	private static String[] HTTP_REQUEST_HEADER_NAMES_OUTBOUND_EXCLUSIONS = new String[0];

	private static String[] HTTP_RESPONSE_HEADER_NAMES_INBOUND_EXCLUSIONS = new String[] {
		CONTENT_LENGTH
	};

	public static final String HTTP_REQUEST_HEADER_NAME_PATTERN = "HTTP_REQUEST_HEADERS";

	public static final String HTTP_RESPONSE_HEADER_NAME_PATTERN = "HTTP_RESPONSE_HEADERS";

	// Copy of 'org.springframework.http.HttpHeaders#DATE_FORMATS'
	private static final String[] DATE_FORMATS = new String[] {
			"EEE, dd MMM yyyy HH:mm:ss zzz",
			"EEE, dd-MMM-yy HH:mm:ss zzz",
			"EEE MMM dd HH:mm:ss yyyy"
	};

	private static TimeZone GMT = TimeZone.getTimeZone("GMT");

	private volatile String[] outboundHeaderNames = new String[0];

	private volatile String[] inboundHeaderNames = new String[0];

	private volatile String[] excludedOutboundStandardRequestHeaderNames = new String[0];

	private volatile String[] excludedInboundStandardResponseHeaderNames = new String[0];

	private volatile String userDefinedHeaderPrefix = "X-";

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Provide the header names that should be mapped to an HTTP request (for outbound adapters)
	 * or HTTP response (for inbound adapters) from a Spring Integration Message's headers.
	 * The values can also contain simple wildcard patterns (e.g. "foo*" or "*foo") to be matched.
	 * <p>
	 * Any non-standard headers will be prefixed with the value specified by
	 * {@link DefaultHttpHeaderMapper#setUserDefinedHeaderPrefix(String)}. The default is 'X-'.
	 *
	 * @param outboundHeaderNames The outbound header names.
	 */
	public void setOutboundHeaderNames(String[] outboundHeaderNames) {
		this.outboundHeaderNames = (outboundHeaderNames != null) ? outboundHeaderNames : new String[0];
	}

	/**
	 * Provide the header names that should be mapped from an HTTP request (for inbound adapters)
	 * or HTTP response (for outbound adapters) to a Spring Integration Message's headers.
	 * The values can also contain simple wildcard patterns (e.g. "foo*" or "*foo") to be matched.
	 * <p>
	 * This will match the header name directly or, for non-standard HTTP headers, it will match
	 * the header name prefixed with the value specified by
	 * {@link DefaultHttpHeaderMapper#setUserDefinedHeaderPrefix(String)}. The default is 'X-'.
	 *
	 * @param inboundHeaderNames The inbound header names.
	 */
	public void setInboundHeaderNames(String[] inboundHeaderNames) {
		this.inboundHeaderNames = (inboundHeaderNames != null) ? inboundHeaderNames : new String[0];
	}

	/**
	 * Provide header names from the list of standard headers that should be suppressed when
	 * mapping outbound endpoint request headers.
	 * @param excludedOutboundStandardRequestHeaderNames the excludedStandardRequestHeaderNames to set
	 */
	public void setExcludedOutboundStandardRequestHeaderNames(String[] excludedOutboundStandardRequestHeaderNames) {
		Assert.notNull(excludedOutboundStandardRequestHeaderNames, "'excludedOutboundStandardRequestHeaderNames' must not be null");
		this.excludedOutboundStandardRequestHeaderNames = excludedOutboundStandardRequestHeaderNames;
	}

	/**
	 * Provide header names from the list of standard headers that should be suppressed when
	 * mapping inbound endopoint response headers.
	 * @param excludedInboundStandardResponseHeaderNames the excludedStandardResponseHeaderNames to set
	 */
	public void setExcludedInboundStandardResponseHeaderNames(String[] excludedInboundStandardResponseHeaderNames) {
		Assert.notNull(excludedInboundStandardResponseHeaderNames, "'excludedInboundStandardResponseHeaderNames' must not be null");
		this.excludedInboundStandardResponseHeaderNames = excludedInboundStandardResponseHeaderNames;
	}

	/**
	 * Sets the prefix to use with user-defined (non-standard) headers. Default is 'X-'.
	 *
	 * @param userDefinedHeaderPrefix The user defined header prefix.
	 */
	public void setUserDefinedHeaderPrefix(String userDefinedHeaderPrefix) {
		this.userDefinedHeaderPrefix = (userDefinedHeaderPrefix != null) ? userDefinedHeaderPrefix : "";
	}

	/**
	 * Map from the integration MessageHeaders to an HttpHeaders instance.
	 * Depending on which type of adapter is using this mapper, the HttpHeaders might be
	 * for an HTTP request (outbound adapter) or for an HTTP response (inbound adapter).
	 */
	@Override
	public void fromHeaders(MessageHeaders headers, HttpHeaders target) {
		if (logger.isDebugEnabled()){
			logger.debug(MessageFormat.format("outboundHeaderNames={0}", CollectionUtils.arrayToList(outboundHeaderNames)));
		}
		Set<String> headerNames = headers.keySet();
		for (String name : headerNames) {
			if (this.shouldMapOutboundHeader(name)) {
				Object value = headers.get(name);
				if (value != null) {
					if (!this.containsElementIgnoreCase(HTTP_REQUEST_HEADER_NAMES, name) &&
						!this.containsElementIgnoreCase(HTTP_RESPONSE_HEADER_NAMES, name)) {
						// prefix the user-defined header names if not already prefixed

						name = StringUtils.startsWithIgnoreCase(name, this.userDefinedHeaderPrefix) ? name :
							this.userDefinedHeaderPrefix + name;
					}
					if (logger.isDebugEnabled()) {
						logger.debug(MessageFormat.format("setting headerName=[{0}], value={1}", name, value));
					}
					this.setHttpHeader(target, name, value);
				}
			}
		}
	}

	/**
	 * Map from an HttpHeaders instance to integration MessageHeaders.
	 * Depending on which type of adapter is using this mapper, the HttpHeaders might be
	 * from an HTTP request (inbound adapter) or from an HTTP response (outbound adapter).
	 */
	@Override
	public Map<String, Object> toHeaders(HttpHeaders source) {
		if (logger.isDebugEnabled()) {
			logger.debug(MessageFormat.format("inboundHeaderNames={0}", CollectionUtils.arrayToList(inboundHeaderNames)));
		}
		Map<String, Object> target = new HashMap<String, Object>();
		Set<String> headerNames = source.keySet();
		for (String name : headerNames) {
			if (this.shouldMapInboundHeader(name)) {
				if (!ObjectUtils.containsElement(HTTP_REQUEST_HEADER_NAMES, name) && !ObjectUtils.containsElement(HTTP_RESPONSE_HEADER_NAMES, name)) {
					String prefixedName = StringUtils.startsWithIgnoreCase(name, this.userDefinedHeaderPrefix) ? name :
						this.userDefinedHeaderPrefix + name;
					Object value = source.containsKey(prefixedName) ? this.getHttpHeader(source, prefixedName) : this.getHttpHeader(source, name);
					if (value != null) {
						if (logger.isDebugEnabled()) {
							logger.debug(MessageFormat.format("setting headerName=[{0}], value={1}", name, value));
						}
						this.setMessageHeader(target, name, value);
					}
				}
				else {
					Object value = this.getHttpHeader(source, name);
					if (value != null) {
						if (logger.isDebugEnabled()) {
							logger.debug(MessageFormat.format("setting headerName=[{0}], value={1}", name, value));
						}
						this.setMessageHeader(target, name, value);
					}
				}
			}
		}
		return target;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.beanFactory != null){
			this.conversionService = IntegrationUtils.getConversionService(this.beanFactory);
		}
	}

	private boolean containsElementIgnoreCase(String[] headerNames, String name){
		for (String headerName : headerNames) {
			if (headerName.equalsIgnoreCase(name)){
				return true;
			}
		}
		return false;
	}

	private boolean shouldMapOutboundHeader(String headerName) {
		if (this.outboundHeaderNames == HTTP_RESPONSE_HEADER_NAMES) { // a default inbound mapper
			/*
			 * When using the default response header name list, suppress the
			 * mapping of exclusions for specific headers.
			 */
			if (this.containsElementIgnoreCase(this.excludedInboundStandardResponseHeaderNames, headerName)) {
				if (logger.isDebugEnabled()) {
					logger.debug(MessageFormat.format("headerName=[{0}] WILL NOT be mapped", headerName));
				}
				return false;
			}
		}
		else if (this.outboundHeaderNames == HTTP_REQUEST_HEADER_NAMES) { // a default outbound mapper
			/*
			 * When using the default request header name list, suppress the
			 * mapping of exclusions for specific headers.
			 */
			if (this.containsElementIgnoreCase(this.excludedOutboundStandardRequestHeaderNames, headerName)) {
				if (logger.isDebugEnabled()) {
					logger.debug(MessageFormat.format("headerName=[{0}] WILL NOT be mapped", headerName));
				}
				return false;
			}
		}
		return this.shouldMapHeader(headerName, this.outboundHeaderNames);
	}

	private boolean shouldMapInboundHeader(String headerName) {
		return this.shouldMapHeader(headerName, this.inboundHeaderNames);
	}

	private boolean shouldMapHeader(String headerName, String[] patterns) {
		if (patterns != null && patterns.length > 0) {
			for (String pattern : patterns) {
				if (PatternMatchUtils.simpleMatch(pattern.toLowerCase(), headerName.toLowerCase())) {
					if (logger.isDebugEnabled()) {
						logger.debug(MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}", headerName, pattern));
					}
					return true;
				}
				else if (HTTP_REQUEST_HEADER_NAME_PATTERN.equals(pattern)
						&& this.containsElementIgnoreCase(HTTP_REQUEST_HEADER_NAMES, headerName)) {
					if (logger.isDebugEnabled()) {
						logger.debug(MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}", headerName, pattern));
					}
					return true;
				}
				else if (HTTP_RESPONSE_HEADER_NAME_PATTERN.equals(pattern)
						&& this.containsElementIgnoreCase(HTTP_RESPONSE_HEADER_NAMES, headerName)) {
					if (logger.isDebugEnabled()) {
						logger.debug(MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}", headerName, pattern));
					}
					return true;
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug(MessageFormat.format("headerName=[{0}] WILL NOT be mapped", headerName));
		}
		return false;
	}

	private void setHttpHeader(HttpHeaders target, String name, Object value) {
		if (ACCEPT.equalsIgnoreCase(name)) {
			if (value instanceof Collection<?>) {
				Collection<?> values = (Collection<?>) value;
				if (!CollectionUtils.isEmpty(values)) {
					List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
					for (Object type : values) {
						if (type instanceof MediaType) {
							acceptableMediaTypes.add((MediaType) type);
						}
						else if (type instanceof String) {
							acceptableMediaTypes.addAll(MediaType.parseMediaTypes((String) type));
						}
						else {
							Class<?> clazz = (type != null) ? type.getClass() : null;
							throw new IllegalArgumentException(
									"Expected MediaType or String value for 'Accept' header value, but received: " + clazz);
						}
					}
					target.setAccept(acceptableMediaTypes);
				}
			}
			else if (value instanceof MediaType) {
				target.setAccept(Collections.singletonList((MediaType) value));
			}
			else if (value instanceof String[]) {
				List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
				for (String next : (String[]) value) {
					acceptableMediaTypes.add(MediaType.parseMediaType(next));
				}
				target.setAccept(acceptableMediaTypes);
			}
			else if (value instanceof String) {
				target.setAccept(MediaType.parseMediaTypes((String) value));
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected MediaType or String value for 'Accept' header value, but received: " + clazz);
			}
		}
		else if (ACCEPT_CHARSET.equalsIgnoreCase(name)) {
			if (value instanceof Collection<?>) {
				Collection<?> values = (Collection<?>) value;
				if (!CollectionUtils.isEmpty(values)) {
					List<Charset> acceptableCharsets = new ArrayList<Charset>();
					for (Object charset : values) {
						if (charset instanceof Charset) {
							acceptableCharsets.add((Charset) charset);
						}
						else if (charset instanceof String) {
							acceptableCharsets.add(Charset.forName((String) charset));
						}
						else {
							Class<?> clazz = (charset != null) ? charset.getClass() : null;
							throw new IllegalArgumentException(
									"Expected Charset or String value for 'Accept-Charset' header value, but received: " + clazz);
						}
					}
					target.setAcceptCharset(acceptableCharsets);
				}
			}
			else if (value instanceof Charset[] || value instanceof String[]) {
				List<Charset> acceptableCharsets = new ArrayList<Charset>();
				Object[] values = ObjectUtils.toObjectArray(value);
				for (Object charset : values) {
					if (charset instanceof Charset) {
						acceptableCharsets.add((Charset) charset);
					}
					else if (charset instanceof String) {
						acceptableCharsets.add(Charset.forName((String) charset));
					}
				}
				target.setAcceptCharset(acceptableCharsets);
			}
			else if (value instanceof Charset) {
				target.setAcceptCharset(Collections.singletonList((Charset) value));
			}
			else if (value instanceof String) {
				String[] charsets = StringUtils.commaDelimitedListToStringArray((String) value);
				List<Charset> acceptableCharsets = new ArrayList<Charset>();
				for (String charset : charsets) {
					acceptableCharsets.add(Charset.forName(charset.trim()));
				}
				target.setAcceptCharset(acceptableCharsets);
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected Charset or String value for 'Accept-Charset' header value, but received: " + clazz);
			}
		}
		else if (ALLOW.equalsIgnoreCase(name)) {
			if (value instanceof Collection<?>) {
				Collection<?> values = (Collection<?>) value;
				if (!CollectionUtils.isEmpty(values)) {
					Set<HttpMethod> allowedMethods = new HashSet<HttpMethod>();
					for (Object method : values) {
						if (method instanceof HttpMethod) {
							allowedMethods.add((HttpMethod) method);
						}
						else if (method instanceof String) {
							allowedMethods.add(HttpMethod.valueOf((String) method));
						}
						else {
							Class<?> clazz = (method != null) ? method.getClass() : null;
							throw new IllegalArgumentException(
									"Expected HttpMethod or String value for 'Allow' header value, but received: " + clazz);
						}
					}
					target.setAllow(allowedMethods);
				}
			}
			else {
				if (value instanceof HttpMethod) {
					target.setAllow(Collections.singleton((HttpMethod) value));
				}
				else if (value instanceof HttpMethod[]) {
					Set<HttpMethod> allowedMethods = new HashSet<HttpMethod>();
					for (HttpMethod next : (HttpMethod[]) value) {
						allowedMethods.add(next);
					}
					target.setAllow(allowedMethods);
				}
				else if (value instanceof String || value instanceof String[]) {
					String[] values = (value instanceof String[]) ? (String[]) value
							: StringUtils.commaDelimitedListToStringArray((String) value);
					Set<HttpMethod> allowedMethods = new HashSet<HttpMethod>();
					for (String next : values) {
						allowedMethods.add(HttpMethod.valueOf(next.trim()));
					}
					target.setAllow(allowedMethods);
				}
				else {
					Class<?> clazz = (value != null) ? value.getClass() : null;
					throw new IllegalArgumentException(
							"Expected HttpMethod or String value for 'Allow' header value, but received: " + clazz);
				}
			}
		}
		else if (CACHE_CONTROL.equalsIgnoreCase(name)) {
			if (value instanceof String) {
				target.setCacheControl((String) value);
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected String value for 'Cache-Control' header value, but received: " + clazz);
			}
		}
		else if (CONTENT_LENGTH.equalsIgnoreCase(name)) {
			if (value instanceof Number) {
				target.setContentLength(((Number) value).longValue());
			}
			else if (value instanceof String) {
				target.setContentLength(Long.parseLong((String) value));
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected Number or String value for 'Content-Length' header value, but received: " + clazz);
			}
		}
		else if (CONTENT_TYPE.equalsIgnoreCase(name)) {
			if (value instanceof MediaType) {
				target.setContentType((MediaType) value);
			}
			else if (value instanceof String) {
				target.setContentType(MediaType.parseMediaType((String) value));
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected MediaType or String value for 'Content-Type' header value, but received: " + clazz);
			}
		}
		else if (DATE.equalsIgnoreCase(name)) {
			if (value instanceof Date) {
				target.setDate(((Date) value).getTime());
			}
			else if (value instanceof Number) {
				target.setDate(((Number) value).longValue());
			}
			else if (value instanceof String) {
				try {
					target.setDate(Long.parseLong((String) value));
				}
				catch (NumberFormatException e) {
					target.setDate(this.getFirstDate((String) value, DATE));
				}
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected Date, Number, or String value for 'Date' header value, but received: " + clazz);
			}
		}
		else if (ETAG.equalsIgnoreCase(name)) {
			if (value instanceof String) {
				target.setETag((String) value);
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected String value for 'ETag' header value, but received: " + clazz);
			}
		}
		else if (EXPIRES.equalsIgnoreCase(name)) {
			if (value instanceof Date) {
				target.setExpires(((Date) value).getTime());
			}
			else if (value instanceof Number) {
				target.setExpires(((Number) value).longValue());
			}
			else if (value instanceof String) {
				try {
					target.setExpires(Long.parseLong((String) value));
				}
				catch (NumberFormatException e) {
					target.setExpires(this.getFirstDate((String) value, EXPIRES));
				}
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected Date, Number, or String value for 'Expires' header value, but received: " + clazz);
			}
		}
		else if (IF_MODIFIED_SINCE.equalsIgnoreCase(name)) {
			if (value instanceof Date) {
				target.setIfModifiedSince(((Date) value).getTime());
			}
			else if (value instanceof Number) {
				target.setIfModifiedSince(((Number) value).longValue());
			}
			else if (value instanceof String) {
				try {
					target.setIfModifiedSince(Long.parseLong((String) value));
				}
				catch (NumberFormatException e) {
					target.setIfModifiedSince(this.getFirstDate((String) value, IF_MODIFIED_SINCE));
				}
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected Date, Number, or String value for 'If-Modified-Since' header value, but received: " + clazz);
			}
		}
		else if (IF_UNMODIFIED_SINCE.equalsIgnoreCase(name)) {
			String ifUnmodifiedSinceValue = null;
			if (value instanceof Date) {
				ifUnmodifiedSinceValue = this.formatDate(((Date) value).getTime());
			}
			else if (value instanceof Number) {
				ifUnmodifiedSinceValue = this.formatDate(((Number) value).longValue());
			}
			else if (value instanceof String) {
				try {
					ifUnmodifiedSinceValue = this.formatDate(Long.parseLong((String) value));
				}
				catch (NumberFormatException e) {
					long longValue = this.getFirstDate((String) value, IF_UNMODIFIED_SINCE);
					ifUnmodifiedSinceValue = this.formatDate(longValue);
				}
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected Date, Number, or String value for 'If-Unmodified-Since' header value, but received: " + clazz);
			}
			target.set(IF_UNMODIFIED_SINCE, ifUnmodifiedSinceValue);
		}
		else if (IF_NONE_MATCH.equalsIgnoreCase(name)) {
			if (value instanceof String) {
				target.setIfNoneMatch((String) value);
			}
			else if (value instanceof String[]) {
				String delmitedString = StringUtils.arrayToCommaDelimitedString((String[]) value);
				target.setIfNoneMatch(delmitedString);
			}
			else if (value instanceof Collection) {
				Collection<?> values = (Collection<?>) value;
				if (!CollectionUtils.isEmpty(values)) {
					List<String> ifNoneMatchList = new ArrayList<String>();
					for (Object next : values) {
						if (next instanceof String) {
							ifNoneMatchList.add((String) next);
						}
						else {
							Class<?> clazz = (next != null) ? next.getClass() : null;
							throw new IllegalArgumentException(
									"Expected String value for 'If-None-Match' header value, but received: " + clazz);
						}
					}
					target.setIfNoneMatch(ifNoneMatchList);
				}
			}
		}
		else if (LAST_MODIFIED.equalsIgnoreCase(name)) {
			if (value instanceof Date) {
				target.setLastModified(((Date) value).getTime());
			}
			else if (value instanceof Number) {
				target.setLastModified(((Number) value).longValue());
			}
			else if (value instanceof String) {
				try {
					target.setLastModified(Long.parseLong((String) value));
				}
				catch (NumberFormatException e) {
					target.setLastModified(this.getFirstDate((String) value, LAST_MODIFIED));
				}
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected Date, Number, or String value for 'Last-Modified' header value, but received: " + clazz);
			}
		}
		else if (LOCATION.equalsIgnoreCase(name)) {
			if (value instanceof URI) {
				target.setLocation((URI) value);
			}
			else if (value instanceof String) {
				try {
					target.setLocation(new URI((String) value));
				}
				catch (URISyntaxException e) {
					throw new IllegalArgumentException(e);
				}
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected URI or String value for 'Location' header value, but received: " + clazz);
			}
		}
		else if (PRAGMA.equalsIgnoreCase(name)) {
			if (value instanceof String) {
				target.setPragma((String) value);
			}
			else {
				Class<?> clazz = (value != null) ? value.getClass() : null;
				throw new IllegalArgumentException(
						"Expected String value for 'Pragma' header value, but received: " + clazz);
			}
		}
		else if (value instanceof String) {
			target.set(name, (String) value);
		}
		else if (value instanceof String[]) {
			for (String next : (String[]) value) {
				target.add(name, next);
			}
		}
		else if (value instanceof Iterable<?>) {
			for (Object next : (Iterable<?>) value) {
				String convertedValue = null;
				if (next instanceof String) {
					convertedValue = (String) next;
				}
				else {
					convertedValue = this.convertToString(value);
				}
				if (StringUtils.hasText(convertedValue)){
					target.add(name, (String) next);
				}
				else {
					logger.warn("Element of the header '" + name + "' with value '" + value +
							"' will not be set since it is not a String and no Converter " +
							"is available. Consider registering a Converter with ConversionService (e.g., <int:converter>)");
				}
			}
		}
		else {
			String convertedValue = this.convertToString(value);
			if (StringUtils.hasText(convertedValue)) {
				target.set(name, convertedValue);
			}
			else {
				logger.warn("Header '" + name + "' with value '" + value +
						"' will not be set since it is not a String and no Converter " +
						"is available. Consider registering a Converter with ConversionService (e.g., <int:converter>)");
			}
		}
	}

	private Object getHttpHeader(HttpHeaders source, String name) {
		if (ACCEPT.equalsIgnoreCase(name)) {
			return source.getAccept();
		}
		else if (ACCEPT_CHARSET.equalsIgnoreCase(name)) {
			return source.getAcceptCharset();
		}
		else if (ALLOW.equalsIgnoreCase(name)) {
			return source.getAllow();
		}
		else if (CACHE_CONTROL.equalsIgnoreCase(name)) {
			String cacheControl = source.getCacheControl();
			return (StringUtils.hasText(cacheControl)) ? cacheControl : null;
		}
		else if (CONTENT_LENGTH.equalsIgnoreCase(name)) {
			long contentLength = source.getContentLength();
			return (contentLength > -1) ? contentLength : null;
		}
		else if (CONTENT_TYPE.equalsIgnoreCase(name)) {
			return source.getContentType();
		}
		else if (DATE.equalsIgnoreCase(name)) {
			long date = source.getDate();
			return (date > -1) ? date : null;
		}
		else if (ETAG.equalsIgnoreCase(name)) {
			String eTag = source.getETag();
			return (StringUtils.hasText(eTag)) ? eTag : null;
		}
		else if (EXPIRES.equalsIgnoreCase(name)) {
			try {
				long expires = source.getExpires();
				return (expires > -1) ? expires : null;
			}
			catch (Exception e) {
				if(logger.isDebugEnabled()) {
					logger.debug(e.getMessage());
				}
				// According to RFC 2616
				return null;
			}
		}
		else if (IF_NONE_MATCH.equalsIgnoreCase(name)) {
			return source.getIfNoneMatch();
		}
		else if (IF_MODIFIED_SINCE.equalsIgnoreCase(name)) {
			long modifiedSince = source.getIfModifiedSince();
			return (modifiedSince > -1) ? modifiedSince : null;
		}
		else if (IF_UNMODIFIED_SINCE.equalsIgnoreCase(name)) {
			String unmodifiedSince = source.getFirst(IF_UNMODIFIED_SINCE);
			return unmodifiedSince != null ? this.getFirstDate(unmodifiedSince, IF_UNMODIFIED_SINCE) : null;
		}
		else if (LAST_MODIFIED.equalsIgnoreCase(name)) {
			long lastModified = source.getLastModified();
			return (lastModified > -1) ? lastModified : null;
		}
		else if (LOCATION.equalsIgnoreCase(name)) {
			return source.getLocation();
		}
		else if (PRAGMA.equalsIgnoreCase(name)) {
			String pragma = source.getPragma();
			return (StringUtils.hasText(pragma)) ? pragma : null;
		}
		return source.get(name);
	}

	private void setMessageHeader(Map<String, Object> target, String name, Object value) {
		if (ObjectUtils.isArray(value)) {
			Object[] values = ObjectUtils.toObjectArray(value);
			if (!ObjectUtils.isEmpty(values)) {
				if (values.length == 1) {
					target.put(name, values);
				}
				else {
					target.put(name, values[0]);
				}
			}
		}
		else if (value instanceof Collection<?>) {
			Collection<?> values = (Collection<?>) value;
			if (!CollectionUtils.isEmpty(values)) {
				if (values.size() == 1) {
					target.put(name, values.iterator().next());
				}
				else {
					target.put(name, values);
				}
			}
		}
		else if (value != null) {
			target.put(name, value);
		}
	}

	private String convertToString(Object value){
		if (this.conversionService != null &&
				this.conversionService.canConvert(TypeDescriptor.forObject(value), TypeDescriptor.valueOf(String.class))){
			return this.conversionService.convert(value, String.class);
		}
		return null;
	}

	// Utility methods

	private long getFirstDate(String headerValue, String headerName) {
		for (String dateFormat : DATE_FORMATS) {
			DateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
			simpleDateFormat.setTimeZone(GMT);
			try {
				return simpleDateFormat.parse(headerValue).getTime();
			}
			catch (ParseException e) {
				// ignore
			}
		}
		throw new IllegalArgumentException("Cannot parse date value '" + headerValue +"' for '" + headerName + "' header");
	}

	private String formatDate(long date) {
		DateFormat dateFormat = new SimpleDateFormat(DATE_FORMATS[0], Locale.US);
		dateFormat.setTimeZone(GMT);
		return dateFormat.format(new Date(date));
	}

	/**
	 * Factory method for creating a basic outbound mapper instance.
	 * This will map all standard HTTP request headers when sending an HTTP request,
	 * and it will map all standard HTTP response headers when receiving an HTTP response.
	 *
	 * @return The default outbound mapper.
	 */
	public static DefaultHttpHeaderMapper outboundMapper() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames(HTTP_REQUEST_HEADER_NAMES);
		mapper.setInboundHeaderNames(HTTP_RESPONSE_HEADER_NAMES);
		mapper.setExcludedOutboundStandardRequestHeaderNames(HTTP_REQUEST_HEADER_NAMES_OUTBOUND_EXCLUSIONS);
		return mapper;
	}

	/**
	 * Factory method for creating a basic inbound mapper instance.
	 * This will map all standard HTTP request headers when receiving an HTTP request,
	 * and it will map all standard HTTP response headers when sending an HTTP response.
	 *
	 * @return The default inbound mapper.
	 */
	public static DefaultHttpHeaderMapper inboundMapper() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames(HTTP_REQUEST_HEADER_NAMES);
		mapper.setOutboundHeaderNames(HTTP_RESPONSE_HEADER_NAMES);
		mapper.setExcludedInboundStandardResponseHeaderNames(HTTP_RESPONSE_HEADER_NAMES_INBOUND_EXCLUSIONS);
		return mapper;
	}

}
