/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.http;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */
@SuppressWarnings("unchecked")
public class DefaultInboundRequestMapperTests {

	private static final String SIMPLE_STRING = "just ascii";

	private static final String COMPLEX_STRING = "A\u00ea\u00f1\u00fcC";

	private DefaultInboundRequestMapper mapper = new DefaultInboundRequestMapper();

	@Test
	public void simpleUtf8TextMapping() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text");
		request.setCharacterEncoding("utf-8");
		byte[] bytes = SIMPLE_STRING.getBytes("utf-8");
		request.setContent(bytes);
		Message<String> message = (Message<String>) mapper.toMessage(request);
		assertThat(message.getPayload(), is(SIMPLE_STRING));
	}

	@Test
	public void complexUtf8TextMapping() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text");
		// don't forget to specify the character encoding on the request or you
		// will end up with unpredictable results!
		request.setCharacterEncoding("utf-8");
		byte[] bytes = COMPLEX_STRING.getBytes("utf-8");
		request.setContent(bytes);
		Message<String> message = (Message<String>) mapper.toMessage(request);
		assertThat(message.getPayload(), is(COMPLEX_STRING));
	}

	@Test
	public void newlineTest() throws Exception {
		String content = "foo\nbar\n";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text");
		byte[] bytes = content.getBytes();
		request.setContent(bytes);
		Message<String> message = (Message<String>) mapper.toMessage(request);
		assertThat(message.getPayload(), is(content));
	}

	@Test
	public void emptyStringTest() throws Exception {
		String content = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text");
		byte[] bytes = content.getBytes();
		request.setContent(bytes);
		Message<String> message = (Message<String>) mapper.toMessage(request);
		assertThat(message.getPayload(), is(content));
	}

	@Test
	public void largeStreamTest() throws Exception {
		final int size = 100000;
		final byte[] content = new byte[size];
		for (int i = 0; i < size; i++) {
			content[i] = 7;
		}
		HttpServletRequest request = createMock(HttpServletRequest.class);
		final InputStream inputStream = new InputStream() {
			private final AtomicInteger next = new AtomicInteger(0);
			public int read() throws IOException {
				if (next.get() >= size) {
					return -1;
				}
				int index = next.getAndIncrement();
				return content[index];
			}
		};
		expect(request.getInputStream()).andReturn(new ServletInputStream() {
			public int read(byte[] b) throws IOException {
				int length = b.length;
				if (length > 99990) {
					length = 99990;
				}
				byte[] temp = new byte[length];
				int numRead = inputStream.read(temp);
				for (int i = 0; i < numRead; i++) {
					b[i] = temp[i];
				}
				return numRead;
			}
			public int read(byte[] b, int off, int length) throws IOException {
				if (length > 99990) {
					length = 99990;
				}
				byte[] temp = new byte[length];
				int numRead = inputStream.read(temp, 0, length);
				for (int i = 0; i < numRead; i++) {
					b[off + i] = temp[i];
				}
				return numRead;
			}
			public int read() throws IOException {
				return inputStream.read();
			}
		});
		expect(request.getContentType()).andReturn(null);
		expect(request.getMethod()).andReturn("POST").anyTimes();
		expect(request.getContentLength()).andReturn(size);
		expect(request.getHeaderNames()).andReturn(null);
		expect(request.getRequestURL()).andReturn(new StringBuffer("http://test"));
		expect(request.getUserPrincipal()).andReturn(null);
		replay(request);
		Message<?> message = mapper.toMessage(request);
		byte[] payload = (byte[]) message.getPayload();
		int byteValueCounter = 0;
		for (byte b : payload) {
			if (b == 7) {
				byteValueCounter++;
			}
		}
		assertEquals(size, byteValueCounter);
	}

}
