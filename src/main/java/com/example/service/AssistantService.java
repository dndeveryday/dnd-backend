package com.example.service;

import com.example.entity.Customer;
import com.example.entity.CustomerOrder;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AssistantService {

    private static final Pattern ORDER_QUERY =
            Pattern.compile("\\b(order|delivery|tracking|track|status|where)\\b", Pattern.CASE_INSENSITIVE);

    private final OrderService orderService;

    public AssistantService(OrderService orderService) {
        this.orderService = orderService;
    }

    public String reply(Customer customer, String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (ORDER_QUERY.matcher(message).find()) {
            var orders = orderService.findOrders(customer);

            CustomerOrder matchedOrder = orders.stream()
                    .filter(order -> normalized.contains(order.getOrderNumber().toLowerCase(Locale.ROOT))
                            || normalized.contains(order.getId().toString().toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .orElse(orders.size() == 1 ? orders.getFirst() : null);

            if (matchedOrder != null) {
                return "Order " + matchedOrder.getOrderNumber() + " is currently "
                        + matchedOrder.getStatus().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".";
            }
            if (orders.isEmpty()) {
                return "I couldn't find any orders on your account. Please contact support if you think this is a mistake.";
            }
            return "Please include your order number so I can check its latest status.";
        }
        return "I can check order status here. For other questions or help from a person, please create a support ticket.";
    }
}
