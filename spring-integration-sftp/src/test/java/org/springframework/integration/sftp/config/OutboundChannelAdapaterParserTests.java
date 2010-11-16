/**
 * 
 */
package org.springframework.integration.sftp.config;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author ozhurakousky
 *
 */
public class OutboundChannelAdapaterParserTests {

	@Test
	public void test(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("OutboundChannelAdapaterParserTests-context.xml", this.getClass());
	}
}
