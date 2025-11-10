package com.liverpool.imageValidator.utils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


public class RestTemplateUrl {

    public static ResponseEntity<String> callMethod(String url, String username, String password) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");

        headers.setBasicAuth(Util.decode(username), Util.decode(password));

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );
    }

}
