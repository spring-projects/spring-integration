/*
 * Copyright 2016-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.nats.math;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
public final class Equations {

	private Equations() {
	}

	private static final Log LOG = LogFactory.getLog(Equations.class);

	/**
	 * @param x           - changeable input value
	 * @param threshold   - starting threshold value given in percentage (from 0% to 100%) from which
	 *                    the function start to delivery increasing f(x) values
	 * @param granularity - defines how granular the power of 2 can be created. If it 5 it generates
	 *                    the values 2 pow 1 , 2 pow 2, 2 pow 3 etc.
	 * @return e.g. by calling Equations.binaryBased(occupancy, 75, 5) f(0)= 0,f(5)= 0,f(10)= 0,f(15)=
	 * 0,f(20)= 0,f(25)= 0,f(30)= 0,f(35)= 0,f(40)= 0,f(45)= 0,f(50)= 0,f(55)= 0,f(60)= 0,f(65)=
	 * 0,f(70)= 0 but starting only from threshold value 75% f(75)= 1 f(80)= 2 f(85)= 4 f(90)= 8
	 * f(95)= 16 f(100)= 32
	 */
	public static int binaryBased(double x, int threshold, int granularity) {
		if (threshold >= 0 && threshold <= 100) {
			byte factor = (byte) Math.pow(2, (x - threshold) / granularity);
			return factor;
		}
		else {
			LOG.info("Wrong usage of function with threshold value=" + threshold);
			return 0;
		}
	}

	public static double percentage(double value, double total) {
		return value * 100 / total;
	}
}
