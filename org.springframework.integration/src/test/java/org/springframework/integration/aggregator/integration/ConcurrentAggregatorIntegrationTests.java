package org.springframework.integration.aggregator.integration;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ConcurrentAggregatorIntegrationTests {
	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Test
	public void configOk() throws Exception {
		// nothing to assert
	}

	@Test(timeout = 1000)
	public void aggregate() throws Exception {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			input.send(new GenericMessage<Integer>(i, headers));
		}
		assertEquals(0+1+2+3+4, output.receive().getPayload());
	}

	//configured in context associated with this test
	public static class SummingAggregator {
		public Integer sum(List<Integer> numbers) {
			int result = 0;
			for (Integer number : numbers) {
				result += number;
			}
			return result;
		}
	}

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correllationId) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(MessageHeaders.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(MessageHeaders.SEQUENCE_SIZE, sequenceSize);
		headers.put(MessageHeaders.CORRELATION_ID, correllationId);
		return headers;
	}

}
