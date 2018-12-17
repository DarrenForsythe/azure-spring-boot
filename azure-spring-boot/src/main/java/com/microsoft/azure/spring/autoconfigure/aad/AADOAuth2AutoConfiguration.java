/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.aad;

import com.microsoft.aad.adal4j.ClientCredential;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "azure.activedirectory", value = "tenant-id")
@PropertySource("classpath:/aad-oauth2-common.properties")
@EnableConfigurationProperties({AADAuthenticationProperties.class, ServiceEndpointsProperties.class})
public class AADOAuth2AutoConfiguration {
    private final AADAuthenticationProperties aadAuthProps;
    private final ServiceEndpointsProperties serviceEndpointsProps;

    public AADOAuth2AutoConfiguration(AADAuthenticationProperties aadAuthProperties,
                                      ServiceEndpointsProperties serviceEndpointsProps) {
        this.aadAuthProps = aadAuthProperties;
        this.serviceEndpointsProps = serviceEndpointsProps;
    }

    @Bean
    @ConditionalOnProperty(prefix = "azure.activedirectory", value = "active-directory-groups")
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(AzureADGraphClient graphClient) {
        return new AADOAuth2UserService(aadAuthProps, graphClient);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @ConditionalOnMissingBean(AADGraphHttpClient.class)
    public AADGraphHttpClient aadHttpClient() {
        return new AADGraphHttpClientDefaultImpl(serviceEndpointsProps.getServiceEndpoints(
                aadAuthProps.getEnvironment()));
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @ConditionalOnMissingBean
    public AzureADGraphClient azureADGraphClient(AADGraphHttpClient aaaHttpClient) {
        return new AzureADGraphClient(new ClientCredential(aadAuthProps.getClientId(), aadAuthProps.getClientSecret()),
                aadAuthProps, serviceEndpointsProps, aaaHttpClient);
    }


}
