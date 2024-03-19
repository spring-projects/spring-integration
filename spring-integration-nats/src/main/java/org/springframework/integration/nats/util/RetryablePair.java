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
package org.springframework.integration.nats.util;

/**
 * Retryable Pair
 *
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class RetryablePair<T1, T2> extends Pair<T1, T2> {

  private int retryCount;

  protected RetryablePair(T1 first, T2 second) {
    super(first, second);
    retryCount = 0;
  }

  public static <T1, T2> RetryablePair<T1, T2> of(T1 first, T2 second) {
    return new RetryablePair<T1, T2>(first, second);
  }

  public int increment() {
    return retryCount++;
  }

  public int getRetryCount() {
    return retryCount;
  }
}
