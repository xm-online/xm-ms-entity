package com.icthh.xm.ms.entity.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ExampleTest {

	@Autowired
	private MessageCollector messageCollector;

	@Autowired
	private Processor processor;

	@Test
	public void testScheduler() {
		Message<String> message = new GenericMessage<>("{}");
		this.processor.input().send(message);
		Message<String> received = (Message<String>) this.messageCollector.forChannel(this.processor.output()).poll();
		assertThat(received.getPayload()).isEqualTo("hello world");
	}

	@EnableBinding(Processor.class)
	public static class MyProcessor {

		@Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
		public String transform(String in) {
			return in + " world";
		}
	}

	public interface Chanel {

    }

}
