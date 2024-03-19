/*
 * Copyright 2016-2024 the original author or authors.
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
package org.springframework.integration.nats.listener;

import io.nats.client.Connection;
import io.nats.client.Consumer;
import io.nats.client.ErrorListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Default NATS Error listener. */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsErrorListener implements ErrorListener {

  private static final Log LOG = LogFactory.getLog(NatsErrorListener.class);

  @Override
  public void slowConsumerDetected(Connection conn, Consumer consumer) {
    LOG.warn(
        conn.getOptions().getConnectionName() + " - " + "NATS connection slow consumer detected");
  }

  @Override
  public void exceptionOccurred(Connection conn, Exception exp) {
    LOG.error(
        conn.getOptions().getConnectionName() + " - " + "NATS connection exception occurred", exp);
  }

  @Override
  public void errorOccurred(Connection conn, String error) {
    LOG.error(
        conn.getOptions().getConnectionName() + " - " + "NATS connection error occurred " + error);
  }
}
