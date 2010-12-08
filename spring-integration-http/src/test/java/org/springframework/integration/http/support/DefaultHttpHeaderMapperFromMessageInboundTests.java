/*
 * Copyright 2002-2010 the original author or authors.
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.util.CollectionUtils;


/**
 * @author Oleg Zhurakousky
 *
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
	@Ignore // we may want to allow that for consistency
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
		messageHeaders.put("ETag", "1234");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals("1234", headers.getETag());
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
	
	// Pragma tested as part of DefaultHttpHeaderMapperFromMessageOutboundTests
}
