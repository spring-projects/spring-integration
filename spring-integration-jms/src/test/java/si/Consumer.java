package si;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		new ClassPathXmlApplicationContext("consumer.xml", Consumer.class);
	}

}
