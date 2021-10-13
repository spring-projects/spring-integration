/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
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
 *
 * @since 2.0
 */
public class DefaultHttpHeaderMapper implements HeaderMapper<HttpHeaders>, BeanFactoryAware, InitializingBean {

	private static final String UNUSED = "unused";

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR - final

	public static final String CONTENT_MD5 = "Content-MD5";

	public static final String REFRESH = "Refresh";

	private static final String ACCEPT_LOWER = "accept";

	private static final String ACCEPT_CHARSET_LOWER = "accept-charset";

	private static final String ALLOW_LOWER = "allow";

	private static final String CACHE_CONTROL_LOWER = "cache-control";

	private static final String CONTENT_LENGTH_LOWER = "content-length";

	private static final String CONTENT_TYPE_LOWER = "content-type";

	private static final String DATE_LOWER = "date";

	private static final String ETAG_LOWER = "etag";

	private static final String EXPIRES_LOWER = "expires";

	private static final String IF_MODIFIED_SINCE_LOWER = "if-modified-since";

	private static final String IF_NONE_MATCH_LOWER = "if-none-match";

	private static final String IF_UNMODIFIED_SINCE_LOWER = "if-unmodified-since";

	private static final String LAST_MODIFIED_LOWER = "last-modified";

	private static final String LOCATION_LOWER = "location";

	private static final String PRAGMA_LOWER = "pragma";

	private static final String[] HTTP_REQUEST_HEADER_NAMES =
			{
					HttpHeaders.ACCEPT,
					HttpHeaders.ACCEPT_CHARSET,
					HttpHeaders.ACCEPT_ENCODING,
					HttpHeaders.ACCEPT_LANGUAGE,
					HttpHeaders.ACCEPT_RANGES,
					HttpHeaders.AUTHORIZATION,
					HttpHeaders.CACHE_CONTROL,
					HttpHeaders.CONNECTION,
					HttpHeaders.CONTENT_LENGTH,
					HttpHeaders.CONTENT_TYPE,
					HttpHeaders.COOKIE,
					HttpHeaders.DATE,
					HttpHeaders.EXPECT,
					HttpHeaders.FROM,
					HttpHeaders.HOST,
					HttpHeaders.IF_MATCH,
					HttpHeaders.IF_MODIFIED_SINCE,
					HttpHeaders.IF_NONE_MATCH,
					HttpHeaders.IF_RANGE,
					HttpHeaders.IF_UNMODIFIED_SINCE,
					HttpHeaders.MAX_FORWARDS,
					HttpHeaders.PRAGMA,
					HttpHeaders.PROXY_AUTHORIZATION,
					HttpHeaders.RANGE,
					HttpHeaders.REFERER,
					HttpHeaders.TE,
					HttpHeaders.UPGRADE,
					HttpHeaders.USER_AGENT,
					HttpHeaders.VIA,
					HttpHeaders.WARNING
			};

	private static final Set<String> HTTP_REQUEST_HEADER_NAMES_LOWER = new HashSet<>();

	private static final String[] HTTP_RESPONSE_HEADER_NAMES =
			{
					HttpHeaders.ACCEPT_RANGES,
					HttpHeaders.AGE,
					HttpHeaders.ALLOW,
					HttpHeaders.CACHE_CONTROL,
					HttpHeaders.CONNECTION,
					HttpHeaders.CONTENT_ENCODING,
					HttpHeaders.CONTENT_LANGUAGE,
					HttpHeaders.CONTENT_LENGTH,
					HttpHeaders.CONTENT_LOCATION,
					CONTENT_MD5,
					HttpHeaders.CONTENT_RANGE,
					HttpHeaders.CONTENT_TYPE,
					HttpHeaders.CONTENT_DISPOSITION,
					HttpHeaders.TRANSFER_ENCODING,
					HttpHeaders.DATE,
					HttpHeaders.ETAG,
					HttpHeaders.EXPIRES,
					HttpHeaders.LAST_MODIFIED,
					HttpHeaders.LOCATION,
					HttpHeaders.PRAGMA,
					HttpHeaders.PROXY_AUTHENTICATE,
					REFRESH,
					HttpHeaders.RETRY_AFTER,
					HttpHeaders.SERVER,
					HttpHeaders.SET_COOKIE,
					HttpHeaders.TRAILER,
					HttpHeaders.VARY,
					HttpHeaders.VIA,
					HttpHeaders.WARNING,
					HttpHeaders.WWW_AUTHENTICATE
			};

	private static final Set<String> HTTP_RESPONSE_HEADER_NAMES_LOWER = new HashSet<>();

	private static final String[] HTTP_REQUEST_HEADER_NAMES_OUTBOUND_EXCLUSIONS = { };

	private static final String[] HTTP_RESPONSE_HEADER_NAMES_INBOUND_EXCLUSIONS =
			{ HttpHeaders.CONTENT_LENGTH, HttpHeaders.TRANSFER_ENCODING };

	public static final String HTTP_REQUEST_HEADER_NAME_PATTERN = "HTTP_REQUEST_HEADERS";

	public static final String HTTP_RESPONSE_HEADER_NAME_PATTERN = "HTTP_RESPONSE_HEADERS";

	// Copy of 'org.springframework.http.HttpHeaders#DATE_FORMATS'
	protected static final DateTimeFormatter[] DATE_FORMATS = {
			DateTimeFormatter.RFC_1123_DATE_TIME,
			DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zz", Locale.US),
			DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", Locale.US).withZone(ZoneId.of("GMT")) // NOSONAR
	};

	static {
		for (String header : HTTP_REQUEST_HEADER_NAMES) {
			HTTP_REQUEST_HEADER_NAMES_LOWER.add(header.toLowerCase());
		}
		for (String header : HTTP_RESPONSE_HEADER_NAMES) {
			HTTP_RESPONSE_HEADER_NAMES_LOWER.add(header.toLowerCase());
		}
	}

	private volatile String[] outboundHeaderNames = {};

	private volatile String[] outboundHeaderNamesLowerWithContentType = {};

	private volatile String[] inboundHeaderNames = {};

	private volatile String[] inboundHeaderNamesLower = {};

	private volatile String[] excludedOutboundStandardRequestHeaderNames = {};

	private volatile String[] excludedInboundStandardResponseHeaderNames = {};

	private volatile String userDefinedHeaderPrefix = "";

	private volatile boolean isDefaultOutboundMapper;

	private volatile boolean isDefaultInboundMapper;

	private volatile ConversionService conversionService;

	private volatile BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Provide the header names that should be mapped to an HTTP request (for outbound adapters)
	 * or HTTP response (for inbound adapters) from a Spring Integration Message's headers.
	 * The values can also contain simple wildcard patterns (e.g. "foo*" or "*foo") to be matched.
	 * <p> Any non-standard headers will be prefixed with the value specified by
	 * {@link DefaultHttpHeaderMapper#setUserDefinedHeaderPrefix(String)}. The default is 'X-'.
	 * @param outboundHeaderNames The outbound header names.
	 */
	public void setOutboundHeaderNames(String... outboundHeaderNames) {
		if (Arrays.equals(HTTP_REQUEST_HEADER_NAMES, outboundHeaderNames)) {
			this.isDefaultOutboundMapper = true;
		}
		else if (Arrays.equals(HTTP_RESPONSE_HEADER_NAMES, outboundHeaderNames)) {
			this.isDefaultInboundMapper = true;
		}
		this.outboundHeaderNames =
				outboundHeaderNames != null
						? Arrays.copyOf(outboundHeaderNames, outboundHeaderNames.length)
						: new String[0];
		String[] outboundHeaderNamesLower = new String[this.outboundHeaderNames.length];
		for (int i = 0; i < this.outboundHeaderNames.length; i++) {
			if (HTTP_REQUEST_HEADER_NAME_PATTERN.equals(this.outboundHeaderNames[i])
					|| HTTP_RESPONSE_HEADER_NAME_PATTERN.equals(this.outboundHeaderNames[i])) {
				outboundHeaderNamesLower[i] = this.outboundHeaderNames[i];
			}
			else {
				outboundHeaderNamesLower[i] = this.outboundHeaderNames[i].toLowerCase();
			}
		}
		this.outboundHeaderNamesLowerWithContentType =
				Arrays.copyOf(outboundHeaderNamesLower, this.outboundHeaderNames.length + 1);
		this.outboundHeaderNamesLowerWithContentType[this.outboundHeaderNamesLowerWithContentType.length - 1]
				= MessageHeaders.CONTENT_TYPE.toLowerCase();
	}

	/**
	 * Provide the header names that should be mapped from an HTTP request (for inbound
	 * adapters) or HTTP response (for outbound adapters) to a Spring Integration
	 * Message's headers. The values can also contain simple wildcard patterns (e.g.
	 * "foo*" or "*foo") to be matched.
	 * <p>This will match the header name directly or, for non-standard HTTP headers, it
	 * will match the header name prefixed with the value specified by
	 * {@link DefaultHttpHeaderMapper#setUserDefinedHeaderPrefix(String)}. The default for
	 * that is an empty String.
	 * @param inboundHeaderNamesArg The inbound header names.
	 */
	public void setInboundHeaderNames(String... inboundHeaderNamesArg) {
		this.inboundHeaderNames =
				inboundHeaderNamesArg != null
						? Arrays.copyOf(inboundHeaderNamesArg, inboundHeaderNamesArg.length)
						: new String[0];
		this.inboundHeaderNamesLower = new String[this.inboundHeaderNames.length];
		for (int i = 0; i < this.inboundHeaderNames.length; i++) {
			if (HTTP_REQUEST_HEADER_NAME_PATTERN.equals(this.inboundHeaderNames[i])
					|| HTTP_RESPONSE_HEADER_NAME_PATTERN.equals(this.inboundHeaderNames[i])) {
				this.inboundHeaderNamesLower[i] = this.inboundHeaderNames[i];
			}
			else {
				this.inboundHeaderNamesLower[i] = this.inboundHeaderNames[i].toLowerCase();
			}
		}
	}

	/**
	 * Provide header names from the list of standard headers that should be suppressed when
	 * mapping outbound endpoint request headers.
	 * @param excludedOutboundStandardRequestHeaderNames the excludedStandardRequestHeaderNames to set
	 */
	public void setExcludedOutboundStandardRequestHeaderNames(String... excludedOutboundStandardRequestHeaderNames) {
		Assert.notNull(excludedOutboundStandardRequestHeaderNames,
				"'excludedOutboundStandardRequestHeaderNames' must not be null");
		Assert.noNullElements(excludedOutboundStandardRequestHeaderNames,
				"'excludedOutboundStandardRequestHeaderNames' must not have null elements");
		this.excludedOutboundStandardRequestHeaderNames = Arrays.copyOf(excludedOutboundStandardRequestHeaderNames,
				excludedOutboundStandardRequestHeaderNames.length);
	}

	/**
	 * Provide header names from the list of standard headers that should be suppressed when
	 * mapping inbound endpoint response headers.
	 * @param excludedInboundStandardResponseHeaderNames the excludedStandardResponseHeaderNames to set
	 */
	public void setExcludedInboundStandardResponseHeaderNames(String... excludedInboundStandardResponseHeaderNames) {
		Assert.notNull(excludedInboundStandardResponseHeaderNames,
				"'excludedInboundStandardResponseHeaderNames' must not be null");
		Assert.noNullElements(excludedInboundStandardResponseHeaderNames,
				"'excludedInboundStandardResponseHeaderNames' must not have null elements");
		this.excludedInboundStandardResponseHeaderNames = Arrays.copyOf(excludedInboundStandardResponseHeaderNames,
				excludedInboundStandardResponseHeaderNames.length);
	}

	/**
	 * Sets the prefix to use with user-defined (non-standard) headers. The default is an
	 * empty string.
	 * @param userDefinedHeaderPrefix The user defined header prefix.
	 */
	public void setUserDefinedHeaderPrefix(String userDefinedHeaderPrefix) {
		this.userDefinedHeaderPrefix = (userDefinedHeaderPrefix != null) ? userDefinedHeaderPrefix : "";
	}

	@Override
	public void afterPropertiesSet() {
		if (this.beanFactory != null) {
			this.conversionService = IntegrationUtils.getConversionService(this.beanFactory);
		}
	}

	/**
	 * Map from the integration MessageHeaders to an HttpHeaders instance.
	 * Depending on which type of adapter is using this mapper, the HttpHeaders might be
	 * for an HTTP request (outbound adapter) or for an HTTP response (inbound adapter).
	 */
	@Override
	public void fromHeaders(MessageHeaders headers, HttpHeaders target) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("outboundHeaderNames=" + Arrays.toString(this.outboundHeaderNames));
		}
		for (Entry<String, Object> entry : headers.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			String lowerName = name.toLowerCase();
			if (value != null && shouldMapOutboundHeader(lowerName)) {
				if (!HTTP_REQUEST_HEADER_NAMES_LOWER.contains(lowerName) &&
						!HTTP_RESPONSE_HEADER_NAMES_LOWER.contains(lowerName) &&
						!MessageHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
					// prefix the user-defined header names if not already prefixed
					name =
							StringUtils.startsWithIgnoreCase(name, this.userDefinedHeaderPrefix)
									? name
									: this.userDefinedHeaderPrefix + name;
				}
				if (this.logger.isDebugEnabled()) {
					this.logger.debug(MessageFormat.format("setting headerName=[{0}], value={1}", name, value));
				}
				setHttpHeader(target, name, value);
			}
		}
	}

	private void setHttpHeader(HttpHeaders target, String name, Object value) { // NOSONAR
		switch (name.toLowerCase()) {
			case ACCEPT_LOWER:
				setAccept(target, value);
				break;
			case ACCEPT_CHARSET_LOWER:
				setAcceptCharset(target, value);
				break;
			case ALLOW_LOWER:
				setAllow(target, value);
				break;
			case CACHE_CONTROL_LOWER:
				setCacheControl(target, value);
				break;
			case CONTENT_LENGTH_LOWER:
				setContentLength(target, value);
				break;
			case "contenttype": // Lower case for MessageHeaders.CONTENT_TYPE
				setContentType(target, value);
				break;
			case DATE_LOWER:
				setDate(target, value);
				break;
			case ETAG_LOWER:
				setETag(target, value);
				break;
			case EXPIRES_LOWER:
				setExpires(target, value);
				break;
			case IF_MODIFIED_SINCE_LOWER:
				setIfModifiedSince(target, value);
				break;
			case IF_UNMODIFIED_SINCE_LOWER:
				setIfUnmodifiedSince(target, value);
				break;
			case IF_NONE_MATCH_LOWER:
				setIfNoneMatch(target, value);
				break;
			case LAST_MODIFIED_LOWER:
				setLastModified(target, value);
				break;
			case LOCATION_LOWER:
				setLocation(target, value);
				break;
			case PRAGMA_LOWER:
				setPragma(target, value);
				break;
			default:
				if (value instanceof String) {
					target.set(name, (String) value);
				}
				else if (value instanceof String[]) {
					target.addAll(name, Arrays.asList((String[]) value));
				}
				else if (value instanceof Iterable<?>) {
					setIterableHeader(target, name, value);
				}
				else {
					setPlainHeader(target, name, value);
				}
		}
	}

	private void setAccept(HttpHeaders target, Object value) {
		Collection<?> valuesToAccept = valueToCollection(value);
		if (!CollectionUtils.isEmpty(valuesToAccept)) {
			List<MediaType> acceptableMediaTypes = new ArrayList<>();
			for (Object type : valuesToAccept) {
				if (type instanceof MimeType) {
					acceptableMediaTypes.add(MediaType.asMediaType((MimeType) type));
				}
				else if (type instanceof String) {
					acceptableMediaTypes.addAll(MediaType.parseMediaTypes((String) type));
				}
				else {
					throwIllegalArgumentForUnexpectedValue(
							"Expected org.springframework.util.MimeType " +
									"or String value for 'Accept' header value, but received: ", type);

				}
			}
			target.setAccept(acceptableMediaTypes);
		}
	}

	private void setAcceptCharset(HttpHeaders target, Object value) {
		Collection<?> valuesToConvert = valueToCollection(value);
		if (!CollectionUtils.isEmpty(valuesToConvert)) {
			List<Charset> acceptableCharsets = new ArrayList<>();
			for (Object charset : valuesToConvert) {
				if (charset instanceof Charset) {
					acceptableCharsets.add((Charset) charset);
				}
				else if (charset instanceof String) {
					String[] charsets = StringUtils.delimitedListToStringArray((String) charset, ",", " ");
					for (String charset2 : charsets) {
						acceptableCharsets.add(Charset.forName(charset2));
					}
				}
				else {
					throwIllegalArgumentForUnexpectedValue(
							"Expected Charset or String value for 'Accept-Charset' header value, but received: ",
							charset);
				}
			}
			target.setAcceptCharset(acceptableCharsets);
		}
	}

	private void setAllow(HttpHeaders target, Object value) {
		Collection<?> valuesToConvert = valueToCollection(value);
		if (!CollectionUtils.isEmpty(valuesToConvert)) {
			Set<HttpMethod> allowedMethods = new HashSet<>();
			for (Object method : valuesToConvert) {
				if (method instanceof HttpMethod) {
					allowedMethods.add((HttpMethod) method);
				}
				else if (method instanceof String) {
					String[] methods = StringUtils.delimitedListToStringArray((String) method, ",", " ");
					for (String method2 : methods) {
						allowedMethods.add(HttpMethod.valueOf(method2));
					}
				}
				else {
					throwIllegalArgumentForUnexpectedValue(
							"Expected HttpMethod or String value for 'Allow' header value, but received: ", method);
				}
			}
			target.setAllow(allowedMethods);
		}
	}

	private Collection<?> valueToCollection(Object value) {
		Collection<?> valuesToConvert;
		if (value instanceof Collection<?>) {
			valuesToConvert = (Collection<?>) value;
		}
		else if (value.getClass().isArray()) {
			valuesToConvert = Arrays.asList(ObjectUtils.toObjectArray(value));
		}
		else {
			valuesToConvert = Collections.singleton(value);
		}
		return valuesToConvert;
	}

	private void setCacheControl(HttpHeaders target, Object value) {
		if (value instanceof String) {
			target.setCacheControl((String) value);
		}
		else {
			throwIllegalArgumentForUnexpectedValue(
					"Expected String value for 'Cache-Control' header value, but received: ", value);
		}
	}

	private void setContentLength(HttpHeaders target, Object value) {
		if (value instanceof Number) {
			target.setContentLength(((Number) value).longValue());
		}
		else if (value instanceof String) {
			target.setContentLength(Long.parseLong((String) value));
		}
		else {
			throwIllegalArgumentForUnexpectedValue(
					"Expected Number or String value for 'Content-Length' header value, but received: ", value);
		}
	}

	private void setContentType(HttpHeaders target, Object value) {
		if (value instanceof MimeType) {
			target.setContentType(MediaType.asMediaType((MimeType) value));
		}
		else if (value instanceof String) {
			target.setContentType(MediaType.parseMediaType((String) value));
		}
		else {
			throwIllegalArgumentForUnexpectedValue(
					"Expected org.springframework.util.MimeType " +
							"or String value for 'Content-Type' header value, but received: ", value);
		}
	}

	private void setDate(HttpHeaders target, Object value) {
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
			catch (@SuppressWarnings(UNUSED) NumberFormatException e) {
				target.setDate(getFirstDate((String) value, HttpHeaders.DATE));
			}
		}
		else {
			throwIllegalArgumentForUnexpectedValue(
					"Expected Date, Number, or String value for 'Date' header value, but received: ", value);
		}
	}

	private void setETag(HttpHeaders target, Object value) {
		if (value instanceof String) {
			target.setETag((String) value);
		}
		else {
			throwIllegalArgumentForUnexpectedValue("Expected String value for 'ETag' header value, but received: ",
					value);
		}
	}

	private void setExpires(HttpHeaders target, Object value) {
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
			catch (@SuppressWarnings(UNUSED) NumberFormatException e) {
				target.setExpires(getFirstDate((String) value, HttpHeaders.EXPIRES));
			}
		}
		else {
			throwIllegalArgumentForUnexpectedValue(
					"Expected Date, Number, or String value for 'Expires' header value, but received: ", value);
		}
	}

	private void setIfModifiedSince(HttpHeaders target, Object value) {
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
			catch (@SuppressWarnings(UNUSED) NumberFormatException e) {
				target.setIfModifiedSince(getFirstDate((String) value, HttpHeaders.IF_MODIFIED_SINCE));
			}
		}
		else {
			throwIllegalArgumentForUnexpectedValue(
					"Expected Date, Number, or String value for 'If-Modified-Since' header value, but received: ",
					value);
		}
	}

	private void setIfUnmodifiedSince(HttpHeaders target, Object value) {
		if (value instanceof Date) {
			target.setIfUnmodifiedSince(((Date) value).getTime());
		}
		else if (value instanceof Number) {
			target.setIfUnmodifiedSince(((Number) value).longValue());
		}
		else if (value instanceof String) {
			try {
				target.setIfUnmodifiedSince(Long.parseLong((String) value));
			}
			catch (@SuppressWarnings(UNUSED) NumberFormatException e) {
				target.setIfUnmodifiedSince(getFirstDate((String) value, HttpHeaders.IF_UNMODIFIED_SINCE));
			}
		}
		else {
			throwIllegalArgumentForUnexpectedValue(
					"Expected Date, Number, or String value for 'If-Unmodified-Since' header value, but received: ",
					value);
		}
	}

	private void setIfNoneMatch(HttpHeaders target, Object value) {
		Collection<?> valuesToAccept = valueToCollection(value);
		List<String> ifNoneMatchList = new ArrayList<>();

		for (Object match : valuesToAccept) {
			if (match instanceof String) {
				ifNoneMatchList.add((String) match);
			}
			else {
				throwIllegalArgumentForUnexpectedValue(
						"Expected String value for 'If-None-Match' header value, but received: ", value);
			}
		}

		if (!ifNoneMatchList.isEmpty()) {
			target.setIfNoneMatch(ifNoneMatchList);
		}
	}

	private void setLastModified(HttpHeaders target, Object value) {
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
			catch (@SuppressWarnings(UNUSED) NumberFormatException e) {
				target.setLastModified(getFirstDate((String) value, HttpHeaders.LAST_MODIFIED));
			}
		}
		else {
			throwIllegalArgumentForUnexpectedValue(
					"Expected Date, Number, or String value for 'Last-Modified' header value, but received: ", value);
		}
	}

	private void setLocation(HttpHeaders target, Object value) {
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
			throwIllegalArgumentForUnexpectedValue(
					"Expected URI or String value for 'Location' header value, but received: ", value);
		}
	}

	private void setPragma(HttpHeaders target, Object value) {
		if (value instanceof String) {
			target.setPragma((String) value);
		}
		else {
			throwIllegalArgumentForUnexpectedValue("Expected String value for 'Pragma' header value, but received: ",
					value);
		}
	}

	private void throwIllegalArgumentForUnexpectedValue(String message, @Nullable Object value) {
		throw new IllegalArgumentException(message + (value != null ? value.getClass() : null));
	}

	private void setIterableHeader(HttpHeaders target, String name, Object value) {
		for (Object next : (Iterable<?>) value) {
			String convertedValue;
			if (next instanceof String) {
				convertedValue = (String) next;
			}
			else {
				convertedValue = convertToString(value);
			}
			if (StringUtils.hasText(convertedValue)) {
				target.add(name, convertedValue);
			}
			else {
				this.logger.warn("Element of the header '" + name + "' with value '" + value +
						"' will not be set since it is not a String and no Converter is available. " +
						"Consider registering a Converter with ConversionService (e.g., <int:converter>)");
			}
		}
	}

	private void setPlainHeader(HttpHeaders target, String name, Object value) {
		String convertedValue = convertToString(value);
		if (StringUtils.hasText(convertedValue)) {
			target.set(name, convertedValue);
		}
		else {
			this.logger.warn("Header '" + name + "' with value '" + value +
					"' will not be set since it is not a String and no Converter is available. " +
					"Consider registering a Converter with ConversionService (e.g., <int:converter>)");
		}
	}

	/**
	 * Map from an HttpHeaders instance to integration MessageHeaders.
	 * Depending on which type of adapter is using this mapper, the HttpHeaders might be
	 * from an HTTP request (inbound adapter) or from an HTTP response (outbound adapter).
	 */
	@Override
	public Map<String, Object> toHeaders(HttpHeaders source) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("inboundHeaderNames=" + Arrays.toString(this.inboundHeaderNames));
		}
		Map<String, Object> target = new HashMap<>();
		Set<String> headerNames = source.keySet();
		for (String name : headerNames) {
			String lowerName = name.toLowerCase();
			if (shouldMapInboundHeader(lowerName)) {
				if (!HTTP_REQUEST_HEADER_NAMES_LOWER.contains(lowerName)
						&& !HTTP_RESPONSE_HEADER_NAMES_LOWER.contains(lowerName)) {
					populateUserDefinedHeader(source, target, name);
				}
				else {
					populateStandardHeader(source, target, name);
				}
			}
		}
		return target;
	}

	private void populateUserDefinedHeader(HttpHeaders source, Map<String, Object> target, String name) {
		String prefixedName = StringUtils.startsWithIgnoreCase(name, this.userDefinedHeaderPrefix)
				? name
				: this.userDefinedHeaderPrefix + name;
		Object value = source.containsKey(prefixedName)
				? getHttpHeader(source, prefixedName)
				: getHttpHeader(source, name);
		if (value != null) {
			setMessageHeader(target, name, value);
		}
	}

	private void populateStandardHeader(HttpHeaders source, Map<String, Object> target, String name) {
		Object value = getHttpHeader(source, name);
		if (value != null) {
			setMessageHeader(target,
					HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name) ? MessageHeaders.CONTENT_TYPE : name, value);
		}
	}

	protected Object getHttpHeader(HttpHeaders source, String name) { // NOSONAR
		switch (name.toLowerCase()) {
			case ACCEPT_LOWER:
				return source.getAccept();
			case ACCEPT_CHARSET_LOWER:
				return source.getAcceptCharset();
			case ALLOW_LOWER:
				return source.getAllow();
			case CACHE_CONTROL_LOWER:
				String cacheControl = source.getCacheControl();
				return (StringUtils.hasText(cacheControl)) ? cacheControl : null;
			case CONTENT_LENGTH_LOWER:
				long contentLength = source.getContentLength();
				return (contentLength > -1) ? contentLength : null;
			case CONTENT_TYPE_LOWER:
				return source.getContentType();
			case DATE_LOWER:
				long date = source.getDate();
				return (date > -1) ? date : null;
			case ETAG_LOWER:
				String eTag = source.getETag();
				return (StringUtils.hasText(eTag)) ? eTag : null;
			case EXPIRES_LOWER:
				long expires = source.getExpires();
				return (expires > -1) ? expires : null;
			case IF_NONE_MATCH_LOWER:
				return source.getIfNoneMatch();
			case IF_MODIFIED_SINCE_LOWER:
				long modifiedSince = source.getIfModifiedSince();
				return (modifiedSince > -1) ? modifiedSince : null;
			case IF_UNMODIFIED_SINCE_LOWER:
				long unmodifiedSince = source.getIfUnmodifiedSince();
				return (unmodifiedSince > -1) ? unmodifiedSince : null;
			case LAST_MODIFIED_LOWER:
				long lastModified = source.getLastModified();
				return (lastModified > -1) ? lastModified : null;
			case LOCATION_LOWER:
				return source.getLocation();
			case PRAGMA_LOWER:
				String pragma = source.getPragma();
				return (StringUtils.hasText(pragma)) ? pragma : null;
			default:
				return source.get(name);
		}
	}

	private void setMessageHeader(Map<String, Object> target, String name, Object value) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(MessageFormat.format("setting headerName=[{0}], value={1}", name, value));
		}
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

	private boolean shouldMapOutboundHeader(String headerName) {
		String[] outboundHeaderNamesLower = this.outboundHeaderNamesLowerWithContentType;

		if (this.isDefaultInboundMapper) {
			/*
			 * When using the default response header name list, suppress the
			 * mapping of exclusions for specific headers.
			 */
			if (containsElementIgnoreCase(this.excludedInboundStandardResponseHeaderNames, headerName)) {
				if (this.logger.isDebugEnabled()) {
					this.logger
							.debug(MessageFormat.format("headerName=[{0}] WILL NOT be mapped (excluded)", headerName));
				}
				return false;
			}
		}
		else if (this.isDefaultOutboundMapper) {
			outboundHeaderNamesLower = this.outboundHeaderNamesLowerWithContentType;
			/*
			 * When using the default request header name list, suppress the
			 * mapping of exclusions for specific headers.
			 */
			if (containsElementIgnoreCase(this.excludedOutboundStandardRequestHeaderNames, headerName)) {
				if (this.logger.isDebugEnabled()) {
					this.logger
							.debug(MessageFormat.format("headerName=[{0}] WILL NOT be mapped (excluded)", headerName));
				}
				return false;
			}
		}
		return shouldMapHeader(headerName, outboundHeaderNamesLower);
	}

	protected final boolean shouldMapInboundHeader(String headerName) {
		return shouldMapHeader(headerName, this.inboundHeaderNamesLower);
	}

	/**
	 * @param headerName the header name (lower cased).
	 * @param patterns the patterns (lower cased).
	 * @return true if should be mapped.
	 */
	private boolean shouldMapHeader(String headerName, String[] patterns) {
		if (patterns != null && patterns.length > 0) {
			for (String pattern : patterns) {
				if (matchHeaderForPattern(headerName, pattern)) {
					return true;
				}
			}
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(MessageFormat.format("headerName=[{0}] WILL NOT be mapped", headerName));
		}
		return false;
	}

	private boolean matchHeaderForPattern(String headerName, String pattern) {
		if (PatternMatchUtils.simpleMatch(pattern, headerName)) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}",
						headerName, pattern));
			}
			return true;
		}
		else if (HTTP_REQUEST_HEADER_NAME_PATTERN.equals(pattern)
				&& HTTP_REQUEST_HEADER_NAMES_LOWER.contains(headerName)) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}",
						headerName, pattern));
			}
			return true;
		}
		else if (HTTP_RESPONSE_HEADER_NAME_PATTERN.equals(pattern)
				&& HTTP_RESPONSE_HEADER_NAMES_LOWER.contains(headerName)) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(MessageFormat.format("headerName=[{0}] WILL be mapped, matched pattern={1}",
						headerName, pattern));
			}
			return true;
		}
		return false;
	}

	@Nullable
	protected String convertToString(Object value) {
		if (this.conversionService != null &&
				this.conversionService.canConvert(TypeDescriptor.forObject(value),
						TypeDescriptor.valueOf(String.class))) {

			return this.conversionService.convert(value, String.class);
		}
		return null;
	}

	// Utility methods

	protected static boolean containsElementIgnoreCase(String[] headerNames, String name) {
		for (String headerName : headerNames) {
			if (headerName.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	protected static long getFirstDate(String headerValue, String headerName) {
		for (DateTimeFormatter dateFormatter : DATE_FORMATS) {
			try {
				return ZonedDateTime.parse(headerValue, dateFormatter)
						.toInstant()
						.toEpochMilli();
			}
			catch (@SuppressWarnings(UNUSED) DateTimeParseException ex) {
				// ignore
			}
		}

		throw new IllegalArgumentException("Cannot parse date value '" + headerValue + "' for '" + headerName
				+ "' header");
	}

	/**
	 * Factory method for creating a basic outbound mapper instance.
	 * This will map all standard HTTP request headers when sending an HTTP request,
	 * and it will map all standard HTTP response headers when receiving an HTTP response.
	 * @return The default outbound mapper.
	 */
	public static DefaultHttpHeaderMapper outboundMapper() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		setupDefaultOutboundMapper(mapper);
		return mapper;
	}

	/**
	 * Subclasses can call this from a static outboundMapper() method to set up
	 * standard header mappings for an outbound mapper.
	 * @param mapper the mapper.
	 */
	protected static void setupDefaultOutboundMapper(DefaultHttpHeaderMapper mapper) {
		mapper.setOutboundHeaderNames(HTTP_REQUEST_HEADER_NAMES);
		mapper.setInboundHeaderNames(HTTP_RESPONSE_HEADER_NAMES);
		mapper.setExcludedOutboundStandardRequestHeaderNames(HTTP_REQUEST_HEADER_NAMES_OUTBOUND_EXCLUSIONS);
	}

	/**
	 * Factory method for creating a basic inbound mapper instance.
	 * This will map all standard HTTP request headers when receiving an HTTP request,
	 * and it will map all standard HTTP response headers when sending an HTTP response.
	 * @return The default inbound mapper.
	 */
	public static DefaultHttpHeaderMapper inboundMapper() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		setupDefaultInboundMapper(mapper);
		return mapper;
	}

	/**
	 * Subclasses can call this from a static inboundMapper() method to set up
	 * standard header mappings for an inbound mapper.
	 * @param mapper the mapper.
	 */
	protected static void setupDefaultInboundMapper(DefaultHttpHeaderMapper mapper) {
		mapper.setInboundHeaderNames(HTTP_REQUEST_HEADER_NAMES);
		mapper.setOutboundHeaderNames(HTTP_RESPONSE_HEADER_NAMES);
		mapper.setExcludedInboundStandardResponseHeaderNames(HTTP_RESPONSE_HEADER_NAMES_INBOUND_EXCLUSIONS);
	}

}
