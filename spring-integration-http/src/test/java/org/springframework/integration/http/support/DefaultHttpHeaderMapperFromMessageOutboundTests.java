/*
 * Copyright 2002-2022 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0.1
 */
public class DefaultHttpHeaderMapperFromMessageOutboundTests {

	// ACCEPT tests
	@Test(expected = IllegalArgumentException.class)
	public void validateAcceptHeaderWithNoSlash() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept", "bar");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	}

	@Test
	public void validateAcceptHeaderSingleString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept", "bar/foo");

		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAccept().get(0).getType()).isEqualTo("bar");
		assertThat(headers.getAccept().get(0).getSubtype()).isEqualTo("foo");
	}

	@Test
	public void validateAcceptHeaderSingleMediaType() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept", new MediaType("bar", "foo"));

		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAccept().get(0).getType()).isEqualTo("bar");
		assertThat(headers.getAccept().get(0).getSubtype()).isEqualTo("foo");
	}

	@Test
	public void validateAcceptHeaderMultipleAsDelimitedString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept", "bar/foo, text/xml");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAccept()).hasSize(2);
		assertThat(headers.getAccept().get(0).toString()).isEqualTo("bar/foo");
		assertThat(headers.getAccept().get(1).toString()).isEqualTo("text/xml");
	}

	@Test
	public void validateAcceptHeaderMultipleAsStringArray() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept", new String[] {"bar/foo", "text/xml"});
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAccept()).hasSize(2);
		assertThat(headers.getAccept().get(0).toString()).isEqualTo("bar/foo");
		assertThat(headers.getAccept().get(1).toString()).isEqualTo("text/xml");
	}

	@Test
	public void validateAcceptHeaderMultipleAsStringCollection() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept", Arrays.asList("bar/foo", "text/xml"));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAccept()).hasSize(2);
		assertThat(headers.getAccept().get(0).toString()).isEqualTo("bar/foo");
		assertThat(headers.getAccept().get(1).toString()).isEqualTo("text/xml");
	}

	@Test
	public void validateAcceptHeaderMultipleAsStringCollectionCaseInsensitive() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("acCePt", Arrays.asList("bar/foo", "text/xml"));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAccept()).hasSize(2);
		assertThat(headers.getAccept().get(0).toString()).isEqualTo("bar/foo");
		assertThat(headers.getAccept().get(1).toString()).isEqualTo("text/xml");
	}

	@Test
	public void validateAcceptHeaderMultipleAsMediaTypeCollection() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept", Arrays.asList(new MediaType("bar", "foo"), new MediaType("text", "xml")));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAccept()).hasSize(2);
		assertThat(headers.getAccept().get(0).toString()).isEqualTo("bar/foo");
		assertThat(headers.getAccept().get(1).toString()).isEqualTo("text/xml");
	}

	// ACCEPT_CHARSET tests

	@Test(expected = UnsupportedCharsetException.class)
	public void validateAcceptCharsetHeaderWithWrongCharset() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept-Charset", "foo");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	}

	@Test
	public void validateAcceptCharsetHeaderSingleString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept-Charset", "UTF-8");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAcceptCharset()).hasSize(1);
		assertThat(headers.getAcceptCharset().get(0).displayName()).isEqualTo("UTF-8");
	}

	@Test
	public void validateAcceptCharsetHeaderSingleCharset() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept-Charset", Charset.forName("UTF-8"));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAcceptCharset()).hasSize(1);
		assertThat(headers.getAcceptCharset().get(0).displayName()).isEqualTo("UTF-8");
	}

	@Test
	public void validateAcceptCharsetHeaderMultipleAsDelimitedString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept-Charset", "UTF-8, ISO-8859-1");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAcceptCharset()).hasSize(2);
		// validate contains since the order is not enforced
		assertThat(headers.getAcceptCharset().contains(Charset.forName("UTF-8"))).isTrue();
		assertThat(headers.getAcceptCharset().contains(Charset.forName("ISO-8859-1"))).isTrue();
	}

	@Test
	public void validateAcceptCharsetHeaderMultipleAsStringArray() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept-Charset", new String[] {"UTF-8", "ISO-8859-1"});
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAcceptCharset()).hasSize(2);
		// validate contains since the order is not enforced
		assertThat(headers.getAcceptCharset().contains(Charset.forName("UTF-8"))).isTrue();
		assertThat(headers.getAcceptCharset().contains(Charset.forName("ISO-8859-1"))).isTrue();
	}

	@Test
	public void validateAcceptCharsetHeaderMultipleAsCharsetArray() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept-Charset", new Charset[] {Charset.forName("UTF-8"), Charset.forName("ISO-8859-1")});
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAcceptCharset()).hasSize(2);
		// validate contains since the order is not enforced
		assertThat(headers.getAcceptCharset().contains(Charset.forName("UTF-8"))).isTrue();
		assertThat(headers.getAcceptCharset().contains(Charset.forName("ISO-8859-1"))).isTrue();
	}

	@Test
	public void validateAcceptCharsetHeaderMultipleAsCollectionOfStrings() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept-Charset", Arrays.asList("UTF-8", "ISO-8859-1"));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAcceptCharset()).hasSize(2);
		// validate contains since the order is not enforced
		assertThat(headers.getAcceptCharset().contains(Charset.forName("UTF-8"))).isTrue();
		assertThat(headers.getAcceptCharset().contains(Charset.forName("ISO-8859-1"))).isTrue();
	}

	@Test
	public void validateAcceptCharsetHeaderMultipleAsCollectionOfCharsets() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Accept-Charset", Arrays.asList(Charset.forName("UTF-8"), Charset.forName("ISO-8859-1")));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getAcceptCharset()).hasSize(2);
		// validate contains since the order is not enforced
		assertThat(headers.getAcceptCharset().contains(Charset.forName("UTF-8"))).isTrue();
		assertThat(headers.getAcceptCharset().contains(Charset.forName("ISO-8859-1"))).isTrue();
	}

	// Cache-Control tests

	@Test
	public void validateCacheControl() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Cache-Control", "foo");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getCacheControl()).isEqualTo("foo");
	}

	// Content-Length tests

	@Test
	public void validateContentLengthAsString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Content-Length", "1");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getContentLength()).isEqualTo(1);
	}

	@Test
	public void validateContentLengthAsNumber() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Content-Length", 1);
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getContentLength()).isEqualTo(1);
	}

	@Test(expected = NumberFormatException.class)
	public void validateContentLengthAsNonNumericString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Content-Length", "foo");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	}

	// Content-Type test

	@Test(expected = IllegalArgumentException.class)
	public void validateContentTypeWrongValue() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(MessageHeaders.CONTENT_TYPE, "foo");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
	}

	@Test
	public void validateContentTypeAsString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Content-Type", "text/html");
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getContentType().getType()).isEqualTo("text");
		assertThat(headers.getContentType().getSubtype()).isEqualTo("html");
	}

	@Test
	public void validateContentTypeAsMediaType() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(MessageHeaders.CONTENT_TYPE, new MediaType("text", "html"));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getContentType().getType()).isEqualTo("text");
		assertThat(headers.getContentType().getSubtype()).isEqualTo("html");
	}

	@Test
	public void validateContentTypeAsMimeType() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(MessageHeaders.CONTENT_TYPE, new MimeType("text", "plain"));
		HttpHeaders headers = new HttpHeaders();

		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getContentType().getType()).isEqualTo("text");
		assertThat(headers.getContentType().getSubtype()).isEqualTo("plain");
	}

	// Date test

	@Test
	public void validateDateAsNumber() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Date", 12345678);
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getDate()).isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	@Test
	public void validateDateAsString() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Date", "12345678");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getDate()).isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	@Test
	public void validateDateAsDate() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Date", new Date(12345678));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getDate()).isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	// If-Modified-Since tests

	@Test
	public void validateIfModifiedSinceAsNumber() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("If-Modified-Since", 12345678);
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getIfModifiedSince())
				.isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	@Test
	public void validateIfModifiedSinceAsString() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("If-Modified-Since", "12345678");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getIfModifiedSince())
				.isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	@Test
	public void validateIfModifiedSinceAsDate() throws ParseException {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("If-Modified-Since", new Date(12345678));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

		assertThat(headers.getIfModifiedSince())
				.isEqualTo(simpleDateFormat.parse("Thu, 01 Jan 1970 03:25:45 GMT").getTime());
	}

	// If-None-Match

	@Test
	public void validateIfNoneMatch() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("If-None-Match", "\"1234567\"");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		assertThat(headers.getIfNoneMatch()).hasSize(1);
		assertThat(headers.getIfNoneMatch().get(0)).isEqualTo("\"1234567\"");
	}

	@Test
	public void validateIfNoneMatchAsDelimitedString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("If-None-Match", "\"123,4567\", \"123\"");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		assertThat(headers.getIfNoneMatch()).hasSize(2);
		assertThat(headers.getIfNoneMatch().get(0)).isEqualTo("\"123,4567\"");
		assertThat(headers.getIfNoneMatch().get(1)).isEqualTo("\"123\"");
	}

	@Test
	public void validateIfNoneMatchAsStringArray() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("If-None-Match", new String[] {"\"1234567\"", "\"123\""});
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		assertThat(headers.getIfNoneMatch()).hasSize(2);
		assertThat(headers.getIfNoneMatch().get(0)).isEqualTo("\"1234567\"");
		assertThat(headers.getIfNoneMatch().get(1)).isEqualTo("\"123\"");
	}

	@Test
	public void validateIfNoneMatchAsCommaDelimitedString() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("If-None-Match", "\"123.4567\", \"123\"");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		assertThat(headers.getIfNoneMatch()).hasSize(2);
		assertThat(headers.getIfNoneMatch().get(0)).isEqualTo("\"123.4567\"");
		assertThat(headers.getIfNoneMatch().get(1)).isEqualTo("\"123\"");
	}

	@Test
	public void validateIfNoneMatchAsStringCollection() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("If-None-Match", Arrays.asList("\"1234567\"", "\"123\""));
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);

		assertThat(headers.getIfNoneMatch()).hasSize(2);
		assertThat(headers.getIfNoneMatch().get(0)).isEqualTo("\"1234567\"");
		assertThat(headers.getIfNoneMatch().get(1)).isEqualTo("\"123\"");
	}

	// Pragma tests
	@Test
	public void validatePragma() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Pragma", "foo");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.getPragma()).isEqualTo("foo");
	}

	// Transfer-Encoding tests
	@Test
	public void validateTransferEncoding() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Transfer-Encoding", "chunked");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.get("Transfer-Encoding")).isNull();
	}

	@Test
	public void validateTransferEncodingToHeaders() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Transfer-Encoding", "chunked");

		Map<String, ?> messageHeaders = mapper.toHeaders(httpHeaders);
		assertThat(messageHeaders).hasSize(1);

	}

	@Test
	@Ignore
	public void perf() {
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Transfer-Encoding", "chunked");
		httpHeaders.set("X-Transfer-Encoding1", "chunked");
		httpHeaders.set("X-Transfer-Encoding2", "chunked");
		httpHeaders.set("X-Transfer-Encoding3", "chunked");
		httpHeaders.set("X-Transfer-Encoding4", "chunked");
		httpHeaders.set("X-Transfer-Encoding5", "chunked");
		httpHeaders.set("X-Transfer-Encoding6", "chunked");
		httpHeaders.set("X-Transfer-Encoding7", "chunked");

		StopWatch watch = new StopWatch();
		watch.start();
		for (int i = 0; i < 100000; i++) {
			mapper.toHeaders(httpHeaders);
		}
		watch.stop();

	}

	// Custom headers

	@Test
	public void validateCustomHeaderWithNoHeaderNames() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("foo", "foo");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.get("foo")).isNull();
		assertThat(headers.get("X-foo")).isNull();
	}

	@Test
	public void validateCustomHeaderWithHeaderNames() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames("foo");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("foo", "foo");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.get("X-foo")).isNull();
		assertThat(headers.get("foo")).isNotNull();
		assertThat(headers.get("foo")).hasSize(1);
		assertThat(headers.get("foo").get(0)).isEqualTo("foo");
	}

	@Test
	public void validateCustomHeaderWithHeaderNamePatterns() {
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
		assertThat(headers).hasSize(3);
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
	public void validateCustomHeaderWithHeaderNamePatternsAndStandardRequestHeaders() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames("foo*", "HTTP_REQUEST_HEADERS");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("Content-Type", "text/html");
		messageHeaders.put("Accept", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers).hasSize(3);
		assertThat(headers.get("foobar")).hasSize(1);
		assertThat(headers.getFirst("foobar")).isEqualTo("abc");
		assertThat(headers.getContentType().toString()).isEqualTo("text/html");
		assertThat(headers.getAccept()).hasSize(1);
		assertThat(headers.getAccept().get(0)).isEqualTo(MediaType.TEXT_XML);
	}

	@Test
	public void validateCustomHeaderWithHeaderNamePatternsAndStandardResponseHeaders() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames("foo*", "HTTP_RESPONSE_HEADERS");
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("foobar", "abc");
		httpHeaders.setContentType(MediaType.TEXT_HTML);
		httpHeaders.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
		Map<String, ?> messageHeaders = mapper.toHeaders(httpHeaders);
		assertThat(messageHeaders).hasSize(2);
		assertThat(messageHeaders.get("Accept")).isNull();
		assertThat(messageHeaders.get("foobar")).isEqualTo("abc");
		assertThat(messageHeaders.get(MessageHeaders.CONTENT_TYPE).toString()).isEqualTo("text/html");
	}

	@Test
	public void validateCustomHeaderWithStandardPrefix() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames("X-Foo");
		HttpHeaders headers = new HttpHeaders();
		headers.set("x-foo", "x-foo-value");
		Map<String, ?> result = mapper.toHeaders(headers);
		assertThat(result).hasSize(1);
		assertThat(result.get("x-foo")).isEqualTo("x-foo-value");
	}

	@Test
	public void validateCustomHeaderWithStandardPrefixSameCase() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setInboundHeaderNames("X-Foo");
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Foo", "x-foo-value");
		Map<String, ?> result = mapper.toHeaders(headers);
		assertThat(result).hasSize(1);
		assertThat(result.get("X-Foo")).isEqualTo("x-foo-value");
	}

	@Test
	public void validateCustomHeaderCaseInsensitivity() {
		DefaultHttpHeaderMapper mapper = new DefaultHttpHeaderMapper();
		mapper.setOutboundHeaderNames("*", "HTTP_REQUEST_HEADERS");
		mapper.setUserDefinedHeaderPrefix("X-");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("foobar", "abc");
		messageHeaders.put("X-bar", "xbar");
		messageHeaders.put("x-baz", "xbaz");
		messageHeaders.put("Content-Type", "text/html");
		messageHeaders.put("Accept", "text/xml");
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.size()).isEqualTo(5);
		assertThat(headers.get("X-foobar")).hasSize(1);
		assertThat(headers.get("x-foobar")).hasSize(1);
		assertThat(headers.get("X-bar")).hasSize(1);
		assertThat(headers.get("x-bar")).hasSize(1);
		assertThat(headers.get("X-baz")).hasSize(1);
		assertThat(headers.get("x-baz")).hasSize(1);
	}

	@Test
	public void dontPropagateContentLength() {
		DefaultHttpHeaderMapper mapper = DefaultHttpHeaderMapper.outboundMapper();
		// not suppressed on outbound request, by default
		mapper.setExcludedOutboundStandardRequestHeaderNames("Content-Length");
		Map<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put("Content-Length", 4);

		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(new MessageHeaders(messageHeaders), headers);
		assertThat(headers.get("Content-Length")).isNull();
	}

	@Test
	public void testInt3063InvalidExpiresHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Expires", "-1");
		Map<String, Object> messageHeaders = DefaultHttpHeaderMapper.outboundMapper().toHeaders(headers);
		assertThat(messageHeaders.size()).isEqualTo(0);
	}

	@Test
	public void testInt2995IfModifiedSince() {
		Date ifModifiedSince = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		String value = dateFormat.format(ifModifiedSince);
		Message<?> testMessage = MessageBuilder.withPayload("foo").setHeader("If-Modified-Since", value).build();
		HeaderMapper<HttpHeaders> mapper = DefaultHttpHeaderMapper.outboundMapper();
		HttpHeaders headers = new HttpHeaders();
		mapper.fromHeaders(testMessage.getHeaders(), headers);
		Calendar c = Calendar.getInstance();
		c.setTime(ifModifiedSince);
		c.set(Calendar.MILLISECOND, 0);
		assertThat(headers.getIfModifiedSince()).isEqualTo(c.getTimeInMillis());
	}

	@Test
	public void testContentDispositionHeader() {
		HttpHeaders headers = new HttpHeaders();
		String headerValue = "attachment; filename=\"test.txt\"";
		headers.set("Content-Disposition", headerValue);
		Map<String, Object> messageHeaders = DefaultHttpHeaderMapper.outboundMapper().toHeaders(headers);
		assertThat(messageHeaders).hasSize(1);
		assertThat(messageHeaders.get("Content-Disposition")).isEqualTo(headerValue);
	}

}
