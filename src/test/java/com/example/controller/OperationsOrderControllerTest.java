package com.example.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.service.OperationsOrderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class OperationsOrderControllerTest {

    private final OperationsOrderController controller =
            new OperationsOrderController(mock(OperationsOrderService.class));

    @Test
    void rejectsPageAndSizeValuesOutsideSupportedRanges() {
        assertBadRequest(10_000_001, 50, null);
        assertBadRequest(0, 101, null);
        assertBadRequest(0, 0, null);
    }

    @Test
    void rejectsSwaggerJsonPlaceholderAsSortField() {
        assertBadRequest(0, 50, List.of("[\"string\"]"));
    }

    private void assertBadRequest(int page, int size, List<String> sort) {
        assertThatThrownBy(() -> controller.searchOrders(
                null, null, null, null, null, null, page, size, sort))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}