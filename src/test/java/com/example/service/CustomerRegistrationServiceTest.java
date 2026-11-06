package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.dto.CustomerRegistrationRequest;
import com.example.entity.Customer;
import com.example.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CustomerRegistrationServiceTest {

    private CustomerRepository customerRepository;
    private KeycloakProvisioningService keycloakProvisioningService;
    private CustomerRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        keycloakProvisioningService = mock(KeycloakProvisioningService.class);
        registrationService = new CustomerRegistrationService(
                customerRepository, keycloakProvisioningService, true);
    }

        @Test
        void createsKeycloakIdentityAndCustomerProfileWithPasswordSignup() {
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                                "  Test  ", "Customer  ", "9000000001", " test@example.test ", "StrongPass!234");
        when(customerRepository.existsByPhoneNumber("+919000000001")).thenReturn(false);
        when(keycloakProvisioningService.createCustomer(request, "+919000000001")).thenReturn("keycloak-subject");
        when(customerRepository.saveAndFlush(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = registrationService.register(request);

        assertThat(result.displayName()).isEqualTo("Test Customer");
        assertThat(result.email()).isEqualTo("test@example.test");
        assertThat(result.phoneNumber()).isEqualTo("+919000000001");
        verify(keycloakProvisioningService).createCustomer(request, "+919000000001");
        verify(customerRepository).saveAndFlush(any(Customer.class));
    }

    @Test
    void rejectsAnExistingPhoneBeforeCreatingKeycloakIdentity() {
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "Test", "Customer", "9000000001", "test@example.test", "StrongPass!234");
        when(customerRepository.existsByPhoneNumber("+919000000001")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(keycloakProvisioningService, never()).createCustomer(any(), any());
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void unverifiedPhoneSignupIsDisabledOutsideTheLocalProfile() {
        registrationService = new CustomerRegistrationService(customerRepository, keycloakProvisioningService, false);
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                "Test", "Customer", "9000000001", "test@example.test", "StrongPass!234");

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(customerRepository, never()).existsByPhoneNumber(any());
        verify(keycloakProvisioningService, never()).createCustomer(any(), any());
    }
}