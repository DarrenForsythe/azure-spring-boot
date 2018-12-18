/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.aad;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;

@Configuration
public class AADGraphHttpClientConfiguration {


    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @ConditionalOnMissingBean(AADGraphHttpClient.class)
    public AADGraphHttpClient aadHttpClient(ServiceEndpointsProperties serviceEndpointsProps,
                                            AADAuthenticationProperties aadAuthProps) {
        return new AADGraphHttpClientDefaultImpl(serviceEndpointsProps.getServiceEndpoints(
                aadAuthProps.getEnvironment()));
    }


    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @ConditionalOnMissingBean
    public AzureADGraphClient azureADGraphClient(AADGraphHttpClient aaaHttpClient,
                                                 AADAuthenticationProperties aadAuthProps,
                                                 ServiceEndpointsProperties serviceEndpointsProps) {
        return new AzureADGraphClient(aadAuthProps.getClientId(), aadAuthProps.getClientSecret(),
                aadAuthProps, serviceEndpointsProps, aaaHttpClient);
    }

}