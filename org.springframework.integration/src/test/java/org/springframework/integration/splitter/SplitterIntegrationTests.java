package org.springframework.integration.splitter;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@MessageEndpoint
public class SplitterIntegrationTests {

	@Autowired
	@Qualifier("inAnnotated")
	MessageChannel inAnnotated;

	@Autowired
	@Qualifier("inMethodInvoking")
	MessageChannel inMethodInvoking;

	@Autowired
	@Qualifier("inDefault")
	MessageChannel inDefault;

	private String sentence = "The quick brown fox jumped over the lazy dog";

	private List<String> words = Arrays.asList(sentence.split("\\s"));

	private List<String> receivedWords = new ArrayList<String>();

	@Test
	public void configOk() throws Exception {
		// just checking the parsing
	}

	@Splitter(inputChannel = "inAnnotated", outputChannel = "out")
	public List<String> split(String sentence) {
		return Arrays.asList(sentence.split("\\s"));
	}

	@Test @Ignore
	public void annotated() throws Exception {
		inAnnotated.send(new GenericMessage<String>("The quick brown fox jumped over the lazy dog"));
		assertTrue(this.receivedWords.containsAll(words));
		assertTrue(words.containsAll(this.receivedWords));
	}

	@ServiceActivator(inputChannel = "out")
	public void deliveredWords(String string) {
		this.receivedWords.add(string);
	}

	@Test @Ignore
	public void methodInvoking() throws Exception {
		inMethodInvoking.send(new GenericMessage<String>("The quick brown fox jumped over the lazy dog"));
		assertTrue(this.receivedWords.containsAll(words));
		assertTrue(words.containsAll(this.receivedWords));
	}

	@Test @Ignore
	public void defaultSplitter() throws Exception {
		inDefault.send(new GenericMessage<List<String>>(words));
		assertTrue(this.receivedWords.containsAll(words));
		assertTrue(words.containsAll(this.receivedWords));
	}

}
