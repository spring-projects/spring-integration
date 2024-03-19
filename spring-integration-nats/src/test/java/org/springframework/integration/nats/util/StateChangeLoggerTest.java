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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.integration.nats.math.Equations;

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class StateChangeLoggerTest {

  /** Test shows that by 4000 possible log cases only 12 logs entries are produced */
  @Test
  public void testLogAmount() {

    int logCounter = 0;
    boolean isChanged;

    // r is rational number which simulate real calculated value of occupancy
    double r = 0.123456;
    for (double occupancy = 0 + r; occupancy <= 100 + r; occupancy = occupancy + 5) {
      StateChangeLogger<Integer> changeLogger = new StateChangeLogger<>(0);
      int factor = Equations.binaryBased(occupancy, 75, 5);
      for (int i = 0; i < 100; i++) {
        isChanged =
            changeLogger.warnByStateChange(
                factor,
                String.format(
                    "NATS AckQueue of Spring Integration Framework reached occupancy=%,.2f%% of capacity!. Slowdown to the factor=%d",
                    occupancy, factor));
        if (isChanged) logCounter++;
      }
    }

    for (double occupancy = 100 + r; occupancy >= 0 + r; occupancy = occupancy - 5) {
      StateChangeLogger<Integer> changeLogger = new StateChangeLogger<>(0);
      int factor = Equations.binaryBased(occupancy, 75, 5);
      for (int i = 0; i < 100; i++) {
        isChanged =
            changeLogger.warnByStateChange(
                factor,
                String.format(
                    "NATS AckQueue of Spring Integration Framework reached occupancy=%,.2f%% of capacity!. Slowdown to the factor=%d",
                    occupancy, factor));
        if (isChanged) logCounter++;
      }
    }
    Assert.assertEquals(6 * 2, logCounter);
  }
}
