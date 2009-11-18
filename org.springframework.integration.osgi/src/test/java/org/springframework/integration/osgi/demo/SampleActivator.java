package org.springframework.integration.osgi.demo;

import org.springframework.integration.core.Message;

public class SampleActivator {

	public void handle(Message<?> message){
		System.out.println("Message: " + message);
	}
}
