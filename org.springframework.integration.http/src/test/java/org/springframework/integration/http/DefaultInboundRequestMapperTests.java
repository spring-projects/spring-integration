package org.springframework.integration.http;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.mock.web.MockHttpServletRequest;

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
}
