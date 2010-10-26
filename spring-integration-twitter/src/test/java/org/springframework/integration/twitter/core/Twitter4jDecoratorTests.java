/**
 * 
 */
package org.springframework.integration.twitter.core;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.integration.twitter.core.Twitter4jDecorator;

import twitter4j.Status;

/**
 * @author ozhurakousky
 *
 */
public class Twitter4jDecoratorTests {

	@Test
	public void testTwitter4JDecorator(){
		Status status = mock(Status.class);
		when(status.getSource()).thenReturn("Hello World");
		ProxyFactory factory = new ProxyFactory(org.springframework.integration.twitter.core.Status.class, EmptyTargetSource.INSTANCE);
		factory.addAdvice(new Twitter4jDecorator(status));
		Object decoratedStatus = factory.getProxy();
		assertTrue(decoratedStatus instanceof org.springframework.integration.twitter.core.Status);
		String source = ((org.springframework.integration.twitter.core.Status)decoratedStatus).getSource();
		System.out.println("source: " + source);
	}
}
