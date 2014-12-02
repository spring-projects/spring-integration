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
package org.springframework.integration.kafka.rule;

import java.io.IOException;
import java.net.Socket;

import javax.net.SocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * <p>
 * A rule that prevents integration tests from failing if the Kafka server is not running or not
 * accessible. If the Kafka server is not running in the background all the tests here will simply be skipped because
 * of a violated assumption (showing as successful).
 * <p>
 * The rule can be declared as static so that it only has to check once for all tests in the enclosing test case, but
 * there isn't a lot of overhead in making it non-static.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 1.0
 */
public class KafkaRunning extends TestWatcher {

	public static final int KAFKA_PORT = 9092;

	public static final int ZOOKEEPER_PORT = 2181;

	private static final Log logger = LogFactory.getLog(KafkaRunning.class);

	/**
	 * @return a new rule that assumes an existing running broker
	 */
	public static KafkaRunning isRunning() {
		return new KafkaRunning();
	}

	@Override
	public Statement apply(Statement base, Description description) {

		Socket kSocket = null;
		Socket zSocket = null;
		try {
			kSocket = SocketFactory.getDefault().createSocket("localhost", KAFKA_PORT);
			kSocket.getInputStream();
			zSocket = SocketFactory.getDefault().createSocket("localhost", ZOOKEEPER_PORT);
			zSocket.getInputStream();
		}
		catch (final Exception e) {
			logger.warn("Not executing tests because basic connectivity test failed");
			Assume.assumeNoException(e);
		}
		finally {
			if (kSocket != null) {
				try {
					kSocket.close();
				}
				catch (IOException e) {
				}
			}
			if (zSocket != null) {
				try {
					zSocket.close();
				}
				catch (IOException e) {
				}
			}
		}

		return super.apply(base, description);
	}

}
