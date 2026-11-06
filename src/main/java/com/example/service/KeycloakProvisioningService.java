package com.example.service;

import com.example.dto.CustomerRegistrationRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KeycloakProvisioningService {

    private final RestClient restClient;
    private final String baseUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakProvisioningService(
            RestClient.Builder restClientBuilder,
            @Value("${app.keycloak.base-url:http://localhost:8081}") String baseUrl,
            @Value("${app.keycloak.realm:dnd}") String realm,
            @Value("${app.keycloak.provisioner-client-id:}") String clientId,
            @Value("${app.keycloak.provisioner-client-secret:}") String clientSecret) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String createCustomer(CustomerRegistrationRequest request, String phoneNumber) {
        String adminToken = getAdminToken();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("username", phoneNumber);
        user.put("enabled", true);
        user.put("firstName", request.firstName().trim());
        user.put("lastName", request.lastName().trim());
        user.put("email", request.email().trim());
        user.put("attributes", Map.of("phone_number", List.of(phoneNumber)));
        user.put("credentials", List.of(Map.of(
                "type", "password", "value", request.password(), "temporary", false)));

        try {
            URI location = restClient.post()
                    .uri(adminUrl("/users"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();
            return userIdFromLocation(location);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this phone number already exists");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider could not create the account");
        }
    }

    public void deleteCustomer(String keycloakUserId) {
        String adminToken = getAdminToken();
        restClient.delete()
                .uri(adminUrl("/users/{id}"), keycloakUserId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity();
    }

    private String getAdminToken() {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Keycloak customer provisioning is not configured");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        try {
            TokenResponse response = restClient.post()
                    .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Identity provider returned no provisioning token");
            }
            return response.accessToken();
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Keycloak provisioning credentials were rejected");
        }
    }

    private String adminUrl(String path) {
        return baseUrl + "/admin/realms/" + realm + path;
    }

    private String userIdFromLocation(URI location) {
        if (location == null || location.getPath() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider returned no account identifier");
        }
        String path = location.getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);
        if (userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider returned no account identifier");
        }
        return userId;
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }
}