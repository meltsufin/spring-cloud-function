/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.function.adapter.gcloud;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.mockito.Mockito;

import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the HTTP functions adapter for Google Cloud Functions.
 *
 * @author Dmitry Solomakha
 * @author Mike Eltsufin
 */
public class FunctionInvokerTests {

	private static final Gson gson = new Gson();

	@Rule
	public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

	private static final String DROPPED_LOG_PREFIX = "Dropping background function result: ";

	@Test
	public void testHelloWorldSupplier() throws Exception {
		testHttpFunction(HelloWorldSupplier.class, null, "Hello World!");
	}

	@Test
	public void testJsonInputFunction() throws Exception {
		testHttpFunction(JsonInputFunction.class, new IncomingRequest("hello"),
				"Thank you for sending the message: hello");
	}

	@Test
	public void testJsonInputOutputFunction() throws Exception {
		testHttpFunction(JsonInputOutputFunction.class, new IncomingRequest("hello"),
				new OutgoingResponse("Thank you for sending the message: hello"));
	}

	@Test
	public void testJsonInputConsumer_Background() throws Exception {
		testHttpFunction(JsonInputConsumer.class, new IncomingRequest("hello"), null);
	}

	@Test
	public void testHelloWorldSupplier_Background() throws Exception {
		testBackgroundFunction(HelloWorldSupplier.class, null, "Hello World!", null);
	}

	@Test
	public void testJsonInputFunction_Background() throws Exception {
		testBackgroundFunction(JsonInputFunction.class, new IncomingRequest("hello"),
				"Thank you for sending the message: hello", null);
	}

	@Test
	public void testJsonInputOutputFunction_Background() throws Exception {
		testBackgroundFunction(JsonInputOutputFunction.class, new IncomingRequest("hello"),
				new OutgoingResponse("Thank you for sending the message: hello"), null);
	}

	@Test
	public void testJsonInputConsumer() throws Exception {
		testBackgroundFunction(JsonInputConsumer.class, new IncomingRequest("hello"), null,
				"Thank you for sending the message: hello");
	}

	@Test
	public void testPubSubBackgroundFunction_PubSub() throws Exception {
		PubSubMessage pubSubMessage = new PubSubMessage();
		pubSubMessage.data = "hello";
		testBackgroundFunction(PubsubBackgroundFunction.class, pubSubMessage, null,
				"Thank you for sending the message: hello");
	}

	private <I, O> void testHttpFunction(Class<?> configurationClass, I input, O expectedOutput) throws Exception {
		try (FunctionInvoker handler = new FunctionInvoker(configurationClass);) {

			HttpRequest request = Mockito.mock(HttpRequest.class);

			if (input != null) {
				when(request.getReader()).thenReturn(new BufferedReader(new StringReader(gson.toJson(input))));
			}

			HttpResponse response = Mockito.mock(HttpResponse.class);
			StringWriter writer = new StringWriter();
			when(response.getWriter()).thenReturn(new BufferedWriter(writer));

			handler.service(request, response);
			if (expectedOutput != null) {
				assertThat(writer.toString()).isEqualTo(gson.toJson(expectedOutput));
			}
		}
	}

	private <I, O> void testBackgroundFunction(Class<?> configurationClass, I input, O expectedResult,
			String expectedSysOut) {

		FunctionInvoker handler = new FunctionInvoker(configurationClass);

		handler.accept(gson.toJson(input), null);

		// verify function sysout statements
		if (expectedSysOut != null) {
			assertThat(systemOutRule.getLog()).contains(expectedSysOut);
		}

		// verify that if function had a return type, it was logged as being dropped
		if (expectedResult != null) {
			assertThat(systemOutRule.getLog()).contains(DROPPED_LOG_PREFIX + gson.toJson(expectedResult));
		}
		else {
			assertThat(systemOutRule.getLog()).doesNotContain(DROPPED_LOG_PREFIX);
		}

	}

	@Configuration
	@Import({ ContextFunctionCatalogAutoConfiguration.class })
	protected static class HelloWorldSupplier {

		@Bean
		public Supplier<String> supplier() {
			return () -> "Hello World!";
		}

	}

	@Configuration
	@Import({ ContextFunctionCatalogAutoConfiguration.class })
	protected static class JsonInputFunction {

		@Bean
		public Function<IncomingRequest, String> function() {
			return (in) -> "Thank you for sending the message: " + in.message;
		}

	}

	@Configuration
	@Import({ ContextFunctionCatalogAutoConfiguration.class })
	protected static class JsonInputOutputFunction {

		@Bean
		public Function<IncomingRequest, Message<OutgoingResponse>> function() {
			return (in) -> {
				return MessageBuilder
						.withPayload(new OutgoingResponse("Thank you for sending the message: " + in.message))
						.setHeader("foo", "bar").build();
			};
		}

	}

	@Configuration
	@Import({ ContextFunctionCatalogAutoConfiguration.class })
	protected static class JsonInputConsumer {

		@Bean
		public Consumer<IncomingRequest> function() {
			return (in) -> System.out.println("Thank you for sending the message: " + in.message);
		}

	}

	private static class IncomingRequest {

		String message;

		IncomingRequest(String message) {
			this.message = message;
		}

	}

	private static class OutgoingResponse {

		String message;

		OutgoingResponse(String message) {
			this.message = message;
		}

	}

	@Configuration
	@Import({ ContextFunctionCatalogAutoConfiguration.class })
	protected static class PubsubBackgroundFunction {

		@Bean
		public Consumer<PubSubMessage> consumer() {
			return (in) -> System.out.println("Thank you for sending the message: " + in.data);
		}

	}

	private static class PubSubMessage {

		String data;

		Map<String, String> attributes;

		String messageId;

		String publishTime;

	}

}
