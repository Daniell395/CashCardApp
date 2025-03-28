package com.example.cashcard.utils;

import org.springframework.http.*;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;


public class TestRestClient {

    private final TestRestTemplate restTemplate;
    private final String baseUrl;

    public TestRestClient(TestRestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public <T> ResponseEntity<String> makeGetRequest(String url, String username, String password, HttpStatus expectedStatus) {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth(username, password)
                .getForEntity(url, String.class);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        return response;
    }

    public <T> ResponseEntity<Void> makePostRequest(String url, T body, String username, String password, HttpStatus expectedStatus) {
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth(username, password)
                .postForEntity(url, body, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        return response;
    }

    public <T> void makePutRequest(String url, T body, String username, String password, HttpStatus expectedStatus) {
        HttpEntity<T> request = new HttpEntity<>(body);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth(username, password)
                .exchange(baseUrl + url, HttpMethod.PUT, request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    }

    public void makeDeleteRequest(String url, String username, String password, HttpStatus expectedStatus) {

        ResponseEntity<Void> deleteResponse = restTemplate
                .withBasicAuth(username, password)
                .exchange(url, HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(expectedStatus);
    }
}
