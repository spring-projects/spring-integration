/*
 * Copyright 2002-2024 the original author or authors.
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DefaultHttpHeaderMapperFromMessageInboundTests {

	@Test
	public void validateAllowAsString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Allow", "GET");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAllow()).hasSize(1);
		assertThat(headers.getAllow().iterator().next()).isEqualTo(HttpMethod.GET);
	}

	@Test
	public void validateAllowAsStringCaseInsensitive() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("allow", "GET");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAllow()).hasSize(1);
		assertThat(headers.getAllow().iterator().next()).isEqualTo(HttpMethod.GET);
	}

	@Test
	public void validateAllowAsHttpMethod() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Allow", HttpMethod.GET);
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAllow()).hasSize(1);
		assertThat(headers.getAllow().iterator().next()).isEqualTo(HttpMethod.GET);
	}

	@Test
	public void validateAllowAsDelimitedString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Allow", "GET, POST");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAllow().size()).isEqualTo(2);
		assertThat(headers.getAllow().contains(HttpMethod.GET)).isTrue();
		assertThat(headers.getAllow().contains(HttpMethod.POST)).isTrue();
	}

	@Test
	public void validateAllowAsStringArray() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Allow", new String[] {"GET", "POST"});
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAllow().size()).isEqualTo(2);
		assertThat(headers.getAllow().contains(HttpMethod.GET)).isTrue();
		assertThat(headers.getAllow().contains(HttpMethod.POST)).isTrue();
	}

	@Test
	public void validateAllowAsHttpMethodArray() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Allow", new HttpMethod[] {HttpMethod.GET, HttpMethod.POST});
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAllow().size()).isEqualTo(2);
		assertThat(headers.getAllow().contains(HttpMethod.GET)).isTrue();
		assertThat(headers.getAllow().contains(HttpMethod.POST)).isTrue();
	}

	@Test
	public void validateAllowAsCollectionOfString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Allow", Arrays.asList("GET", "POST"));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAllow().size()).isEqualTo(2);
		assertThat(headers.getAllow().contains(HttpMethod.GET)).isTrue();
		assertThat(headers.getAllow().contains(HttpMethod.POST)).isTrue();
	}

	@Test
	public void validateAllowAsCollectionOfHttpMethods() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Allow", Arrays.asList(HttpMethod.GET, HttpMethod.POST));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAllow().size()).isEqualTo(2);
		assertThat(headers.getAllow().contains(HttpMethod.GET)).isTrue();
		assertThat(headers.getAllow().contains(HttpMethod.POST)).isTrue();
	}

	// Cache-Control tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests
	// Content-Length tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests
	// Content-Type tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests
	// Date tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests

	// ETag tests

	@Test
	public void validateETag() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("ETag", "\"1234\"");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getETag()).isEqualTo("\"1234\"");
	}

	// Expires tests

	@Test
	public void validateExpiresAsNumber() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Expires", 12345678);
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getExpires()).isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	@Test
	public void validateExpiresAsString() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Expires", "12345678");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getExpires()).isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	@Test
	public void validateExpiresAsDate() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Expires", new Date(12345678));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getExpires()).isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	// Last-Modified tests

	@Test
	public void validateLastModifiedAsNumber() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Last-Modified", 12345678);
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getLastModified())
				.isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	@Test
	public void validateLastModifiedAsString() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Last-Modified", "12345678");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getLastModified())
				.isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	@Test
	public void validateLastModifiedAsDate() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Last-Modified", new Date(12345678));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getLastModified())
				.isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	// Location tests

	@Test
	public void validateLocation() throws URISyntaxException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Location", "https://www.example.com/");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		assertThat(headers.getLocation().toString()).isEqualTo(new URI("https://www.example.com/").toString());
	}

	// Transfer Encoding tests

	@Test
	public void validateTransferEncodingNotMappedFromMessageHeaders() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Transfer-Encoding", "chunked");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		assertThat(headers.isEmpty()).as(String.format("'Headers' is not empty. It contains '%s' element(s).",
				headers.size())).isTrue();
	}

	@Test
	public void validateTransferEncodingMappedFromHttpHeaders() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames("Transfer-Encoding");
		HttpHeaders headers = new HttpHeaders();
		headers.set("Transfer-Encoding", "chunked");

		Map<String, ?> result = mapper.toHeaders(headers);
		assertThat(result).hasSize(1);
		assertThat(result.get("Transfer-Encoding")).isEqualTo("chunked");

	}

	@Test
	public void validateTransferEncodingNotMappedFromHttpHeadersByDefault() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();

		HttpHeaders headers = new HttpHeaders();
		headers.set("Transfer-Encoding", "chunked");

		Map<String, ?> result = mapper.toHeaders(headers);
		assertThat(result.isEmpty()).as(String.format("'result' is not empty. It contains '%s' element(s).",
				result.size())).isTrue();

	}

	// Pragma tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests

	@Test
	public void validateCustomHeaderNamesMappedToHttpHeaders() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames("foo", "bar");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("foo", "abc");
		messageHeaders.put("bar", "123");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.size()).isEqualTo(2);
		assertThat(headers.get("foo")).hasSize(1);
		assertThat(headers.getFirst("foo")).isEqualTo("abc");
		assertThat(headers.get("bar")).hasSize(1);
		assertThat(headers.getFirst("bar")).isEqualTo("123");
	}

	@Test
	public void validateCustomHeaderNamePatternsMappedToHttpHeaders() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames("x*", "*z", "a*f");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("x1", "x1-value");
		messageHeaders.put("1x", "1x-value");
		messageHeaders.put("z1", "z1-value");
		messageHeaders.put("1z", "1z-value");
		messageHeaders.put("abc", "abc-value");
		messageHeaders.put("def", "def-value");
		messageHeaders.put("abcdef", "abcdef-value");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.size()).isEqualTo(3);
		assertThat(headers.get("1x")).isNull();
		assertThat(headers.get("z1")).isNull();
		assertThat(headers.get("abc")).isNull();
		assertThat(headers.get("def")).isNull();
		assertThat(headers.get("x1")).hasSize(1);
		assertThat(headers.getFirst("x1")).isEqualTo("x1-value");
		assertThat(headers.get("1z")).hasSize(1);
		assertThat(headers.getFirst("1z")).isEqualTo("1z-value");
		assertThat(headers.get("abcdef")).hasSize(1);
		assertThat(headers.getFirst("abcdef")).isEqualTo("abcdef-value");
	}

	@Test
	public void validateCustomHeaderNamePatternsAndStandardResponseHeadersMappedToHttpHeaders() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames("foo*", "HTTP_RESPONSE_HEADERS");
		mapper.setUserDefinedHeaderPrefix("X-");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("Accept", "text/html");
		messageHeaders.put("Content-Type", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.size()).isEqualTo(2);
		assertThat(headers.getAccept().isEmpty()).isTrue();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_XML);
		assertThat(headers.get("X-foobar")).hasSize(1);
		assertThat(headers.getFirst("X-foobar")).isEqualTo("abc");
	}

	@Test
	public void validateCustomHeaderNamePatternsAndStandardResponseHeadersMappedToHttpHeadersWithCustomPrefix() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setUserDefinedHeaderPrefix("Z-");
		mapper.setOutboundHeaderNames("foo*", "HTTP_RESPONSE_HEADERS");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("Accept", "text/html");
		messageHeaders.put("Content-Type", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.size()).isEqualTo(2);
		assertThat(headers.getAccept().isEmpty()).isTrue();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_XML);
		assertThat(headers.get("Z-foobar")).hasSize(1);
		assertThat(headers.getFirst("Z-foobar")).isEqualTo("abc");
	}

	@Test
	void validateCustomHeaderNamePatternsAndStandardResponseHeadersMappedToHttpHeadersWithCustomPrefixEmptyString() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setUserDefinedHeaderPrefix("");
		mapper.setOutboundHeaderNames("foo*", "HTTP_RESPONSE_HEADERS");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("Accept", "text/html");
		messageHeaders.put("Content-Type", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.size()).isEqualTo(2);
		assertThat(headers.getAccept().isEmpty()).isTrue();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_XML);
		assertThat(headers.get("foobar")).hasSize(1);
		assertThat(headers.getFirst("foobar")).isEqualTo("abc");
	}

	@Test
	public void validateCustomHeaderNamePatternsAndStandardResponseHeadersMappedToHttpHeadersWithCustomPrefixNull() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setUserDefinedHeaderPrefix(null);
		mapper.setOutboundHeaderNames("foo*", "HTTP_RESPONSE_HEADERS");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("Accept", "text/html");
		messageHeaders.put("Content-Type", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.size()).isEqualTo(2);
		assertThat(headers.getAccept().isEmpty()).isTrue();
		assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_XML);
		assertThat(headers.get("foobar")).hasSize(1);
		assertThat(headers.getFirst("foobar")).isEqualTo("abc");
	}

	@Test
	public void validateCustomHeaderNamesMappedFromHttpHeaders() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames("foo", "bar");
		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "abc");
		headers.set("bar", "123");
		Map<String, ?> result = mapper.toHeaders(headers);
		assertThat(result.size()).isEqualTo(2);
		assertThat(result.get("foo")).isEqualTo("abc");
		assertThat(result.get("bar")).isEqualTo("123");
	}

	@Test
	public void validateCustomHeaderNamePatternsMappedFromHttpHeaders() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames("x*", "*z", "a*f");
		HttpHeaders headers = new HttpHeaders();
		headers.set("x1", "x1-value");
		headers.set("1x", "1x-value");
		headers.set("z1", "z1-value");
		headers.set("1z", "1z-value");
		headers.set("abc", "abc-value");
		headers.set("def", "def-value");
		headers.set("abcdef", "abcdef-value");
		Map<String, ?> result = mapper.toHeaders(headers);
		assertThat(result.size()).isEqualTo(3);
		assertThat(result.get("1x")).isNull();
		assertThat(result.get("z1")).isNull();
		assertThat(result.get("abc")).isNull();
		assertThat(result.get("def")).isNull();
		assertThat(result.get("x1")).isEqualTo("x1-value");
		assertThat(result.get("1z")).isEqualTo("1z-value");
		assertThat(result.get("abcdef")).isEqualTo("abcdef-value");
	}

	@Test
	public void validateCustomHeaderNamePatternsAndStandardRequestHeadersMappedFromHttpHeaders()
			throws URISyntaxException {

		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames("foo*", "HTTP_REQUEST_HEADERS");
		HttpHeaders headers = new HttpHeaders();
		headers.set("foobar", "abc");
		headers.setAccept(Collections.singletonList(MediaType.TEXT_XML));
		headers.setLocation(new URI("http://www.example.org"));
		Map<String, ?> result = mapper.toHeaders(headers);
		assertThat(result).hasSize(2);
		assertThat(result.get("Location")).isNull();
		assertThat(result.get("foobar")).isEqualTo("abc");
		assertThat(result.get("Accept")).isEqualTo(MediaType.TEXT_XML);
	}

	@Test
	public void validateCustomHeadersWithNonStringValuesAndNoConverter() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames("customHeader*");

		HttpHeaders headers = new HttpHeaders();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("customHeaderA", 123);
		messageHeaders.put("customHeaderB", new TestClass());

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.get("X-customHeaderA")).isNull();
		assertThat(headers.get("X-customHeaderB")).isNull();
	}

	@Test
	public void validateCustomHeadersWithNonStringValuesAndDefaultConverterOnly() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames("customHeader*");
		ConversionService cs = new DefaultConversionService();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("integrationConversionService", cs);
		mapper.setBeanFactory(beanFactory);
		mapper.afterPropertiesSet();

		HttpHeaders headers = new HttpHeaders();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("customHeaderA", 123);
		messageHeaders.put("customHeaderB", new TestClass());

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.get("customHeaderA")).isNotNull();
		assertThat(headers.get("customHeaderA").get(0)).isEqualTo("123");
		assertThat(headers.get("customHeaderB")).isNull();
	}

	@Test
	public void validateCustomHeadersWithNonStringValuesAndDefaultConverterWithCustomConverter() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames("customHeader*");
		GenericConversionService cs = new DefaultConversionService();
		cs.addConverter(new TestClassConverter());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("integrationConversionService", cs);
		mapper.setBeanFactory(beanFactory);
		mapper.afterPropertiesSet();

		HttpHeaders headers = new HttpHeaders();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("customHeaderA", 123);
		messageHeaders.put("customHeaderB", new TestClass());

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.get("customHeaderA")).isNotNull();
		assertThat(headers.get("customHeaderA").get(0)).isEqualTo("123");
		assertThat(headers.get("customHeaderB")).isNotNull();
		assertThat(headers.get("customHeaderB").get(0)).isEqualTo("TestClass.class");
	}

	@Test
	public void dontPropagateContentLength() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		HttpHeaders headers = new HttpHeaders();
		// suppressed in response on inbound, by default
		headers.put("Content-Length", Collections.singletonList("3"));
		Map<String, Object> messageHeaders = mapper.toHeaders(headers);
		headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.get("Content-Length")).isNull();
	}

	@Test
	public void testInt2995IfModifiedSince() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Date ifModifiedSince = new Date();
		long ifModifiedSinceTime = ifModifiedSince.getTime();
		HttpHeaders headers = new HttpHeaders();
		headers.setIfModifiedSince(ifModifiedSinceTime);
		Map<String, ?> result = mapper.toHeaders(headers);
		Calendar c = Calendar.getInstance();
		c.setTime(ifModifiedSince);
		c.set(Calendar.MILLISECOND, 0);
		assertThat(result.get("If-Modified-Since")).isEqualTo(c.getTimeInMillis());
	}

	@Test
	public void testContentTypeHeader() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.CONTENT_TYPE, "text/plain");
		MessageHeaders messageHeaders = new MessageHeaders(map);
		HttpHeaders httpHeaders = new HttpHeaders();
		mapper.fromHeaders(messageHeaders, httpHeaders);
		assertThat(httpHeaders.getContentType()).isEqualTo(MediaType.valueOf("text/plain"));
	}

	@Test
	public void testContentTypeInboundHeader() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.inboundMapper();
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE.toLowerCase(), "text/plain");
		Map<String, ?> result = mapper.toHeaders(headers);
		assertThat(result.get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MediaType.valueOf("text/plain"));
	}

	public static class TestClass {

	}

	public static class TestClassConverter implements Converter<TestClass, String> {

		@Override
		public String convert(TestClass source) {
			return "TestClass.class";
		}

	}

}
