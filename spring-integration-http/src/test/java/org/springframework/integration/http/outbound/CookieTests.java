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

package org.springframework.integration.http.outbound;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class CookieTests {

	@Autowired
	private MessageChannel ch1;

	@Autowired
	private QueueChannel ch6;

	private static final ByteArrayOutputStream bos = new ByteArrayOutputStream();

	private static final List<HttpHeaders> allHeaders = new ArrayList<>();

	@Test
	public void testCookie() throws Exception {
		ch1.send(new GenericMessage<>("Hello, world!"));
		assertThat(ch6.receive()).isNotNull();

		bos.close();
		BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bos.toByteArray())));
		String line = br.readLine();
		assertThat(line).isEqualTo("Hello, world!Hello, again!Hello, once more!");
		assertThat(br.readLine()).isNull();
		br.close();
		assertThat(allHeaders.size()).isEqualTo(3);
		assertThat(allHeaders.get(0).containsKey("Cookie")).isFalse();
		assertThat(allHeaders.get(1).containsKey("Cookie")).isTrue();
		assertThat(allHeaders.get(1).get("Cookie").get(0)).isEqualTo("JSESSIONID=X123");
		assertThat(allHeaders.get(2).containsKey("Cookie")).isTrue();
		assertThat(allHeaders.get(2).get("Cookie").get(0)).isEqualTo("JSESSIONID=X124");
	}

	public static class RequestFactory implements ClientHttpRequestFactory {

		private int count = 123;

		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
			return new ClientHttpRequest() {

				private HttpHeaders headers = new HttpHeaders();

				public HttpHeaders getHeaders() {
					return headers;
				}

				public OutputStream getBody() {
					return bos;
				}

				public URI getURI() {
					return null;
				}

				public HttpMethod getMethod() {
					return null;
				}

				public ClientHttpResponse execute() {
					allHeaders.add(headers);
					MockClientHttpResponse clientHttpResponse =
							new MockClientHttpResponse(new ByteArrayInputStream("OK".getBytes()), HttpStatus.OK);
					HttpHeaders httpHeaders = clientHttpResponse.getHeaders();
					httpHeaders.set("Set-cookie", "JSESSIONID=X" + count++); // test case insensitivity
					httpHeaders.set("Content-Length", "2");
					httpHeaders.set("Content-Type", "text/plain");
					return clientHttpResponse;
				}

			};

		}

	}

}
