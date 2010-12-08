/**
 * 
 */
package org.springframework.integration.http.support;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.util.CollectionUtils;

/**
 * @author ozhurakousky
 *
 */
public class DefaultHttpHeaderMapperFromMessageOutboundTests {

	// ACCEPT tests
	@Test(expected=IllegalArgumentException.class)
	public void validateAcceptHeaderWithNoSlash(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept", "bar");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	}
	
	@Test
	public void validateAcceptHeaderSingleString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept", "bar/foo");

		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals("bar", headers.getAccept().get(0).getType());
		assertEquals("foo", headers.getAccept().get(0).getSubtype());
	}
	
	@Test
	public void validateAcceptHeaderSingleMediaType(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept", new MediaType("bar", "foo"));

		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals("bar", headers.getAccept().get(0).getType());
		assertEquals("foo", headers.getAccept().get(0).getSubtype());
	}
	
	@Test
	public void validateAcceptHeaderMultipleAsDelimitedString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept", "bar/foo, text/xml");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAccept().size());
		assertEquals("bar/foo", headers.getAccept().get(0).toString());
		assertEquals("text/xml", headers.getAccept().get(1).toString());
	}
	
	@Test
	public void validateAcceptHeaderMultipleAsStringArray(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept", new String[]{"bar/foo", "text/xml"});
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAccept().size());
		assertEquals("bar/foo", headers.getAccept().get(0).toString());
		assertEquals("text/xml", headers.getAccept().get(1).toString());
	}
	
	@Test
	public void validateAcceptHeaderMultipleAsStringCollection(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept", CollectionUtils.arrayToList(new String[]{"bar/foo", "text/xml"}));
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAccept().size());
		assertEquals("bar/foo", headers.getAccept().get(0).toString());
		assertEquals("text/xml", headers.getAccept().get(1).toString());
	}
	
	@Test
	public void validateAcceptHeaderMultipleAsMediatypeCollection(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept", 
				CollectionUtils.arrayToList(new MediaType[]{new MediaType("bar", "foo"), new MediaType("text", "xml")}));
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAccept().size());
		assertEquals("bar/foo", headers.getAccept().get(0).toString());
		assertEquals("text/xml", headers.getAccept().get(1).toString());
	}
	
	// ACCEPT_CHARSET tests
	
	@Test(expected=UnsupportedCharsetException.class)
	public void validateAcceptCharsetHeaderWithWrongCharset(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept-Charset", "foo");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	}
	
	@Test
	public void validateAcceptCharsetHeaderSingleString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept-Charset", "UTF-8");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(1, headers.getAcceptCharset().size());
		assertEquals("UTF-8", headers.getAcceptCharset().get(0).displayName());
	}
	
	@Test
	public void validateAcceptCharsetHeaderSingleCharset(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept-Charset", Charset.forName("UTF-8"));
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(1, headers.getAcceptCharset().size());
		assertEquals("UTF-8", headers.getAcceptCharset().get(0).displayName());
	}
	@Test
	@Ignore // Since we allow coma delimited strings for ACCEPT, we may allow it here
	public void validateAcceptCharsetHeaderMultipleAsDelimitedString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept-Charset", "UTF-8, ISO-8859-1");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAcceptCharset().size());
		// validate contains since the order is not enforsed
		assertTrue(headers.getAcceptCharset().contains(Charset.forName("UTF-8"))); 
		assertTrue(headers.getAcceptCharset().contains(Charset.forName("ISO-8859-1"))); 
	}
	
	@Test
	@Ignore // we may want to support String[] as well
	public void validateAcceptCharsetHeaderMultipleAsStringArray(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept-Charset", new String[]{"UTF-8", "ISO-8859-1"});
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAcceptCharset().size());
		// validate contains since the order is not enforsed
		assertTrue(headers.getAcceptCharset().contains(Charset.forName("UTF-8"))); 
		assertTrue(headers.getAcceptCharset().contains(Charset.forName("ISO-8859-1"))); 
	}
	
	@Test
	public void validateAcceptCharsetHeaderMultipleAsCollectionOfStrings(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept-Charset", CollectionUtils.arrayToList(new String[]{"UTF-8", "ISO-8859-1"}));
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAcceptCharset().size());
		// validate contains since the order is not enforsed
		assertTrue(headers.getAcceptCharset().contains(Charset.forName("UTF-8"))); 
		assertTrue(headers.getAcceptCharset().contains(Charset.forName("ISO-8859-1"))); 
	}
	@Test
	public void validateAcceptCharsetHeaderMultipleAsCollectionOfCharsets(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Accept-Charset", 
				CollectionUtils.arrayToList(new Charset[]{Charset.forName("UTF-8"), Charset.forName("ISO-8859-1")}));
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(2, headers.getAcceptCharset().size());
		// validate contains since the order is not enforsed
		assertTrue(headers.getAcceptCharset().contains(Charset.forName("UTF-8"))); 
		assertTrue(headers.getAcceptCharset().contains(Charset.forName("ISO-8859-1"))); 
	}
	
	// Cache-Control tests
	
	@Test
	public void validateCacheControl(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Cache-Control", "foo");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals("foo", headers.getCacheControl());
	}
	
	// Content-Length tests
	
	@Test
	public void validateContentLengthAsString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Content-Length", "1");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(1, headers.getContentLength());
	}
	
	@Test
	public void validateContentLengthAsNumber(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Content-Length", 1);
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals(1, headers.getContentLength());
	}
	
	@Test(expected=NumberFormatException.class)
	public void validateContentLengthAsNonNumericString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Content-Length", "foo");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	}
	
	// Content-Type test
	
	@Test(expected=IllegalArgumentException.class)
	public void validateContentTypeWrongValue(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Content-Type", "foo");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	}
	
	@Test
	public void validateContentTypeAsString(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Content-Type", "text/html");
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals("text", headers.getContentType().getType());
		assertEquals("html", headers.getContentType().getSubtype());
	}
	
	@Test
	public void validateContentTypeAsMediaType(){
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Content-Type", new MediaType("text", "html"));
		HttpHeaders headers = new HttpHeaders();
		
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals("text", headers.getContentType().getType());
		assertEquals("html", headers.getContentType().getSubtype());
	}
	
	// Date test
	
	@Test
	public void validateDateAsNumber() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Date", 12345678);
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	
		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getDate());
	}
	
	@Test
	public void validateDateAsString() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Date", "12345678");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	
		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getDate());
	}
	@Test
	public void validateDateAsDate() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Date", new Date(12345678));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	
		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getDate());
	}
	
	// If-Modified-Since tests
	
	@Test
	public void validateIfModifiedSinceAsNumber() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("If-Modified-Since", 12345678);
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	
		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getIfNotModifiedSince());
	}
	
	@Test
	public void validateIfModifiedSinceAsString() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("If-Modified-Since", "12345678");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	
		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getIfNotModifiedSince());
	}
	@Test
	public void validateIfModifiedSinceAsDate() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("If-Modified-Since", new Date(12345678));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
	
		assertEquals(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime(), headers.getIfNotModifiedSince());
	}
	
	// If-None-Match
	
	@Test
	public void validateIfNoneMatch() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("If-None-Match", "1234567");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		assertEquals(1, headers.getIfNoneMatch().size());
		assertEquals("1234567", headers.getIfNoneMatch().get(0));
	}
	
	@Test
	public void validateIfNoneMatchAsDelimitedString() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("If-None-Match", "1234567, 123");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		assertEquals(2, headers.getIfNoneMatch().size());
		assertEquals("1234567", headers.getIfNoneMatch().get(0));
		assertEquals("123", headers.getIfNoneMatch().get(1));
	}
	
	@Test
	@Ignore // we may want to support it or throw exception
	public void validateIfNoneMatchAsStringArray() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("If-None-Match", new String[]{"1234567", "123"});
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		assertEquals(2, headers.getIfNoneMatch().size());
		assertEquals("1234567", headers.getIfNoneMatch().get(0));
		assertEquals("123", headers.getIfNoneMatch().get(1));
	}
	
	@Test
	public void validateIfNoneMatchAsStringCollection() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("If-None-Match", CollectionUtils.arrayToList(new String[]{"1234567", "123"}));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	
		assertEquals(2, headers.getIfNoneMatch().size());
		assertEquals("1234567", headers.getIfNoneMatch().get(0));
		assertEquals("123", headers.getIfNoneMatch().get(1));
	}
	
	// Pragma tests
	@Test
	public void validatePragma() throws ParseException{
		HeaderMapper<HttpHeaders> mapper  = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<String, Object>();
		messageHeaders.put("Pragma", "foo");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertEquals("foo", headers.getPragma());
	}
}
