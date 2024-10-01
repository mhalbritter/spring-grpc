/*
 * Copyright 2024-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.grpc.server;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Lists;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerProvider;
import io.grpc.ServerServiceDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base implementation for {@link GrpcServerFactory gRPC service factories}.
 * <p>
 * The server builder implementation is discovered via Java's SPI mechanism.
 *
 * @author David Syer
 * @author Chris Bono
 * @param <T> the type of server builder
 * @see ServerProvider#provider()
 */
public class BaseGrpcServerFactory<T extends ServerBuilder<T>> implements GrpcServerFactory {

	// VisibleForSubclass
	protected final Log logger = LogFactory.getLog(getClass());

	private final List<ServerServiceDefinition> serviceList = Lists.newLinkedList();

	private final String address;

	private final int port;

	private final List<ServerBuilderCustomizer<T>> serverBuilderCustomizers;

	public BaseGrpcServerFactory(String address, int port, List<ServerBuilderCustomizer<T>> serverBuilderCustomizers) {
		this.address = address;
		this.port = port;
		this.serverBuilderCustomizers = Objects.requireNonNull(serverBuilderCustomizers, "serverBuilderCustomizers");
	}

	protected String getAddress() {
		return this.address;
	}

	protected int getPort() {
		return this.port;
	}

	@Override
	public Server createServer() {
		T builder = newServerBuilder();
		configure(builder, this.serviceList);
		return builder.build();
	}

	@Override
	public void addService(ServerServiceDefinition service) {
		this.serviceList.add(service);
	}

	/**
	 * Creates a new server builder.
	 * @return The newly created server builder.
	 */
	@SuppressWarnings("unchecked")
	protected T newServerBuilder() {
		return (T) ServerBuilder.forPort(port);
	}

	/**
	 * Configures the server builder by adding service definitions and applying
	 * customizers to the builder.
	 * <p>
	 * Subclasses can override this to add features that are not yet supported by this
	 * library.
	 * @param builder the server builder to configure
	 * @param serviceDefinitions the service definitions to add to the builder
	 */
	protected void configure(T builder, List<ServerServiceDefinition> serviceDefinitions) {
		configureServices(builder, serviceDefinitions);
		this.serverBuilderCustomizers.forEach((c) -> c.customize(builder));
	}

	/**
	 * Configure the services to be served by the server.
	 * @param builder the server builder to add the services to
	 * @param serviceDefinitions the service definitions to configure and add to the
	 * builder
	 */
	protected void configureServices(T builder, List<ServerServiceDefinition> serviceDefinitions) {
		Set<String> serviceNames = new LinkedHashSet<>();
		serviceDefinitions.forEach((service) -> {
			String serviceName = service.getServiceDescriptor().getName();
			if (!serviceNames.add(serviceName)) {
				throw new IllegalStateException("Found duplicate service implementation: " + serviceName);
			}
			logger.info("Registered gRPC service: " + serviceName);
			builder.addService(service);
		});
	}

}