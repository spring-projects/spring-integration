package si;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.jms.config.ActiveMqTestUtils;

public class Broker {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		ActiveMqTestUtils.prepare();
		new ClassPathXmlApplicationContext("broker.xml", Broker.class);
		System.in.read();
	}

}
