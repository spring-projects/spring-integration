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

package org.springframework.integration.ftp;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.ftp.outbound.FtpSendingMessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 */
public class FtpParserOutboundTests {
	
	@Test
	public void testFtpOutboundWithFileGenerator() throws Exception{
		ClassPathXmlApplicationContext context =  
				new ClassPathXmlApplicationContext("FtpParserOutboundTests-context.xml", this.getClass());
		FileNameGenerator fileNameGenerator = context.getBean("fileNameGenerator", FileNameGenerator.class);
		assertNotNull(fileNameGenerator);
		when(fileNameGenerator.generateFileName(Mockito.any(Message.class))).thenReturn("oleg-ftp-test.txt");
		EventDrivenConsumer fileOutboundEndpoint = context.getBean("ftpOutboundAdapter", EventDrivenConsumer.class);
		FtpSendingMessageHandler handler = (FtpSendingMessageHandler) TestUtils.getPropertyValue(fileOutboundEndpoint, "handler");
		Message<String> message = new GenericMessage<String>("ftp file generator test");
		try {
			handler.handleMessage(message);
		}
		catch (Exception e) {
			// ignore
		}
		verify(fileNameGenerator, times(1)).generateFileName(message);
	}

}
