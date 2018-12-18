/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.autoconfigure.aad;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Default implementation of a {@link AADGraphHttpClient}.
 */
@RequiredArgsConstructor
@Slf4j
public class AADGraphHttpClientDefaultImpl implements AADGraphHttpClient {

    private final ServiceEndpoints serviceEndpoints;

    @Override
    public String getMemberships(String accessToken) throws AADGraphHttpClientException {
        HttpURLConnection conn = null;
        try {
            final URL url = new URL(serviceEndpoints.getAadMembershipRestUri());
            conn = (HttpURLConnection) url.openConnection();
            // Set the appropriate header fields in the request header.
            conn.setRequestProperty("api-version", "1.6");
            conn.setRequestProperty("Authorization", accessToken);
            conn.setRequestProperty("Accept", "application/json;odata=minimalmetadata");
            final String responseInJson = getResponseStringFromConn(conn);
            final int responseCode = conn.getResponseCode();
            if (responseCode == HttpStatus.OK.value()) {
                return responseInJson;
            } else {
                log.error("Response code was not 200. Got - {}", responseCode);
                throw new AADGraphHttpClientException("Response is not " + HttpStatus.OK.value() +
                        ", response json: " + responseInJson);
            }
        } catch (IOException e) {
            throw new AADGraphHttpClientException("Failed to execute HTTP Call to Membership REST URI", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String getResponseStringFromConn(HttpURLConnection conn) throws AADGraphHttpClientException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            return buildString(reader);
        } catch (IOException e) {
            try (BufferedReader errorStream = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                final String error = buildString(errorStream);
                log.error("Could not read response from Membership URI  - Error Response - {}", error, e);
                throw new AADGraphHttpClientException("Failed to read response from Membership call", e);
            } catch (IOException errorStreamException) {
                throw new AADGraphHttpClientException("Failed to read Error Stream from Membership call",
                        errorStreamException);
            }
        }
    }

    private String buildString(BufferedReader errorStream) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = errorStream.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }
}