package org.springframework.integration.monitor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.channel.NullChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.ClassUtils;

/**
 * @author Tareq Abedrabbo
 * @author Dave Syer
 * @since 2.0.4
 */
public class MessageMetricsAdviceTests {

	private IntegrationMBeanExporter mBeanExporter;

	private MessageHandler handler;

	private MessageChannel channel;

	@Before
	public void setUp() throws Exception {
		channel = new NullChannel();
		mBeanExporter = new IntegrationMBeanExporter();
		mBeanExporter.setBeanFactory(new DefaultListableBeanFactory());
		mBeanExporter.setBeanClassLoader(ClassUtils.getDefaultClassLoader());
		mBeanExporter.afterPropertiesSet();
		handler = new DummyHandler();
	}

	@Test
	public void adviceExportedHandler() throws Exception {
		Advised advised = (Advised) mBeanExporter.postProcessAfterInitialization(handler, "test");

		DummyInterceptor interceptor = new DummyInterceptor();
		advised.addAdvice(interceptor);

		assertThat(advised.getAdvisors().length, equalTo(2));

		((MessageHandler) advised).handleMessage(MessageBuilder.withPayload("test").build());
		assertThat(interceptor.invoked, is(true));
	}

	@Test
	public void exportAdvisedHandler() throws Exception {

		DummyInterceptor interceptor = new DummyInterceptor();
		NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(interceptor);
		advisor.addMethodName("handleMessage");

		ProxyFactory factory = new ProxyFactory(handler);
		factory.addAdvisor(advisor);
		MessageHandler advised = (MessageHandler) factory.getProxy();

		MessageHandler exported = (MessageHandler) mBeanExporter.postProcessAfterInitialization(advised, "test");
		exported.handleMessage(MessageBuilder.withPayload("test").build());
	}

	@Test
	public void adviceExportedChannel() throws Exception {
		Advised advised = (Advised) mBeanExporter.postProcessAfterInitialization(channel, "testChannel");

		DummyInterceptor interceptor = new DummyInterceptor();
		advised.addAdvice(interceptor);

		assertThat(advised.getAdvisors().length, equalTo(2));

		((MessageChannel) advised).send(MessageBuilder.withPayload("test").build());
		assertThat(interceptor.invoked, is(true));
	}

	@Test
	public void exportAdvisedChannel() throws Exception {

		DummyInterceptor interceptor = new DummyInterceptor();
		NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(interceptor);
		advisor.addMethodName("send");

		ProxyFactory factory = new ProxyFactory(channel);
		factory.addAdvisor(advisor);
		MessageChannel advised = (MessageChannel) factory.getProxy();

		MessageChannel exported = (MessageChannel) mBeanExporter.postProcessAfterInitialization(advised, "test");
		exported.send(MessageBuilder.withPayload("test").build());
	}

	private static class DummyHandler implements MessageHandler {

		@SuppressWarnings("unused")
		boolean invoked = false;

		public void handleMessage(Message<?> message) throws MessagingException {
			invoked = true;
		}
	}

	private static class DummyInterceptor implements MethodInterceptor {

		boolean invoked = false;

		public Object invoke(MethodInvocation invocation) throws Throwable {
			invoked = true;
			return invocation.proceed();
		}

		@Override
		public String toString() {
			return super.toString() + "{" + "invoked=" + invoked + '}';
		}
	}
}
