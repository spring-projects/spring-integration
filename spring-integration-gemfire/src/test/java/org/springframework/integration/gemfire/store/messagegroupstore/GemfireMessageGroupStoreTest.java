package org.springframework.integration.gemfire.store.messagegroupstore;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Tests the Gemfire
 * {@link org.springframework.integration.store.MessageGroupStore}
 * implementation,
 * {@link org.springframework.integration.gemfire.store.GemfireMessageGroupStore}
 * .
 * <p/>
 * It tests the {@link org.springframework.integration.store.MessageGroupStore}
 * by sending 10 batches of letters (all of the same width), and then counting
 * on the other end that indeed all 10 batches arrived and that all letters
 * expected are there. *
 * 
 * @author Josh Long
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { GemfireMessageGroupStoreTestConfiguration.class })
public class GemfireMessageGroupStoreTest {

	@Autowired
	private GemfireMessageGroupStoreTestConfiguration.FakeMessageConsumer consumer;
	@Autowired
	private GemfireMessageGroupStoreTestConfiguration.FakeMessageProducer producer;

	private List<String> letters = GemfireMessageGroupStoreTestConfiguration.LIST_OF_STRINGS;

	private int maxSize = 10;

	@Test
	public void testGemfireMessageGroupStore() throws Exception {
			producer.afterPropertiesSet();
			producer.start();
			List<Collection<Object>> batches = consumer.getBatches();
			assertEquals(maxSize, batches.size());
			for (Collection<Object> collection : batches) {
				Assert.assertTrue(letters.size() == collection.size());
				for (String c : this.letters) {
					Assert.assertTrue(collection.contains(c));
				}
				for (Object o : collection) {
					Assert.assertTrue(o instanceof String);
				}
			}
			producer.stop();
	}
}