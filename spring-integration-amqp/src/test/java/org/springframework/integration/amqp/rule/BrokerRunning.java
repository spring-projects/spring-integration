/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.amqp.rule;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.util.Assert;

/**
 * <p>
 * A rule that prevents integration tests from failing if the Rabbit broker application is not running or not
 * accessible. If the Rabbit broker is not running in the background all the tests here will simply be skipped because
 * of a violated assumption (showing as successful).
 * <p>
 * The rule can be declared as static so that it only has to check once for all tests in the enclosing test case, but
 * there isn't a lot of overhead in making it non-static.
 *
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class BrokerRunning extends TestWatcher {

	public static final int PORT = 5672;

	private static final Log logger = LogFactory.getLog(BrokerRunning.class);

	private static final Queue DEFAULT_QUEUE_NAME = new Queue(BrokerRunning.class.getName());

	private final Queue[] queues;

	/**
	 * Ensure the broker is running and has an empty queue (which can be addressed via the default exchange).
	 *
	 * @return a new rule that assumes an existing running broker
	 */
	public static BrokerRunning isRunningWithEmptyQueues(Queue... queues) {
		Assert.notNull(queues);
		Assert.noNullElements(queues);
		return new BrokerRunning(queues);
	}

	/**
	 * Ensure the broker is running and has an empty queue (which can be addressed via the default exchange).
	 *
	 * @return a new rule that assumes an existing running broker
	 */
	public static BrokerRunning isRunningWithEmptyQueues(String... queues) {
		Assert.notNull(queues);
		Assert.noNullElements(queues);
		return new BrokerRunning(queues);
	}

	/**
	 * @return a new rule that assumes an existing running broker
	 */
	public static BrokerRunning isRunning() {
		return new BrokerRunning(DEFAULT_QUEUE_NAME);
	}


	private BrokerRunning(Queue... queues) {
		this.queues = queues;
	}

	private BrokerRunning(String... queues) {
		List<Queue> queueList = new ArrayList<Queue>(queues.length);
		for (String queue : queues) {
			queueList.add(new Queue(queue));
		}
		this.queues = queueList.toArray(new Queue[queues.length]);
	}

	@Override
	public Statement apply(Statement base, Description description) {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		connectionFactory.setHost("localhost");

		try {

			connectionFactory.setPort(PORT);

			RabbitAdmin admin = new RabbitAdmin(connectionFactory);

			for (Queue queue : queues) {
				String queueName = queue.getName();
				logger.info("Deleting queue: " + queueName);
				// Delete completely - gets rid of consumers and bindings as well
				admin.deleteQueue(queueName);

				if (!DEFAULT_QUEUE_NAME.getName().equals(queueName)) {
					admin.declareQueue(queue);
				}
			}


		}
		catch (final Exception e) {
			logger.warn("Not executing tests because basic connectivity test failed", e);
			Assume.assumeNoException(e);
		}
		finally {
			connectionFactory.destroy();
		}

		return super.apply(base, description);
	}

}
