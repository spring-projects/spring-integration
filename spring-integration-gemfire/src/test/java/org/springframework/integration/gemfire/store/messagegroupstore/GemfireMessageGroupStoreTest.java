package org.springframework.integration.gemfire.store.messagegroupstore;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Tests the Gemfire {@link org.springframework.integration.store.MessageGroupStore} implementation,
 * {@link org.springframework.integration.gemfire.store.GemfireMessageGroupStore}.
 * <p/>
 * It tests the {@link org.springframework.integration.store.MessageGroupStore} by sending 10 batches of letters (all of the same width),
 * and then counting on the other end that indeed all 10 batches arrived and that all letters expected are there.
 * *
 *
 * @author Josh Long
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {GemfireMessageGroupStoreTestConfiguration.class})
public class GemfireMessageGroupStoreTest {

	@Autowired private GemfireMessageGroupStoreTestConfiguration.FakeMessageConsumer consumer;

	private List<String> letters = GemfireMessageGroupStoreTestConfiguration.LIST_OF_STRINGS;
	private int maxSize = 10;
	private long totalTimeSleeping = 10 * 1000; // give it 10s to send the messages

	@Test
	public void testGemfireMessageGroupStore() throws Throwable {
		long counter = 0;
		int delay = 1000;
		Set<Collection<Object>> batches = consumer.getBatches();

		while (batches.size() < maxSize && counter < totalTimeSleeping) {
			counter += delay;
			Thread.sleep(delay);
		}

		Assert.assertTrue(batches.size() == maxSize);
		for (Collection<Object> collection : batches) {
			Assert.assertTrue(letters.size() == collection.size());
			for (String c : this.letters) {
				Assert.assertTrue(collection.contains(c));
			}
			for (Object o : collection) {
				Assert.assertTrue(o instanceof String);
			}
		}
	}
}