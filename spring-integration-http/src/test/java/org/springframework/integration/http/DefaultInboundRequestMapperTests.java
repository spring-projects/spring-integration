/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;

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
	public void multipartUpload() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<String, MultipartFile>();
		MultipartFile file = new StubMultipartFile("file", "testFile.txt", "foo");
		files.add("file", file);
		Map<String, String[]> params = new HashMap<String, String[]>();
		MultipartHttpServletRequest multipartRequest = new DefaultMultipartHttpServletRequest(request, files, params);
		mapper.setCopyUploadedFiles(true);
		Message<?> result = mapper.toMessage(multipartRequest);
		File tmpFile = (File) ((Map) result.getPayload()).get("file");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(new FileInputStream(tmpFile), baos);
		assertThat(baos.toString(), is("foo"));
		tmpFile.deleteOnExit();
	}

	@Test
	public void testProcessMessageWithDollar() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("text");
		request.setCharacterEncoding("utf-8");
		byte[] bytes = SIMPLE_STRING.getBytes("utf-8");
		request.setContent(bytes);
		Message<String> message = (Message<String>) mapper.toMessage(request);
		ExpressionEvaluatingMessageProcessor processor = new ExpressionEvaluatingMessageProcessor("headers['$http_requestUrl']");
		assertEquals(message.getHeaders().get(HttpHeaders.REQUEST_URL), processor.processMessage(message));
	}

}
