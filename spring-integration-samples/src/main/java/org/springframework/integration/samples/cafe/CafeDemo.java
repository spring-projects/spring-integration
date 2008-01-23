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

package org.springframework.integration.samples.cafe;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Provides the 'main' method for running the Cafe Demo application. When an
 * order is placed, the Cafe will send that order to the "orders" channel.
 * The relevant components are defined within the configuration file
 * ("cafeDemo.xml") or configured with annotations (such as the
 * OrderSplitter, DrinkRouter, and Barista classes).
 * 
 * @author Mark Fisher
 */
public class CafeDemo {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("cafeDemo.xml", CafeDemo.class);
		context.start();
		Cafe cafe = (Cafe) context.getBean("cafe");
		DrinkOrder order = new DrinkOrder();
		Drink hotDoubleLatte = new Drink(DrinkType.LATTE, 2, false);
		Drink icedTripleMocha = new Drink(DrinkType.MOCHA, 3, true);
		order.addDrink(hotDoubleLatte);
		order.addDrink(icedTripleMocha);
		for (int i = 0; i < 100; i++) {
			cafe.placeOrder(order);
		}
	}

}
