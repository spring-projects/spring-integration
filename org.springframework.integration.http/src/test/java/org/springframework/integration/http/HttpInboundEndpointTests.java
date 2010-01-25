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

package org.springframework.integration.http;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.reset;
import static org.easymock.classextension.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.IAnswer;
import org.easymock.classextension.ConstructorArgs;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.StringMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.View;

/**
 * @author Alex Peters
 */
public class HttpInboundEndpointTests {

	private static final String ANY_ENCODING = "UTF-8";

	private static final String ANY_STRING_PAYLOAD = "any text content..blabla...bla.äöüßßß€€€€";

	private static final byte[] ANY_BINARY_PAYLOAD;

	private final MessageChannel requestChannel = createMock(MessageChannel.class);

	private final MessageChannel replyChannel = createMock(MessageChannel.class);

	private final Object[] allMocks = new Object[] { requestChannel, replyChannel };

	private HttpInboundEndpoint endpoint;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


    static {
		try {
			ANY_BINARY_PAYLOAD = "any binary content..blabla...bla.äöüßßß€€€€".getBytes(ANY_ENCODING);
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}


	@Before
	public void initializeSample() {
		endpoint = new HttpInboundEndpoint();
		endpoint.setRequestChannel(requestChannel);
		endpoint.setReplyChannel(replyChannel);
		endpoint.afterPropertiesSet();
		reset(allMocks);
		request = new MockHttpServletRequest("GET", "/anyurl");
		response = new MockHttpServletResponse();
		response.setCharacterEncoding(ANY_ENCODING);
    }


	@Test
	public void handleRequest_withDefaultSettingsAndUnsupportedHTTPMethods_returns405()
			throws ServletException, IOException {
		String[] httpMethods = { "OPTIONS", "HEAD", "PUT", "DELETE", "TRACE", "CONNECT", "ANY_INVALID" };
		for (String deniedHttpMethod : httpMethods) {
			request = new MockHttpServletRequest(deniedHttpMethod, "/anyurl");
			endpoint.handleRequest(request, response);
			assertThat("Unexpected result for http method: " + deniedHttpMethod,
					response.getStatus(),
					is(HttpServletResponse.SC_METHOD_NOT_ALLOWED));
		}
	}

	@Test
	public void handleRequest_withGETRequest_allReqParametersInMessagePayload()
			throws ServletException, IOException {
		final Map<String, String> sourceParams = addAnyParametersToRequest();
		expect(requestChannel.send(isA(Message.class))).andAnswer(
			new IAnswer<Boolean>() {
				@SuppressWarnings("unchecked")
				public Boolean answer() throws Throwable {
					final Map<?, ?> msgParams = (Map<?, ?>) ((Message) getCurrentArguments()[0]).getPayload();
					assertThat(msgParams.size(), is(sourceParams.size()));
					for (String key : sourceParams.keySet()) {
						assertThat((List<String>) msgParams.get(key),
								is(Collections.singletonList(sourceParams.get(key))));
					}
					return true;
				}
			});
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
	}

	@Test
	public void handleRequest_withEmptyGETRequest_emptyMapIsInMessagePayload()
			throws ServletException, IOException {
		expect(requestChannel.send(isA(Message.class))).andAnswer(
			new IAnswer<Boolean>() {
				@SuppressWarnings("unchecked")
				public Boolean answer() throws Throwable {
					final Map<?, ?> msgParams = (Map<?, ?>) ((Message) getCurrentArguments()[0]).getPayload();
					assertThat(msgParams.size(), is(0));
					return true;
				}
			});
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
	}

	@Test
	public void handleRequest_withCustomRequestMapper_requestObjectIsInPayload()
			throws ServletException, IOException {
		endpoint.setRequestMapper(new InboundRequestMapper() {
			public Message<?> toMessage(HttpServletRequest request) throws Exception {
				return new StringMessage(request.getRequestURI());
			}
		});
		expect(requestChannel.send(isA(Message.class))).andAnswer(
			new IAnswer<Boolean>() {
				@SuppressWarnings("unchecked")
				public Boolean answer() throws Throwable {
					assertThat(((Message) getCurrentArguments()[0]).getPayload(), is((Object) "/anyurl"));
					return true;
				}
			});
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
	}

	@Test
	public void handleRequest_RequestHeadersInMsgHeaders()
			throws ServletException, IOException {
		final Principal anyPrincipal = createMock(Principal.class);
		request.setUserPrincipal(anyPrincipal);
		expect(requestChannel.send(isA(Message.class))).andAnswer(
			new IAnswer<Boolean>() {
				@SuppressWarnings("unchecked")
				public Boolean answer() throws Throwable {
					MessageHeaders headers = ((Message) getCurrentArguments()[0]).getHeaders();
					assertThat(headers.get(HttpHeaders.REQUEST_METHOD),
							is((Object) "GET"));
					assertThat(headers.get(HttpHeaders.REQUEST_URL),
							is((Object) "http://localhost:80/anyurl"));
					assertThat(headers.get(HttpHeaders.USER_PRINCIPAL),
							is((Object) anyPrincipal));
					return true;
				}
			});
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
	}

	@Test
	public void handleRequest_withPOSTRequestAndTextContent_sameInMessagePayload()
			throws ServletException, IOException {
		final String characterEncoding = ANY_ENCODING;
		addRequestContent("POST", "text/plain", characterEncoding,
				ANY_STRING_PAYLOAD.getBytes(characterEncoding));
		expect(requestChannel.send(isA(Message.class))).andAnswer(
			new IAnswer<Boolean>() {
				@SuppressWarnings("unchecked")
				public Boolean answer() throws Throwable {
					assertThat((String) ((Message) getCurrentArguments()[0]).getPayload(),
							is(ANY_STRING_PAYLOAD));
					return true;
				}
			});
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
	}

	@Test
	public void handleRequest_withPOSTRequestAndFormContent_sameInMessagePayload()
			throws ServletException, IOException {
		addRequestContent("POST", "application/x-www-form-urlencoded", ANY_ENCODING, new byte[0]);
		final Map<String, String> sourceParams = addAnyParametersToRequest();
		request.setParameters(sourceParams);
		expect(requestChannel.send(isA(Message.class))).andAnswer(
			new IAnswer<Boolean>() {
				@SuppressWarnings("unchecked")
				public Boolean answer() throws Throwable {
					MultiValueMap<String, String> payloadMap = (MultiValueMap<String, String>)
							((Message) getCurrentArguments()[0]).getPayload();
					for (String key : sourceParams.keySet()) {
						assertThat(payloadMap.get(key),
								is(Collections.singletonList(sourceParams.get(key))));
					}
					return true;
				}
			});
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
	}

	@Test
	public void handleRequest_withEmptyPOSTRequest_emptyStringAsPayload()
			throws ServletException, IOException {
		addRequestContent("POST", "text/plain", null, new byte[0]);
		expect(requestChannel.send(isA(Message.class))).andAnswer(
			new IAnswer<Boolean>() {
				@SuppressWarnings("unchecked")
				public Boolean answer() throws Throwable {
					assertThat((String) ((Message) getCurrentArguments()[0]).getPayload(), is(""));
					return true;
				}
			});
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
	}

	@Test
	public void handleRequest_withPOSTRequestAndBinaryContent_sameInMessagePayload()
			throws ServletException, IOException {
		addRequestContent("POST", "", ANY_ENCODING, ANY_BINARY_PAYLOAD);
		expect(requestChannel.send(isA(Message.class))).andAnswer(
			new IAnswer<Boolean>() {
				@SuppressWarnings("unchecked")
				public Boolean answer() throws Throwable {
					assertThat((byte[]) ((Message) getCurrentArguments()[0]).getPayload(),
							is(ANY_BINARY_PAYLOAD));
					return true;
				}
			});
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
	}

	@Test
	public void handleRequest_withPOSTRequestEmptyContentLenght_return411()
			throws ServletException, IOException {
		addRequestContent("POST", "", ANY_ENCODING, null);
		endpoint.handleRequest(request, response);
		assertThat(response.getStatus(), is(HttpServletResponse.SC_LENGTH_REQUIRED));
    }

	@Test
	public void handleRequest_withoutReplyMessage_return200()
			throws ServletException, IOException {
		expect(requestChannel.send(isA(Message.class))).andReturn(true);
		replay(allMocks);
		endpoint.handleRequest(request, response);
		assertThat(response.getStatus(), is(HttpServletResponse.SC_OK));
	}

	@Test
	public void handleRequest_replyWithTextPayload_textAsRespContent()
			throws ServletException, IOException {
		setupEndpointAsMock(ANY_STRING_PAYLOAD);
		replay(allMocks);
		endpoint.handleRequest(request, response);
		assertThat(response.getContentAsString(), is(ANY_STRING_PAYLOAD));
		verify(allMocks);
	}

	@Test
	public void handleRequest_replyWithBytePayload_bytesAsRespContent()
			throws ServletException, IOException {
		setupEndpointAsMock(ANY_BINARY_PAYLOAD);
		replay(allMocks);
		endpoint.handleRequest(request, response);
		assertThat(response.getContentAsByteArray(), is(ANY_BINARY_PAYLOAD));
		verify(allMocks);
	}

	@Test
	public void handleRequest_replyWithSerializablePayload_serializableAsRespContent()
			throws ServletException, IOException, ClassNotFoundException {
		Date obj = new Date();
		setupEndpointAsMock(obj);
		replay(allMocks);
		endpoint.handleRequest(request, response);
		byte[] content = response.getContentAsByteArray();
		Object deserializedObj = new ObjectInputStream(
				new ByteArrayInputStream(content)).readObject();
		assertThat(deserializedObj, is(Date.class));
		assertThat((Date) deserializedObj, is(obj));
		verify(allMocks);
	}

	@Test(expected = ServletException.class)
	public void handleRequest_replyWithNonSerializablePayload_exceptionThrown()
			throws ServletException, IOException, ClassNotFoundException {
		Object obj = new Object();
		setupEndpointAsMock(obj);
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void handleRequest_expectReplyWithView_responseDirectedToView() throws Exception {
		setupEndpointAsMock(ANY_STRING_PAYLOAD);
		View view = createMock(View.class);
		view.render(isA(Map.class), eq(request), eq(response));
		expectLastCall().andAnswer(new IAnswer<Object>() {
			public Object answer() throws Throwable {
				Map<?, ?> model = (Map<?, ?>) getCurrentArguments()[0];
				assertThat(model.isEmpty(), is(false));
				assertThat(model.get("reply"), is((Object) ANY_STRING_PAYLOAD));
				assertThat(model.get("requestMessage"), is(notNullValue()));
				return null;
			}
		});
		endpoint.setView(view);
		replay(view);
		replay(allMocks);
		endpoint.handleRequest(request, response);
		verify(allMocks);
		verify(view);
	}


	/** add some dummy parameters to the request */
	private Map<String, String> addAnyParametersToRequest() {
		final Map<String, String> sourceParams = new HashMap<String, String>();
		for (int i = 0; i < 20; i++) {
			sourceParams.put("anyParameter" + i, "anyValue" + i);
		}
		request.setParameters(sourceParams);
		return sourceParams;
	}

	/** set given params on request instance */
	private void addRequestContent(String httpMethod, String contentType, String encoding, byte[] content) {
		request.setMethod(httpMethod);
		request.setContentType(contentType);
		request.setContent(content);
		request.setCharacterEncoding(encoding);
	}

	/** create a new mocked endpoint instance and setup basic data */
	private void setupEndpointAsMock(final Object anyPayload) {
		try {
			endpoint = createMock(HttpInboundEndpoint.class,
					new ConstructorArgs(
							HttpInboundEndpoint.class.getConstructor(new Class[0]),
							new Object[0]),
					HttpInboundEndpoint.class.getMethod("sendAndReceive", Object.class));
			endpoint.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		expect(endpoint.sendAndReceive(anyObject())).andReturn(anyPayload);
		endpoint.setExpectReply(true);
		replay(endpoint);
	}

}
