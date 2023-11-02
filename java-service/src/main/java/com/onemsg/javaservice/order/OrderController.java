package com.onemsg.javaservice.order;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onemsg.javaservice.order.model.OrderCreationRequest;

@ConditionalOnProperty(name = "app.use.pulsar", havingValue = "true")
@RestController
@RequestMapping("/api/order")
public class OrderController {
    
    @Autowired
    private OrderService orderService;

    @PostMapping()
    public ResponseEntity<Object> createOrder(@RequestBody OrderCreationRequest orderCreationRequest, 
            @RequestHeader("X-Auth-UserId") long userId) {

        orderCreationRequest.setConsumerId(userId);
        long id = orderService.create(orderCreationRequest);
        return ResponseEntity.created(buildGetLocation(id)).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getOrder(@PathVariable long id,
            @RequestHeader("X-Auth-UserId") long userId) {
        var order = orderService.get(id,userId);
        return ResponseEntity.ofNullable(order);
    }

    @GetMapping()
    public ResponseEntity<Object> getOrder(@RequestHeader("X-Auth-UserId") long userId) {
        var list = orderService.getAll(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping(headers = {"X-Auth-Admin=123456789"})
    public ResponseEntity<Object> getOrder() {
        var list = orderService.getAll();
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Object> cancelOrder(@PathVariable long id,
            @RequestHeader("X-Auth-UserId") long userId) {
        orderService.cancel(id, userId);
        return ResponseEntity.ok().build();
    }


    private static URI buildGetLocation(long id) {
        return URI.create("/api/order/" + id);
    }

}
