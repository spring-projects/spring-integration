/**
 * 
 */
package org.springframework.integration.monitor;

import static junit.framework.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChainWithMessageProducingHandlersTests {

	@Autowired
	private ApplicationContext applicationContext;
	
	@Test
	public void testSuccessfulApplicationContext(){
		// this is all we need to do. Until INT-1431 was solved initialization of this AC would fail.
		assertNotNull(applicationContext);
	}
	
	public static class SampleProducer{
		public String echo(String value){
			return value;
		}
	}
	public static class SampleService{
		public void echo(String value){}
	}
}
