/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.http.inbound;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.2
 */
public class MultipartAsRawByteArrayTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testMultiPass() throws Exception {
		HttpRequestHandlingMessagingGateway gw = new HttpRequestHandlingMessagingGateway(false);
		gw.setMessageConverters(
				Collections.<HttpMessageConverter<?>>singletonList(new ByteArrayHttpMessageConverter()));
		gw.setMergeWithDefaultConverters(false);
		QueueChannel requestChannel = new QueueChannel();
		gw.setRequestChannel(requestChannel);
		gw.setBeanFactory(mock(BeanFactory.class));
		gw.setRequestPayloadType(byte[].class);
		gw.afterPropertiesSet();
		gw.start();

		HttpServletRequest request = mock(HttpServletRequest.class);
		ServletInputStream sis = mock(ServletInputStream.class);
		doAnswer(new Answer<Integer>() {

			int done;

			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				byte[] buff = (byte[]) invocation.getArguments()[0];
				buff[0] = 'f';
				buff[1] = 'o';
				buff[2] = 'o';
				return done++ > 0 ? -1 : 3;
			}

		}).when(sis).read(Matchers.any(byte[].class));
		when(request.getInputStream()).thenReturn(sis);
		when(request.getMethod()).thenReturn("POST");
		when(request.getHeaderNames()).thenReturn(mock(Enumeration.class));
		when(request.getContentType()).thenReturn("multipart/form-data");
		when(request.getRequestURL()).thenReturn(new StringBuffer("foo"));
		RequestContextHolder.setRequestAttributes(mock(RequestAttributes.class));
		gw.handleRequest(request, mock(HttpServletResponse.class));
		Message<?> received = requestChannel.receive(10000);
		assertNotNull(received);
		assertThat(received.getPayload(), instanceOf(byte[].class));
		assertEquals("foo", new String((byte[]) received.getPayload()));
	}

}
