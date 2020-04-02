/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.xml.transformer.jaxbmarshaling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.xml.transform.StringSource;

/**
 * @author Jonas Partner
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class JaxbMarshallingIntegrationTests {

	@Autowired
	@Qualifier("marshallIn")
	MessageChannel marshallIn;

	@Autowired
	@Qualifier("marshallOut")
	PollableChannel marshalledOut;

	@Autowired
	@Qualifier("unmarshallIn")
	MessageChannel unmarshallIn;

	@Autowired
	@Qualifier("unmarshallOut")
	PollableChannel unmarshallOut;

	@TempDir
	Path tempDirectory;

	@Test
	public void testMarshalling() {
		JaxbAnnotatedPerson person = new JaxbAnnotatedPerson();
		person.setFirstName("john");
		this.marshallIn.send(new GenericMessage<>(person));
		Message<?> res = this.marshalledOut.receive(2000);
		assertThat(res).as("No response received").isNotNull();
		assertThat(res.getPayload() instanceof DOMResult).as("payload was not a DOMResult").isTrue();
		Document doc = (Document) ((DOMResult) res.getPayload()).getNode();
		assertThat(doc.getDocumentElement().getLocalName()).as("Wrong name for root element ").isEqualTo("person");
	}


	@Test
	public void testUnmarshalling() {
		StringSource source = new StringSource("<person><firstname>bob</firstname></person>");
		this.unmarshallIn.send(new GenericMessage<Source>(source));
		Message<?> res = this.unmarshallOut.receive(2000);
		assertThat(res).as("No response").isNotNull();
		assertThat(res.getPayload() instanceof JaxbAnnotatedPerson).as("Not a Person ").isTrue();
		JaxbAnnotatedPerson person = (JaxbAnnotatedPerson) res.getPayload();
		assertThat(person.getFirstName()).as("Wrong firstname").isEqualTo("bob");
	}

	@Test
	public void testFileUnlockedAfterUnmarshallingFailure() throws IOException {
		Path tempFile = Files.createTempFile(this.tempDirectory, null, null);
		Files.write(tempFile, "junk".getBytes());
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> this.unmarshallIn.send(new GenericMessage<>(tempFile.toFile())))
				.withCauseInstanceOf(UnmarshallingFailureException.class)
				.withStackTraceContaining("Content is not allowed in prolog.");

		Files.delete(tempFile);
	}

}
