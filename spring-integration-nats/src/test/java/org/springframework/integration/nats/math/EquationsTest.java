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
package org.springframework.integration.nats.math;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

/** Unit Test for Equations Utility function */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class EquationsTest {

  /**
   * Verifies the binary based implementation of the equations method which returns increasing f(x)
   * values based on threshold
   *
   * <p>Eg: Equations.binaryBased(0, 75, 5); f(0)= 0,f(5)= 0,f(10)= 0,f(15)= 0,f(20)= 0,f(25)=
   * 0,f(30)= 0,f(35)= 0,f(40)= 0,f(45)= 0,f(50)= 0,f(55)= 0,f(60)= 0,f(65)= 0,f(70)= 0 * * f(75)= 1
   * * * f(80)= 2 * * f(85)= 4 * * f(90)= 8 * * f(95)= 16 * * f(100)= 32
   */
  @Test
  public void binaryBased() {
    int[] expected = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 4, 8, 16, 32};
    int i = 0;
    int granularity = 5;
    int threshold = 75;
    for (int x = 0; x <= 100; x = x + granularity) {
      int y = Equations.binaryBased(x, threshold, granularity);
      System.out.println("f(" + x + ")= " + y);
      Assert.assertEquals(expected[i], y);
      i++;
    }
  }

  /**
   * Verifies the binary based implementation of the equations method which returns increasing f(x)
   * values based on threshold where(x) is specified in percentage
   */
  @Test
  public void binaryBasedWithPercentage() {

    int total = 100000;
    int granularity = 5;
    int threshold = 75;
    for (int x = 0; x <= total; x = x + 10) {
      double y = Equations.percentage(x, total);
      System.out.println("y(" + x + ")= " + y);
      int z = Equations.binaryBased(y, threshold, granularity);
      System.out.println("z(" + x + ")= " + z);
      if (range(y, 0, 75)) {
        Assert.assertEquals(0, z);
      } else if (range(y, 75, 80)) {
        Assert.assertEquals(1, z);
      } else if (range(y, 80, 82)) {
        Assert.assertEquals(2, z);
      } else if (range(y, 85, 86.5)) {
        Assert.assertEquals(4, z);
      } else if (range(y, 90, 90.5)) {
        Assert.assertEquals(8, z);
      } else if (range(y, 95, 95.4)) {
        Assert.assertEquals(16, z);
      } else if (y == 100) {
        Assert.assertEquals(32, z);
      }
    }
  }

  private boolean range(double y, double between1, double between2) {
    return y >= between1 && y < between2;
  }
}
