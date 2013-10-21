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
package org.springframework.integration.http.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Gunnar Hillert
 *
 * @since 2.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class CookieTests {

	@Autowired
	private MessageChannel ch1;

	@Autowired
	private QueueChannel ch6;

	private static ByteArrayOutputStream bos = new ByteArrayOutputStream();

	private static List<HttpHeaders> allHeaders = new ArrayList<HttpHeaders>();

	@Test
	public void testCookie() throws Exception {
		ch1.send(new GenericMessage<String>("Hello, world!"));
		Assert.assertNotNull(ch6.receive());


		bos.close();
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bos.toByteArray())));
		String line = br.readLine();
		assertEquals("Hello, world!Hello, again!Hello, once more!", line);
		assertNull(br.readLine());
		br.close();
		assertEquals(3, allHeaders.size());
		assertFalse(allHeaders.get(0).containsKey("Cookie"));
		assertTrue(allHeaders.get(1).containsKey("Cookie"));
		assertEquals("JSESSIONID=X123", allHeaders.get(1).get("Cookie").get(0));
		assertTrue(allHeaders.get(2).containsKey("Cookie"));
		assertEquals("JSESSIONID=X124", allHeaders.get(2).get("Cookie").get(0));
	}

	public static class RequestFactory implements ClientHttpRequestFactory {

		private int count = 123;

		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod)
				throws IOException {
			return new ClientHttpRequest() {

				private HttpHeaders headers = new HttpHeaders();

				public HttpHeaders getHeaders() {
					return headers;
				}

				public OutputStream getBody() throws IOException {
					return bos;
				}

				public URI getURI() {
					return null;
				}

				public HttpMethod getMethod() {
					return null;
				}

				public ClientHttpResponse execute() throws IOException {
					allHeaders.add(headers);
					return new ClientHttpResponse() {

						public HttpHeaders getHeaders() {
							HttpHeaders headers = new HttpHeaders();
							headers.set("Set-cookie", "JSESSIONID=X" + count++); // test case insensitivity
							headers.set("Content-Length", "2");
							headers.set("Content-Type", "text/plain");
							return headers;
						}

						public InputStream getBody() throws IOException {
							return new ByteArrayInputStream("OK".getBytes());
						}

						public String getStatusText() throws IOException {
							return "OK";
						}

						public HttpStatus getStatusCode() throws IOException {
							return HttpStatus.OK;
						}

						public void close() {
						}

						public int getRawStatusCode() throws IOException {
							return 200;
						}
					};
				}

			};
		}

	}
}
