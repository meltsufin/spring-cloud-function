/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.cloud.function.context.catalog;

import java.util.Collections;
import java.util.Set;

import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.catalog.SimpleFunctionRegistry.FunctionInvocationWrapper;
import org.springframework.cloud.function.context.config.RoutingFunction;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 */
public interface FunctionInspector {

	FunctionRegistration<?> getRegistration(Object function);

	default boolean isMessage(Object function) {
		FunctionRegistration<?> registration = getRegistration(function);
		if (registration != null && registration.getTarget() instanceof FunctionInvocationWrapper
				&& ((FunctionInvocationWrapper) registration.getTarget()).getTarget() instanceof RoutingFunction) {
			// we always want to give routing function as much information as possible
			return true;
		}
		return registration == null ? false : registration.getType().isMessage();
	}

	default Class<?> getInputType(Object function) {
		FunctionRegistration<?> registration = getRegistration(function);
		return registration == null ? Object.class
				: registration.getType().getInputType();
	}

	default Class<?> getOutputType(Object function) {
		FunctionRegistration<?> registration = getRegistration(function);
		return registration == null ? Object.class
				: registration.getType().getOutputType();
	}

	default Class<?> getInputWrapper(Object function) {
		FunctionRegistration<?> registration = getRegistration(function);
		return registration == null ? Object.class
				: registration.getType().getInputWrapper();
	}

	default Class<?> getOutputWrapper(Object function) {
		FunctionRegistration<?> registration = getRegistration(function);
		return registration == null ? Object.class
				: registration.getType().getOutputWrapper();
	}

	default String getName(Object function) {
		FunctionRegistration<?> registration = getRegistration(function);
		Set<String> names = registration == null ? Collections.emptySet()
				: registration.getNames();
		return names.isEmpty() ? null : names.iterator().next();
	}

}
