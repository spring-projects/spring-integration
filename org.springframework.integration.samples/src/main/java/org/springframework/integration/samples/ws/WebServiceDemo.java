/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.samples.ws;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.ws.handler.AbstractWebServiceHandler;

/**
 * Demonstrating a web service invocation through a Web Service MessageHandler.
 * 
 * @author Marius Bogoevici
 */
public class WebServiceDemo {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("temperatureConversion.xml", WebServiceDemo.class);

		// Compose the XML message according to the server's schema
		String requestXml =
				"<FahrenheitToCelsius xmlns=\"http://tempuri.org/\">" +
				"  <Fahrenheit>90.0</Fahrenheit>" +
				"</FahrenheitToCelsius>";

		// Create the Message object
		// In this case the service expects a SoapAction header
		Message<String> message = MessageBuilder.fromPayload(requestXml)
				.setHeader(AbstractWebServiceHandler.SOAP_ACTION_PROPERTY_KEY, "http://tempuri.org/FahrenheitToCelsius")
				.build();

		// Send the Message to the handler's input channel
		((MessageChannel) context.getBean("fahrenheitChannel")).send(message);
	}

}
