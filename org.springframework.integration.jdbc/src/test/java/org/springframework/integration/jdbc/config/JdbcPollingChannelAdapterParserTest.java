package org.springframework.integration.jdbc.config;

import static org.junit.Assert.*;

import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.sun.xml.internal.xsom.impl.scd.Iterators.Map;

@Transactional
public class JdbcPollingChannelAdapterParserTest {
	
	
	final long receiveTimeout = 5000;
	
	SimpleJdbcTemplate jdbcTemplate;
	
	MessageChannelTemplate channelTemplate;
	
	ConfigurableApplicationContext appCtx;
	
	
	
	@Test
	public void testSimpleInboundChannelAdapter(){
		setUp("pollingForMapJdbcInboundChannelAdapterTest.xml", getClass());
		this.jdbcTemplate.update("insert into item values(1,2)");
		Message<?> message = channelTemplate.receive();
		assertNotNull("No message found ", message);
		assertTrue("Wrong payload type expected instance of List", message.getPayload() instanceof List);
	}
	
	
	@Test
	public void testSimpleInboundChannelAdapterWithUpdate(){
		setUp("pollingForMapJdbcInboundChannelAdapterWithUpdateTest.xml", getClass());
		this.jdbcTemplate.update("insert into item values(1,2)");
		Message<?> message = channelTemplate.receive();
		assertNotNull(message);
		message = channelTemplate.receive();
		assertNull(channelTemplate.receive());
	}
	
	@After
	public void tearDown(){
		if(appCtx != null){
			appCtx.close();
		}
	}
	
	public void setUp(String name, Class<?> cls){
		appCtx = new ClassPathXmlApplicationContext(name, cls);
		setupJdbcTemplate();
		setupMessageChannelTemplate();
	}
	
	
	protected void setupMessageChannelTemplate(){
		PollableChannel pollableChannel = this.appCtx.getBean("target", PollableChannel.class);
		this.channelTemplate =  new MessageChannelTemplate(pollableChannel);
		this.channelTemplate.setReceiveTimeout(5000);
	}
	
	protected void setupJdbcTemplate(){
		this.jdbcTemplate = new SimpleJdbcTemplate(this.appCtx.getBean("dataSource",DataSource.class));
	}

}
