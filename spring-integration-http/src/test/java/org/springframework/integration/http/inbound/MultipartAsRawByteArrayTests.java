/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.http.inbound;

import java.util.Collections;
import java.util.Enumeration;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class MultipartAsRawByteArrayTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testMultiPass() throws Exception {
		HttpRequestHandlingMessagingGateway gw = new HttpRequestHandlingMessagingGateway(false);
		gw.setMessageConverters(Collections.singletonList(new ByteArrayHttpMessageConverter()));
		gw.setMergeWithDefaultConverters(false);
		QueueChannel requestChannel = new QueueChannel();
		gw.setRequestChannel(requestChannel);
		gw.setBeanFactory(mock(BeanFactory.class));
		gw.setRequestPayloadTypeClass(byte[].class);
		gw.afterPropertiesSet();
		gw.start();

		String testData = "test data";

		HttpServletRequest request = mock(HttpServletRequest.class);
		ServletInputStream sis = mock(ServletInputStream.class);
		doReturn(testData.getBytes()).when(sis).readNBytes(anyInt());
		when(request.getInputStream()).thenReturn(sis);
		when(request.getMethod()).thenReturn("POST");
		when(request.getHeaderNames()).thenReturn(mock(Enumeration.class));
		when(request.getContentType()).thenReturn("multipart/form-data");
		when(request.getRequestURL()).thenReturn(new StringBuffer("foo"));
		RequestContextHolder.setRequestAttributes(mock(RequestAttributes.class));
		gw.handleRequest(request, mock(HttpServletResponse.class));
		Message<?> received = requestChannel.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isInstanceOf(byte[].class);
		assertThat(new String((byte[]) received.getPayload())).isEqualTo(testData);
	}

}
