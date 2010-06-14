package org.springframework.integration.jdbc.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

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

@Transactional
public class JdbcPollingChannelAdapterParserTests {
	
	
	final long receiveTimeout = 5000;
	
	private SimpleJdbcTemplate jdbcTemplate;
	
	private MessageChannelTemplate channelTemplate;
	
	private ConfigurableApplicationContext appCtx;
		
	@Test
	public void testSimpleInboundChannelAdapter(){
		setUp("pollingForMapJdbcInboundChannelAdapterTest.xml", getClass());
		this.jdbcTemplate.update("insert into item values(1,'',2)");
		Message<?> message = channelTemplate.receive();
		assertNotNull("No message found ", message);
		assertTrue("Wrong payload type expected instance of List", message.getPayload() instanceof List<?>);
	}
	
	
	@Test
	public void testSimpleInboundChannelAdapterWithUpdate(){
		setUp("pollingForMapJdbcInboundChannelAdapterWithUpdateTest.xml", getClass());
		this.jdbcTemplate.update("insert into item values(1,'',2)");
		Message<?> message = channelTemplate.receive();
		assertNotNull(message);
		message = channelTemplate.receive();
		assertNull(channelTemplate.receive());
	}
	
	@Test
	public void testExtendedInboundChannelAdapter(){
		setUp("pollingWithJdbcOperationsJdbcInboundChannelAdapterTest.xml", getClass());
		this.jdbcTemplate.update("insert into item values(1,'',2)");
		Message<?> message = channelTemplate.receive();
		assertNotNull(message);
	}
	
	@Test
	public void testParameterSourceInboundChannelAdapter(){
		setUp("pollingWithParameterSourceJdbcInboundChannelAdapterTest.xml", getClass());
		this.jdbcTemplate.update("insert into item values(1,'',2)");
		Message<?> message = channelTemplate.receive();
		assertNotNull(message);
		List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM item WHERE status=1");
		assertEquals(1, list.size());
		assertEquals("bar", list.get(0).get("NAME"));
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
		this.channelTemplate.setReceiveTimeout(500);
	}
	
	protected void setupJdbcTemplate(){
		this.jdbcTemplate = new SimpleJdbcTemplate(this.appCtx.getBean("dataSource",DataSource.class));
	}

}
