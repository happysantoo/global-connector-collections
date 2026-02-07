package com.example.connector.core.transport;

/**
 * Named registration of an inbound transport for Actuator aggregation.
 */
public record TransportRegistration(String name, InboundTransport transport) {}
