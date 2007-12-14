/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.samples;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import org.springframework.integration.endpoint.annotation.MessageEndpoint;
import org.springframework.integration.endpoint.annotation.Polled;

/**
 * @author Mark Fisher
 */
@MessageEndpoint(defaultOutput="quotes")
public class QuotePublisher {

	@Polled(period=300)
	public Quote getQuote() {
		BigDecimal price = new BigDecimal(new Random().nextDouble() * 100);
		return new Quote("SOA", price.setScale(2, RoundingMode.HALF_EVEN));
	}

}
