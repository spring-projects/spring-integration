/**
 * 
 */
package org.springframework.integration.xmpp.config;

import static org.mockito.Mockito.when;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author ozhurakousky
 *
 */
public class XmppConnectionParserTests {

	@Test
	@Ignore // temporary
	public void testSimpleConfiguration(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmppConnectionParserTest-simple.xml", this.getClass());
	}
}
