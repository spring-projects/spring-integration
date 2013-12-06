/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageHeaders;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.util.CollectionUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.0.1
 */
public class DefaultHttpHeaderMapperFromMessageInboundTests {

	// Allow tests
	@Test(expected=IllegalArgumentException.class)
	public void validateAllowWithWrongMethodName(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Allow", "bar");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	}

	@Test
	public void validateAllowAsString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Allow", "GET");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(1, headers.getAllow().size());
		assertEquals(HttpMethod.GET, headers.getAllow().iterator().next());
	}

	@Test
	public void validateAllowAsStringCaseInsensitive(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("allow", "GET");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(1, headers.getAllow().size());
		assertEquals(HttpMethod.GET, headers.getAllow().iterator().next());
	}

	@Test
	public void validateAllowAsHttpMethod(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Allow", HttpMethod.GET);
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(1, headers.getAllow().size());
		assertEquals(HttpMethod.GET, headers.getAllow().iterator().next());
	}

	@Test
	public void validateAllowAsDelimitedString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Allow", "GET, POST");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAllow().size());
		assertTrue(headers.getAllow().contains(HttpMethod.GET));
		assertTrue(headers.getAllow().contains(HttpMethod.POST));
	}

	@Test
	public void validateAllowAsStringArray(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Allow", new String[]{"GET", "POST"});
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAllow().size());
		assertTrue(headers.getAllow().contains(HttpMethod.GET));
		assertTrue(headers.getAllow().contains(HttpMethod.POST));
	}

	@Test
	public void validateAllowAsHttpMethodArray(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Allow", new HttpMethod[]{HttpMethod.GET, HttpMethod.POST});
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAllow().size());
		assertTrue(headers.getAllow().contains(HttpMethod.GET));
		assertTrue(headers.getAllow().contains(HttpMethod.POST));
	}

	@Test
	public void validateAllowAsCollectionOfString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Allow", CollectionUtils.arrayToList(new String[]{"GET", "POST"}));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAllow().size());
		assertTrue(headers.getAllow().contains(HttpMethod.GET));
		assertTrue(headers.getAllow().contains(HttpMethod.POST));
	}

	@Test
	public void validateAllowAsCollectionOfHttpMethods(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Allow", CollectionUtils.arrayToList(new HttpMethod[]{HttpMethod.GET, HttpMethod.POST}));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAllow().size());
		assertTrue(headers.getAllow().contains(HttpMethod.GET));
		assertTrue(headers.getAllow().contains(HttpMethod.POST));
	}

	// Cache-Control tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests
	// Content-Length tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests
	// Content-Type tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests
	// Date tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests

	// ETag tests

	@Test
	public void validateETag(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("ETag", "\"1234\"");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals("\"1234\"", headers.getETag());
	}

	// Expires tests

	@Test
	public void validateExpiresAsNumber() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Expires", 12345678);
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getExpires());
	}

	@Test
	public void validateExpiresAsString() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Expires", "12345678");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getExpires());
	}
	@Test
	public void validateExpiresAsDate() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Expires", new Date(12345678));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getExpires());
	}

	// Last-Modified tests

	@Test
	public void validateLastModifiedAsNumber() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Last-Modified", 12345678);
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getLastModified());
	}

	@Test
	public void validateLastModifiedAsString() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Last-Modified", "12345678");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getLastModified());
	}
	@Test
	public void validateLastModifiedAsDate() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Last-Modified", new Date(12345678));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getLastModified());
	}

	// Location tests

	@Test
	public void validateLocation() throws Exception{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Location", "http://foo.com");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		assertEquals(new URI("http://foo.com").toString(), headers.getLocation().toString());
	}

	// Transfer Encoding tests

	@Test
	public void validateTransferEncodingNotMappedFromMessageHeaders() throws Exception{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Transfer-Encoding", "chunked");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		assertTrue(String.format("'Headers' is not empty. It contains '%s' element(s).",
				headers.size()), headers.isEmpty());
	}

	@Test
	public void validateTransferEncodingMappedFromHttpHeaders() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames(new String[] {"Transfer-Encoding"});
		HttpHeaders headers = new HttpHeaders();
		headers.set("Transfer-Encoding", "chunked");

		Map<String, ?> result = mapper.toHeaders(headers);
		assertEquals(1, result.size());
		assertEquals("chunked", result.get("Transfer-Encoding"));

	}

	@Test
	public void validateTransferEncodingNotMappedFromHttpHeadersByDefault() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();

		HttpHeaders headers = new HttpHeaders();
		headers.set("Transfer-Encoding", "chunked");

		Map<String, ?> result = mapper.toHeaders(headers);
		assertTrue(String.format("'result' is not empty. It contains '%s' element(s).",
				result.size()), result.isEmpty());

	}

	// Pragma tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests

	@Test
	public void validateCustomHeaderNamesMappedToHttpHeaders() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames(new String[] {"foo", "bar"});
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("foo", "abc");
		messageHeaders.put("bar", "123");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.size());
		assertEquals(1, headers.get("X-foo").size());
		assertEquals("abc", headers.getFirst("X-foo"));
		assertEquals(1, headers.get("X-bar").size());
		assertEquals("123", headers.getFirst("X-bar"));
	}

	@Test
	public void validateCustomHeaderNamePatternsMappedToHttpHeaders() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames(new String[] {"x*", "*z", "a*f"});
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("x1", "x1-value");
		messageHeaders.put("1x", "1x-value");
		messageHeaders.put("z1", "z1-value");
		messageHeaders.put("1z", "1z-value");
		messageHeaders.put("abc", "abc-value");
		messageHeaders.put("def", "def-value");
		messageHeaders.put("abcdef", "abcdef-value");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(3, headers.size());
		assertNull(headers.get("1x"));
		assertNull(headers.get("z1"));
		assertNull(headers.get("abc"));
		assertNull(headers.get("def"));
		assertEquals(1, headers.get("X-x1").size());
		assertEquals("x1-value", headers.getFirst("X-x1"));
		assertEquals(1, headers.get("X-1z").size());
		assertEquals("1z-value", headers.getFirst("X-1z"));
		assertEquals(1, headers.get("X-abcdef").size());
		assertEquals("abcdef-value", headers.getFirst("X-abcdef"));
	}

	@Test
	public void validateCustomHeaderNamePatternsAndStandardResponseHeadersMappedToHttpHeaders() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames(new String[] {"foo*", "HTTP_RESPONSE_HEADERS"});
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("Accept", "text/html");
		messageHeaders.put("Content-Type", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.size());
		assertTrue(headers.getAccept().isEmpty());
		assertEquals(MediaType.TEXT_XML, headers.getContentType());
		assertEquals(1, headers.get("X-foobar").size());
		assertEquals("abc", headers.getFirst("X-foobar"));
	}

	@Test
	public void validateCustomHeaderNamePatternsAndStandardResponseHeadersMappedToHttpHeadersWithCustomPrefix() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setUserDefinedHeaderPrefix("Z-");
		mapper.setOutboundHeaderNames(new String[] {"foo*", "HTTP_RESPONSE_HEADERS"});
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("Accept", "text/html");
		messageHeaders.put("Content-Type", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.size());
		assertTrue(headers.getAccept().isEmpty());
		assertEquals(MediaType.TEXT_XML, headers.getContentType());
		assertEquals(1, headers.get("Z-foobar").size());
		assertEquals("abc", headers.getFirst("Z-foobar"));
	}

	@Test
	public void validateCustomHeaderNamePatternsAndStandardResponseHeadersMappedToHttpHeadersWithCustomPrefixEmptyString() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setUserDefinedHeaderPrefix("");
		mapper.setOutboundHeaderNames(new String[] {"foo*", "HTTP_RESPONSE_HEADERS"});
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("Accept", "text/html");
		messageHeaders.put("Content-Type", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.size());
		assertTrue(headers.getAccept().isEmpty());
		assertEquals(MediaType.TEXT_XML, headers.getContentType());
		assertEquals(1, headers.get("foobar").size());
		assertEquals("abc", headers.getFirst("foobar"));
	}

	@Test
	public void validateCustomHeaderNamePatternsAndStandardResponseHeadersMappedToHttpHeadersWithCustomPrefixNull() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setUserDefinedHeaderPrefix(null);
		mapper.setOutboundHeaderNames(new String[] {"foo*", "HTTP_RESPONSE_HEADERS"});
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("Accept", "text/html");
		messageHeaders.put("Content-Type", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.size());
		assertTrue(headers.getAccept().isEmpty());
		assertEquals(MediaType.TEXT_XML, headers.getContentType());
		assertEquals(1, headers.get("foobar").size());
		assertEquals("abc", headers.getFirst("foobar"));
	}

	@Test
	public void validateCustomHeaderNamesMappedFromHttpHeaders() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames(new String[] {"foo", "bar"});
		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "abc");
		headers.set("bar", "123");
		Map<String, ?> result = mapper.toHeaders(headers);
		assertEquals(2, result.size());
		assertEquals("abc", result.get("foo"));
		assertEquals("123", result.get("bar"));
	}

	@Test
	public void validateCustomHeaderNamePatternsMappedFromHttpHeaders() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames(new String[] {"x*", "*z", "a*f"});
		HttpHeaders headers = new HttpHeaders();
		headers.set("x1", "x1-value");
		headers.set("1x", "1x-value");
		headers.set("z1", "z1-value");
		headers.set("1z", "1z-value");
		headers.set("abc", "abc-value");
		headers.set("def", "def-value");
		headers.set("abcdef", "abcdef-value");
		Map<String, ?> result = mapper.toHeaders(headers);
		assertEquals(3, result.size());
		assertNull(result.get("1x"));
		assertNull(result.get("z1"));
		assertNull(result.get("abc"));
		assertNull(result.get("def"));
		assertEquals("x1-value", result.get("x1"));
		assertEquals("1z-value", result.get("1z"));
		assertEquals("abcdef-value", result.get("abcdef"));
	}

	@Test
	public void validateCustomHeaderNamePatternsAndStandardRequestHeadersMappedFromHttpHeaders() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames(new String[] {"foo*", "HTTP_REQUEST_HEADERS"});
		HttpHeaders headers = new HttpHeaders();
		headers.set("foobar", "abc");
		headers.setAccept(Collections.singletonList(MediaType.TEXT_XML));
		headers.setLocation(new URI("http://example.org"));
		Map<String, ?> result = mapper.toHeaders(headers);
		assertEquals(2, result.size());
		assertNull(result.get("Location"));
		assertEquals("abc", result.get("foobar"));
		assertEquals(MediaType.TEXT_XML, result.get("Accept"));
	}

	@Test
	public void validateCustomHeadersWithNonStringValuesAndNoConverter() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames(new String[] {"customHeader*"});

		HttpHeaders headers = new HttpHeaders();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("customHeaderA", 123);
		messageHeaders.put("customHeaderB", new TestClass());

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertNull(headers.get("X-customHeaderA"));
		assertNull(headers.get("X-customHeaderB"));
	}


	@Test
	public void validateCustomHeadersWithNonStringValuesAndDefaultConverterOnly() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames(new String[] {"customHeader*"});
		ConversionService cs = new DefaultConversionService();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("integrationConversionService", cs);
		mapper.setBeanFactory(beanFactory);
		mapper.afterPropertiesSet();

		HttpHeaders headers = new HttpHeaders();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("customHeaderA", 123);
		messageHeaders.put("customHeaderB", new TestClass());

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertNotNull(headers.get("X-customHeaderA"));
		assertEquals("123", headers.get("X-customHeaderA").get(0));
		assertNull(headers.get("X-customHeaderB"));
	}

	@Test
	public void validateCustomHeadersWithNonStringValuesAndDefaultConverterWithCustomConverter() throws Exception{
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames(new String[] {"customHeader*"});
		GenericConversionService cs = new DefaultConversionService();
		cs.addConverter(new TestClassConverter());
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("integrationConversionService", cs);
		mapper.setBeanFactory(beanFactory);
		mapper.afterPropertiesSet();

		HttpHeaders headers = new HttpHeaders();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("customHeaderA", 123);
		messageHeaders.put("customHeaderB", new TestClass());

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertNotNull(headers.get("X-customHeaderA"));
		assertEquals("123", headers.get("X-customHeaderA").get(0));
		assertNotNull(headers.get("X-customHeaderB"));
		assertEquals("TestClass.class", headers.get("X-customHeaderB").get(0));
	}

    @Test
    public void dontPropagateContentLength() {
        HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
        HttpHeaders headers = new HttpHeaders();
        // suppressed in response on inbound, by default
        headers.put("Content-Length", Arrays.asList("3"));
        Map<String, Object> messageHeaders = mapper.toHeaders(headers);
        headers = new HttpHeaders();
        mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
        assertNull(headers.get("Content-Length"));
    }

	@Test
	public void testInt2995IfModifiedSince() throws Exception{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.inboundMapper();
		Date ifModifiedSince = new Date();
		long ifModifiedSinceTime = ifModifiedSince.getTime();
		HttpHeaders headers = new HttpHeaders();
		headers.setIfModifiedSince(ifModifiedSinceTime);
		Map<String, ?> result = mapper.toHeaders(headers);
		Calendar c = Calendar.getInstance();
		c.setTime(ifModifiedSince);
		c.set(Calendar.MILLISECOND, 0);
		assertEquals(c.getTimeInMillis(), result.get("If-Modified-Since"));
	}

	public static class TestClass {

	}

	public static class TestClassConverter implements Converter<TestClass, String>{

		@Override
		public String convert(TestClass source) {
			return "TestClass.class";
		}

	}

}
