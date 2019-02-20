/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.xml.transformer.jaxbmarshaling;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;

import org.junit.Test;
import org.w3c.dom.Document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.xml.transform.StringSource;

/**
 * @author Jonas Partner
 */
@ContextConfiguration
public class JaxbMarshallingIntegrationTests extends AbstractJUnit4SpringContextTests {

	@Autowired @Qualifier("marshallIn")
	MessageChannel marshallIn;

	@Autowired @Qualifier("marshallOut")
	PollableChannel marshalledOut;

	@Autowired @Qualifier("unmarshallIn")
	MessageChannel unmarshallIn;

	@Autowired @Qualifier("unmarshallOut")
	PollableChannel unmarshallOut;


	@SuppressWarnings("unchecked")
	@Test
	public void testMarshalling() throws Exception {
		JaxbAnnotatedPerson person = new JaxbAnnotatedPerson();
		person.setFirstName("john");
		marshallIn.send(new GenericMessage<Object>(person));
		GenericMessage<Result> res = (GenericMessage<Result>) marshalledOut.receive(2000);
		assertThat(res).as("No response recevied").isNotNull();
		assertThat(res.getPayload() instanceof DOMResult).as("payload was not a DOMResult").isTrue();
		Document doc = (Document) ((DOMResult) res.getPayload()).getNode();
		assertThat(doc.getDocumentElement().getLocalName()).as("Wrong name for root element ").isEqualTo("person");
	}


	@SuppressWarnings("unchecked")
	@Test
	public void testUnmarshalling() throws Exception {
		StringSource source = new StringSource("<person><firstname>bob</firstname></person>");
		unmarshallIn.send(new GenericMessage<Source>(source));
		GenericMessage<Object> res = (GenericMessage<Object>) unmarshallOut.receive(2000);
		assertThat(res).as("No response").isNotNull();
		assertThat(res.getPayload() instanceof JaxbAnnotatedPerson).as("Not a Person ").isTrue();
		JaxbAnnotatedPerson person = (JaxbAnnotatedPerson) res.getPayload();
		assertThat(person.getFirstName()).as("Worng firstname").isEqualTo("bob");

	}


}
