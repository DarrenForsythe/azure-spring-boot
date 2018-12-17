/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.aad;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;

import javax.naming.ServiceUnavailableException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class AADOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private static final String INVALID_REQUEST = "invalid_request";
    private static final String SERVER_ERROR = "server_error";
    private static final String DEFAULT_USERNAME_ATTR_NAME = "name";

    private final AADAuthenticationProperties aadAuthProps;
    private final ServiceEndpointsProperties serviceEndpointsProps;
    private final AzureADGraphClient graphClient;

    public AADOAuth2UserService(AADAuthenticationProperties aadAuthProps,
                                ServiceEndpointsProperties serviceEndpointsProps, AzureADGraphClient graphClient) {
        this.aadAuthProps = aadAuthProps;
        this.serviceEndpointsProps = serviceEndpointsProps;
        this.graphClient = graphClient;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        final OidcUserService delegate = new OidcUserService();

        // Delegate to the default implementation for loading a user
        OidcUser oidcUser = delegate.loadUser(userRequest);
        final OidcIdToken idToken = userRequest.getIdToken();

        final String graphApiToken;
        final Set<GrantedAuthority> mappedAuthorities;

        try {
       

            graphApiToken = graphClient.acquireTokenForGraphApi(idToken.getTokenValue(),
                    aadAuthProps.getTenantId()).getAccessToken();

            mappedAuthorities = graphClient.getGrantedAuthorities(graphApiToken);
        } catch (MalformedURLException e) {
            throw wrapException(INVALID_REQUEST, "Failed to acquire token for Graph API.", e);
        } catch (ServiceUnavailableException | InterruptedException | ExecutionException e) {
            throw wrapException(SERVER_ERROR, "Failed to acquire token for Graph API.", e);
        } catch (IOException e) {
            throw wrapException(SERVER_ERROR, "Failed to map group to authorities.", e);
        } catch (AADGraphHttpClientException e) {
            throw wrapException(SERVER_ERROR, "Failed to get Membership for user.", e);
        }

        // Create a copy of oidcUser but use the mappedAuthorities instead
        oidcUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), getUserNameAttrName(userRequest));

        return oidcUser;
    }

    private OAuth2AuthenticationException wrapException(String errorCode, String errDesc, Exception e) {
        final OAuth2Error oAuth2Error = new OAuth2Error(errorCode, errDesc, null);
        throw new OAuth2AuthenticationException(oAuth2Error, e);
    }

    private String getUserNameAttrName(OAuth2UserRequest userRequest) {
        String userNameAttrName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        if (StringUtils.isEmpty(userNameAttrName)) {
            userNameAttrName = DEFAULT_USERNAME_ATTR_NAME;
        }

        return userNameAttrName;
    }
}
