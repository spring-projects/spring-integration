/**
 * 
 */
package org.springframework.integration.dispatcher;

import static junit.framework.Assert.assertTrue;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Oleg Zhurakousky
 *
 * This test was influenced by INT-1483 where by registering TX Advisor
 * in the BeanFactory while having <aop:config> resent resulted in 
 * TX Advisor being applied on all beans in AC
 */
public class TransactionalPollerWithMixedAopConfig {

	@Test
	public void validateTransactionalProxyIsolationToThePollerOnly(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("TransactionalPollerWithMixedAopConfig-context.xml", this.getClass());
		
		assertTrue(!(context.getBean("foo") instanceof Advised));
		assertTrue(!(context.getBean("inputChannel") instanceof Advised));
	}
	
	public static class SampleService{
		public void foo(String payload){}
	}
	
	public static class Foo{
		public Foo(String value){}
	}
	
	public static class SampleAdvice implements MethodInterceptor{
		public Object invoke(MethodInvocation invocation) throws Throwable {
			return invocation.proceed();
		}	
	}
}
