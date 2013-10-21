/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.TestChannelResolver;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.CollectionUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class RouterTests {

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelRaisesMessageDeliveryExceptionByDefault() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@Override
			protected List<Object> getChannelKeys(Message<?> message) {
				return null;
			}
		};
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelIdentifierUsingChannelResolverRaisesMessageDeliveryExceptionByDefault() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@Override
			protected List<Object> getChannelKeys(Message<?> message) {
				return null;
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void nullChannelIdentifierInListRaisesMessageDeliveryExceptionByDefault() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@Override
			protected List<Object> getChannelKeys(Message<?> message) {
				return Collections.singletonList(null);
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessageDeliveryException.class)
	public void emptyChannelNameArrayRaisesMessageDeliveryExceptionByDefault() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			protected List<Object> getChannelKeys(Message<?> message) {
				return new ArrayList<Object>();
			}
		};
		TestChannelResolver channelResolver = new TestChannelResolver();
		router.setChannelResolver(channelResolver);
		Message<String> message = new GenericMessage<String>("test");
		router.handleMessage(message);
	}

	@Test(expected = MessagingException.class)
	public void channelMappingIsRequiredWhenResolvingChannelNames() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message){
				return CollectionUtils.arrayToList(new String[] { "notImportant" });
			}
		};
		router.setBeanFactory(mock(BeanFactory.class));
		router.handleMessage(new GenericMessage<String>("this should fail"));
	}

	@Test
	public void beanFactoryWithRouter() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "testChannel" });
			}
		};
		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testChannel", testChannel);

		router.setBeanFactory(context);
		context.refresh();
		router.handleMessage(new GenericMessage<String>("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void beanFactoryWithRouterAndMultipleCommaSeparatedChannelNames() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "testChannel1, , testChannel2  " });
			}
		};

		QueueChannel testChannel1 = new QueueChannel();
		QueueChannel testChannel2 = new QueueChannel();

		GenericApplicationContext context = new GenericApplicationContext();

		context.getBeanFactory().registerSingleton("testChannel1", testChannel1);
		context.getBeanFactory().registerSingleton("testChannel2", testChannel2);

		router.setBeanFactory(context);
		context.refresh();

		router.handleMessage(new GenericMessage<String>("test"));

		Message<?> reply1 = testChannel1.receive(0);
		assertEquals("test", reply1.getPayload());

		Message<?> reply2 = testChannel2.receive(0);
		assertEquals("test", reply2.getPayload());
	}

	@Test(expected = MessagingException.class)
	public void channelResolutionIsRequiredByDefault() {

		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "testChannelDoesNotExist", "testChannel" });
			}
		};

		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testChannel", testChannel);
		context.refresh();

		router.setBeanFactory(context);
		router.handleMessage(new GenericMessage<String>("test"));

	}

	@Test
	public void unresolvableChannelIdentifierInListAreIgnoredWhenResolutionRequiredIsFalse() {

		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "testChannelDoesNotExist", "testChannel" });
			}
		};

		router.setResolutionRequired(false);

		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testChannel", testChannel);

		router.setBeanFactory(context);
		context.refresh();
		router.handleMessage(new GenericMessage<String>("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());

	}

	@Test
	public void beanFactoryWithRouterAndChannelPrefix() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "MyChannel" });
			}
		};

		router.setPrefix("testing_");

		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testing_MyChannel", testChannel);

		router.setBeanFactory(context);
		context.refresh();
		router.handleMessage(new GenericMessage<String>("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());
	}


	@Test(expected = MessagingException.class)
	public void beanFactoryWithRouterAndChannelPrefixFailing() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "testing_MyChannel" });
			}
		};

		router.setPrefix("testing_");

		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("testing_MyChannel", testChannel);
		context.refresh();

		router.setBeanFactory(context);
		router.handleMessage(new GenericMessage<String>("test"));

	}

	@Test
	public void beanFactoryWithRouterAndChannelSuffix() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "MyChannel" });
			}
		};

		router.setSuffix("_withSuffix");

		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("MyChannel_withSuffix", testChannel);

		router.setBeanFactory(context);
		context.refresh();
		router.handleMessage(new GenericMessage<String>("test"));
		Message<?> reply = testChannel.receive(0);
		assertEquals("test", reply.getPayload());
	}

	@Test(expected = MessagingException.class)
	public void beanFactoryWithRouterAndChannelSuffixFailing() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new String[] { "MyChannel_withSuffix" });
			}
		};

		router.setSuffix("_withSuffix");

		QueueChannel testChannel = new QueueChannel();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("MyChannel_withSuffix", testChannel);
		context.refresh();

		router.setBeanFactory(context);
		router.handleMessage(new GenericMessage<String>("test"));

	}

	@Test
	public void beanFactoryWithRouterAndChannelIdentifiersInListWithinAList() {
		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {

				List<String> channelNames1 = CollectionUtils.arrayToList(new String[] { "channel1" });
				List<String> channelNames2 = CollectionUtils.arrayToList(new String[] { "channel2" });

				List<Object> listWithListOfChannelNames = new ArrayList<Object>();

				listWithListOfChannelNames.add(channelNames1);
				listWithListOfChannelNames.add(channelNames2);

				return listWithListOfChannelNames;
			}
		};

		QueueChannel testChannel1 = new QueueChannel();
		QueueChannel testChannel2 = new QueueChannel();

		GenericApplicationContext context = new GenericApplicationContext();

		context.getBeanFactory().registerSingleton("channel1", testChannel1);
		context.getBeanFactory().registerSingleton("channel2", testChannel2);
		context.refresh();

		router.setBeanFactory(context);

		router.handleMessage(new GenericMessage<String>("test"));

		Message<?> reply1 = testChannel1.receive(0);
		assertEquals("test", reply1.getPayload());

		Message<?> reply2 = testChannel2.receive(0);
		assertEquals("test", reply2.getPayload());
	}

	@Test
	public void beanFactoryWithRouterAndChannelIdentifiersInMessageChannelArrayWithinAList() {

		final QueueChannel testChannel1 = new QueueChannel();
		final QueueChannel testChannel2 = new QueueChannel();

		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {

			protected List<Object> getChannelKeys(Message<?> message) {

				MessageChannel[] channelNames1 = new MessageChannel[] { testChannel1 };
				MessageChannel[] channelNames2 = new MessageChannel[] { testChannel2 };

				List<Object> listWithListOfChannelNames = new ArrayList<Object>();

				listWithListOfChannelNames.add(channelNames1);
				listWithListOfChannelNames.add(channelNames2);

				return listWithListOfChannelNames;
			}
		};

		GenericApplicationContext context = new GenericApplicationContext();

		context.getBeanFactory().registerSingleton("channel1", testChannel1);
		context.getBeanFactory().registerSingleton("channel2", testChannel2);
		context.refresh();

		router.setBeanFactory(context);
		router.handleMessage(new GenericMessage<String>("test"));

		Message<?> reply1 = testChannel1.receive(0);
		assertEquals("test", reply1.getPayload());

		Message<?> reply2 = testChannel2.receive(0);
		assertEquals("test", reply2.getPayload());
	}

	@Test
	public void beanFactoryWithRouterAndRetrieveChannelIdentifiersUsingDefaultConversionService() {

		final QueueChannel testChannel1 = new QueueChannel();
		final QueueChannel testChannel2 = new QueueChannel();

		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new Integer[] { 100, 200 });
			}
		};

		GenericApplicationContext context = new GenericApplicationContext();

		context.getBeanFactory().registerSingleton("100", testChannel1);
		context.getBeanFactory().registerSingleton("200", testChannel2);

		router.setBeanFactory(context);
		context.refresh();

		router.handleMessage(new GenericMessage<String>("test"));

		Message<?> reply1 = testChannel1.receive(0);
		assertEquals("test", reply1.getPayload());

		Message<?> reply2 = testChannel2.receive(0);
		assertEquals("test", reply2.getPayload());
	}

	private class CustomObjectWithChannelName {

		String channel = "channel1";

		@SuppressWarnings("unused")
		public String getChannel() {
			return this.channel;
		}

	}

	@Test(expected = MessagingException.class)
	public void beanFactoryWithRouterAndRetrieveChannelIdentifierUsingDefaultConversionServiceFailing() {

		final QueueChannel testChannel1 = new QueueChannel();

		AbstractMappingMessageRouter router = new AbstractMappingMessageRouter() {
			@SuppressWarnings("unchecked")
			protected List<Object> getChannelKeys(Message<?> message) {
				return CollectionUtils.arrayToList(new CustomObjectWithChannelName[] { new CustomObjectWithChannelName() });
			}
		};

		GenericApplicationContext context = new GenericApplicationContext();

		context.getBeanFactory().registerSingleton("channel1", testChannel1);
		context.refresh();

		router.setBeanFactory(context);
		router.handleMessage(new GenericMessage<String>("test"));

	}

}
