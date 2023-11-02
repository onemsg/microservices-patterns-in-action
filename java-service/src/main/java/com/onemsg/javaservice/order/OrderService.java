package com.onemsg.javaservice.order;

import static com.onemsg.javaservice.event.CreateOrderSagaResult.Type.CARD_AUTHORIZED;
import static com.onemsg.javaservice.event.CreateOrderSagaResult.Type.CARD_AUTHORIZED_FAILED;
import static com.onemsg.javaservice.event.CreateOrderSagaResult.Type.CONSUMER_VERIFIED;
import static com.onemsg.javaservice.event.CreateOrderSagaResult.Type.CONSUMER_VERIFIED_FAILED;
import static com.onemsg.javaservice.event.CreateOrderSagaResult.Type.TICKET_CREATED;
import static com.onemsg.javaservice.event.CreateOrderSagaResult.Type.TICKET_CREATED_FAILED;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.impl.schema.JSONSchema;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.onemsg.javaservice.event.AccountingCommand;
import com.onemsg.javaservice.event.ConsumerCommand;
import com.onemsg.javaservice.event.CreateOrderSagaResult;
import com.onemsg.javaservice.event.KitchenCommand;
import com.onemsg.javaservice.event.OrderCommand;
import com.onemsg.javaservice.exception.OrderAccessDeniedException;
import com.onemsg.javaservice.exception.OrderNotExistedException;
import com.onemsg.javaservice.exception.OrderStateModifyException;
import com.onemsg.javaservice.order.model.Order;
import com.onemsg.javaservice.order.model.OrderCreationRequest;

import lombok.extern.slf4j.Slf4j;


@ConditionalOnProperty(name = "app.use.pulsar", havingValue = "true")
@Slf4j
@Service
public class OrderService implements DisposableBean {

    private final Map<Long, Order> orderRepository = new HashMap<>();
    private final Map<Long, Long> ticketRepository = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    private final Producer<OrderCommand> orderServiceCommandProducer;
    private final Producer<ConsumerCommand> consumerServiceCommandProducer;
    private final Producer<KitchenCommand> kitchenServiceCommandProducer;
    private final Producer<AccountingCommand> accountingServiceCommandProducer;
    private final Consumer<CreateOrderSagaResult> createOrderSageResultConsumer;

    private final TaskExecutor executor;

    private final AtomicBoolean start = new AtomicBoolean(false);

    public OrderService(PulsarClient pulsarClient, @Qualifier("pulsarHandleExecutor") TaskExecutor executor) throws Exception {
        orderServiceCommandProducer = pulsarClient.newProducer(JSONSchema.of(OrderCommand.class))
                .producerName("Order-Service")
                .topic("order-service-command-channel")
                .sendTimeout(5, TimeUnit.SECONDS)
                .create();

        consumerServiceCommandProducer = pulsarClient.newProducer(JSONSchema.of(ConsumerCommand.class))
                .producerName("Order-Service")
                .topic("consumer-service-command-channel")
                .sendTimeout(5, TimeUnit.SECONDS)
                .create();

        kitchenServiceCommandProducer = pulsarClient.newProducer(JSONSchema.of(KitchenCommand.class))
                .producerName("Order-Service")
                .topic("kitchen-service-command-channel")
                .sendTimeout(5, TimeUnit.SECONDS)
                .create();

        accountingServiceCommandProducer = pulsarClient.newProducer(JSONSchema.of(AccountingCommand.class))
                .producerName("Order-Service")
                .topic("accounting-service-command-channel")
                .sendTimeout(5, TimeUnit.SECONDS)
                .create();

        createOrderSageResultConsumer = pulsarClient.newConsumer(JSONSchema.of(CreateOrderSagaResult.class))
            .topic("create-order-saga-reply-channel")
            .consumerName("Order-Service")
            .subscriptionMode(SubscriptionMode.Durable)
            .subscriptionType(SubscriptionType.Shared)
            .subscriptionName("Order-Service-Subscription")
            .subscriptionInitialPosition(SubscriptionInitialPosition.Latest)
            .subscribe();

        createOrderSageResultConsumer.seek(System.currentTimeMillis());
        
        this.executor = executor;
        start.set(true);
        this.executor.execute(this::schedule);

    }

    public long create(OrderCreationRequest orderCreationRequest) {
        w.lock();
        try {
            Order order = newOrder(orderCreationRequest);
            orderRepository.put(order.getId(), order);
            log.info("Create Order {}", order);
            executor.execute(() -> setupCreateOrderSaga(order));
            return order.getId();
        } finally {
            w.unlock();
        }
    }

    public void cancel(long orderId, long consumerId) throws OrderNotExistedException,
            OrderAccessDeniedException, OrderStateModifyException {
        w.lock();
        try {
            var order = orderRepository.get(orderId);
            if (order == null) {
                throw new OrderNotExistedException(orderId);
            }
            if (order.getConsumerId() != consumerId) {
                throw new OrderAccessDeniedException(orderId);
            }
            if (!order.canCancel()) {
                throw new OrderStateModifyException(orderId, order.getState().name(), Order.State.CANCEL.name());
            }
            order.setState(Order.State.CANCEL);
            log.info("Cancel order {}", orderId);
        } finally {
            w.unlock();
        }
    }

    public void approveOrder(long orderId) {
        w.lock();
        try {
            var order = orderRepository.get(orderId);
            if (order == null) return;
            if (order.canApproved()) {
                order.setState(Order.State.APPROVED);
                log.info("Approve order {}", orderId);
            } else {
                log.warn("Cannot approve order {} with {}", orderId, order.getState());
            }
        } finally {
            w.unlock();
        }
    }

    public void rejectOrder(long orderId) {
        w.lock();
        try {
            var order = orderRepository.get(orderId);
            if (order == null)
                return;
            if (order.canReject()) {
                order.setState(Order.State.REJECT);
                log.info("Reject order {}", orderId);
            } else {
                log.warn("Cannot reject order {} with {}", orderId, order.getState());
            }
        } finally {
            w.unlock();
        }
    }

    private void setupCreateOrderSaga(Order order) {
        try {
            var command = ConsumerCommand.verifyConsumer(order.getConsumerId(), order.getId());
            consumerServiceCommandProducer.send(command);
            log.info("Setup CreateOrderSaga {}", order.getId());
        } catch (Exception e) {
            log.warn("Failed to setup CreateOrderSaga {}", order.getId());
        }
    }

    public Order get(long orderId, long consumerId) throws OrderNotExistedException, OrderAccessDeniedException {
        r.lock();
        try {
            var order = orderRepository.get(orderId);
            if (order == null) {
                throw new OrderNotExistedException(orderId);
            }
            if (order.getConsumerId() != consumerId) {
                throw new OrderAccessDeniedException(orderId);
            }
            return order;
        } finally {
            r.unlock();
        }
    }

    public Order getInternal(long orderId){
        r.lock();
        try {
            return orderRepository.get(orderId);
        } finally {
            r.unlock();
        }
    }

    public List<Order> getAll(long consumerId) {
        r.lock();
        try {
            return orderRepository.values().stream()
                .filter(o -> o.getConsumerId() == consumerId)
                .sorted(Comparator.comparingLong(Order::getId).reversed())
                .toList();
        } finally {
            r.unlock();
        }
    }

    public List<Order> getAll() {
        r.lock();
        try {
            return orderRepository.values().stream()
                    .sorted(Comparator.comparingLong(Order::getId).reversed())
                    .toList();
        } finally {
            r.unlock();
        }
    }

    private void schedule() {
        log.info("Start schedule");
        while (start.get()) {
            try {
                Message<CreateOrderSagaResult> message = createOrderSageResultConsumer.receive();
                executor.execute(() -> handleCreateOrderSagaResult(message));
            } catch (PulsarClientException e) {
                log.warn("Receive message from {} failed {}", createOrderSageResultConsumer.getTopic(), e.toString());
            }
        }
        log.info("Stop schedule");
    }


    private void handleCreateOrderSagaResult(Message<CreateOrderSagaResult> message) {
        try {
            var result = message.getValue();
            var name = result.getName();
            log.info("Handle saga result {} {}", name, result.getOrderId());
            if (CONSUMER_VERIFIED.match(name)) {
                var order = getInternal(result.getOrderId());
                if (order == null) return;
                var command = KitchenCommand.createTicket(order.getRestaurantId(), order.getId());
                kitchenServiceCommandProducer.send(command);
            } else if (CONSUMER_VERIFIED_FAILED.match(name)) {
                rejectOrder(result.getOrderId());
            } else if (TICKET_CREATED.match(name)) {
                var order = getInternal(result.getOrderId());
                if (order == null) return;
                long ticketId = result.getTicketId();
                ticketRepository.put(order.getId(), ticketId);
                var command = AccountingCommand.authorizedCard(order.getConsumerId(), order.getTotalMoney(), order.getId());
                accountingServiceCommandProducer.send(command);
                log.info("Handle saga reult {} {} {}", name, result.getOrderId(), ticketId);
            } else if (TICKET_CREATED_FAILED.match(name)) {
                rejectOrder(result.getOrderId());
            } else if (CARD_AUTHORIZED.match(name)) {
                var order = getInternal(result.getOrderId());
                if (order == null) return;
                Long ticketId = ticketRepository.get(order.getId());
                if (ticketId == null) return;
                var command1 = KitchenCommand.approveTicket(ticketId, order.getId());
                kitchenServiceCommandProducer.send(command1);
                approveOrder(order.getId());
            } else if (CARD_AUTHORIZED_FAILED.match(name)) {
                var order = getInternal(result.getOrderId());
                if (order == null) return;
                Long ticketId = ticketRepository.get(order.getId());
                if (ticketId == null) return;
                var command1 = KitchenCommand.rejectTicket(ticketId, order.getId());
                kitchenServiceCommandProducer.send(command1);
                rejectOrder(order.getId());
            }
        } catch (Exception e) {
            log.error("Failed to handle message {} {}", message.getMessageId(), e);
            reconsumeLater(message);
        }
    }

    private void reconsumeLater(Message<CreateOrderSagaResult> message) {
        try {
            createOrderSageResultConsumer.reconsumeLater(message, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to reconsumeLater {}", message.getMessageId());
        }
    }

    private static final AtomicLong idGenerator = new AtomicLong(1);

    private Order newOrder(OrderCreationRequest orderCreationRequest) {
        Order order = new Order();
        order.setId(nextId());
        order.setConsumerId(orderCreationRequest.getConsumerId());
        order.setFoods(orderCreationRequest.getFoods());
        order.setRestaurantId(orderCreationRequest.getRestaurantId());
        order.setCreatedTime(LocalDateTime.now());
        order.setLastUpdatedTime(order.getCreatedTime());
        order.setState(Order.State.APPROVAL_PENDING);
        order.setTotalMoney(getTotalMoney(order));
        return order;
    }

    public int getTotalMoney(Order order) {
        return (int) order.getFoods().stream().mapToLong(Long::longValue).sum();
    }

    private long nextId() {
        return idGenerator.getAndIncrement();
    }

    @Override
    public void destroy() throws Exception {
        start.set(false);
    }

}
