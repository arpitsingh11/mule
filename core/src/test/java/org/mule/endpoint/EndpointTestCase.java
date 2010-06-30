/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.EndpointMessageProcessorsFactory;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class EndpointTestCase extends AbstractMuleTestCase
{
    /**
     * Tests that endpoint configuration is done before setting the endpoint in the
     * passed transformers to avoid a race condition when the transformer asks for
     * endpoint's information that has not ben set yet. Related to these issues:
     * EE-1937, MULE-3983
     */
    @SuppressWarnings("serial")
    public void testTransformersAreSetupAfterCompleteEndpointConfig()
    {
        // Defines all the values required in order to create a full configured
        // endpoint
        final Connector mockConnector = mock(Connector.class);
        final EndpointURI uri = mock(EndpointURI.class);
        final List<Transformer> inputTransformers = new ArrayList<Transformer>();
        final List<Transformer> outputTransformers = new ArrayList<Transformer>();
        final String name = "testEndpoint";
        final Map<String, String> properties = new HashMap<String, String>();
        final String property1 = "property1";
        final String value1 = "value1";
        properties.put(property1, value1);
        final TransactionConfig mockTransactionConfig = mock(TransactionConfig.class);
        final Filter mockFilter = mock(Filter.class);
        final boolean deleteUnacceptedMessages = true;
        final EndpointSecurityFilter mockEndpointSecurityFilter = mock(EndpointSecurityFilter.class);
        final boolean synchronous = true;
        final int responseTimeout = 5;
        final String initialState = "state1";
        final String endpointEncoding = "enconding1";
        final String endpointBuilderName = "builderName1";
        final MuleContext muleContext = mock(MuleContext.class);
        final RetryPolicyTemplate retryPolicyTemplate = mock(RetryPolicyTemplate.class);
        final EndpointMessageProcessorsFactory messageProcessorsFactory = mock(EndpointMessageProcessorsFactory.class);
        final List<MessageProcessor> messageProcessors = new ArrayList<MessageProcessor>();
        final List<MessageProcessor> responseMessageProcessors = new ArrayList<MessageProcessor>();

        // Creates a mock Transformer that will check that the endpoint is completely
        // configured when setEndpoint method is called.
        Transformer mockTransformer = mock(Transformer.class);
        doAnswer(new Answer<Object>()
        {
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                AbstractEndpoint endpoint = (AbstractEndpoint) invocation.getArguments()[0];
                assertEquals(mockConnector, endpoint.getConnector());
                assertEquals(uri, endpoint.getEndpointURI());
                assertEquals(name, endpoint.getName());
                assertEquals(value1, endpoint.getProperties().get(property1));
                assertEquals(mockTransactionConfig, endpoint.getTransactionConfig());
                assertEquals(mockFilter, endpoint.getFilter());
                assertEquals(deleteUnacceptedMessages, endpoint.isDeleteUnacceptedMessages());
                assertEquals(mockEndpointSecurityFilter, endpoint.getSecurityFilter());
                assertEquals(synchronous, endpoint.isSynchronous());
                assertEquals(responseTimeout, endpoint.getResponseTimeout());
                assertEquals(initialState, endpoint.getInitialState());
                assertEquals(endpointEncoding, endpoint.getEncoding());
                assertEquals(endpointBuilderName, endpoint.getEndpointBuilderName());
                assertEquals(muleContext, endpoint.getMuleContext());
                assertEquals(retryPolicyTemplate, endpoint.getRetryPolicyTemplate());

                return null;
            }
        }).when(mockTransformer).setEndpoint(any(ImmutableEndpoint.class));

        inputTransformers.add(mockTransformer);
        outputTransformers.add(mockTransformer);

        // Creates the endpoint using the transformers which will validate the
        // configuration
        new AbstractEndpoint(mockConnector, uri, inputTransformers, outputTransformers, name, properties,
            mockTransactionConfig, mockFilter, deleteUnacceptedMessages, mockEndpointSecurityFilter,
            synchronous, responseTimeout, initialState, endpointEncoding, endpointBuilderName, muleContext,
            retryPolicyTemplate, messageProcessorsFactory, messageProcessors, responseMessageProcessors)
        {
            @Override
            protected MessageProcessor createMessageProcessorChain() throws MuleException
            {
                return null;
            }
        };
    }
}
