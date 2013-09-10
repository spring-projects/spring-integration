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
package org.springframework.integration.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MultipleEndpointGatewayTests {
	
	@Autowired
	@Qualifier("gatewayA")
	private SampleGateway gatewayA;
	
	@Autowired
	@Qualifier("gatewayB")  
	private SampleGateway gatewayB;
	
	@Test
	public void gatewayNoDefaultReplyChannel(){
		gatewayA.echo("echoAsMessageChannel");
		// there is nothing to assert. Successful execution of the above is all we care in this test
	}
	@Test
	public void gatewayWithDefaultReplyChannel(){
		gatewayB.echo("echoAsMessageChannelIgnoreDefOutChannel");
		// there is nothing to assert. Successful execution of the above is all we care in this test
	}
	
	@Test
	public void gatewayWithReplySentBackToDefaultReplyChannel(){
		gatewayB.echo("echoAsMessageChannelDefaultOutputChannel");
		// there is nothing to assert. Successful execution of the above is all we care in this test
	}
	
	public static interface SampleGateway{
		public Object echo(Object value);
	}
	
	public static class SampleEchoService {
		public Object echo(Object value){
			return "R:" + value;
		}
		public Message<?> echoAsMessage(Object value){
			return MessageBuilder.withPayload("R:" + value).build();
		}
	}
}
