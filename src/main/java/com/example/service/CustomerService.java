package com.example.service;

import com.example.dto.CustomerResponse;
import com.example.entity.Customer;
import com.example.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer requireCustomer(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token is missing a subject");
        }

        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        if (subject.length() > 255
                || (email != null && email.length() > 320)
                || (name != null && name.length() > 160)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access token profile claims are too long");
        }
        return customerRepository.findByAuthSubject(subject)
                .map(customer -> {
                    if (!java.util.Objects.equals(customer.getEmail(), email)
                            || !java.util.Objects.equals(customer.getDisplayName(), name)) {
                        customer.updateProfile(email, name);
                    }
                    return customer;
                })
                .orElseGet(() -> customerRepository.save(new Customer(subject, email, name)));
    }

    public CustomerResponse getProfile(Jwt jwt) {
        Customer customer = requireCustomer(jwt);
        return new CustomerResponse(
                customer.getId(), customer.getEmail(), customer.getDisplayName(), customer.getCreatedAt());
    }
}
