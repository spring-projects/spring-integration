/*
 *  Copyright 2002-2009 the original author or authors.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.integration.ws.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.ws.MarshallingWebServiceInboundGateway;
import org.springframework.integration.ws.SimpleWebServiceInboundGateway;
import org.springframework.oxm.AbstractMarshaller;
import org.springframework.oxm.Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class WebServiceInboundGatewayParserTests {

	@Autowired
	@Qualifier("requests")
	MessageChannel requestChannel;

	@Test
	public void configOk() throws Exception {
		// config valid
	}

	//Simple
	@Autowired
	@Qualifier("simple")
	SimpleWebServiceInboundGateway simpleGateway;

	@Test
	public void simpleGatewayProperties() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(simpleGateway);
		assertThat(
				(MessageChannel) accessor.getPropertyValue("requestChannel"),
				is(requestChannel));
	}

	//extractPayload = false
	@Autowired
	@Qualifier("extractsPayload")
	SimpleWebServiceInboundGateway payloadExtractingGateway;

	@Test
	public void extractPayloadSet() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				payloadExtractingGateway);
		assertThat((Boolean) accessor.getPropertyValue("extractPayload"),
				is(false));
	}

	//marshalling
	@Autowired
	MarshallingWebServiceInboundGateway marshallingGateway;
	@Autowired
	AbstractMarshaller marshaller;

	@Test
	public void marshallersSet() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(marshallingGateway);
		assertThat((AbstractMarshaller) accessor.getPropertyValue("marshaller"),
				is(marshaller));
		assertThat((AbstractMarshaller) accessor.getPropertyValue("unmarshaller"),
				is(marshaller));
	}
}
