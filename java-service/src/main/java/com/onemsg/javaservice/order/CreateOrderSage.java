package com.onemsg.javaservice.order;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Producer;

import com.onemsg.javaservice.event.AccountingCommand;
import com.onemsg.javaservice.event.ConsumerCommand;
import com.onemsg.javaservice.event.CreateOrderSagaResult;
import com.onemsg.javaservice.event.KitchenCommand;
import com.onemsg.javaservice.event.OrderCommand;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CreateOrderSage {
    
    private final Producer<OrderCommand> orderServiceCommandProducer;
    private final Producer<ConsumerCommand> consumerServiceCommandProducer;
    private final Producer<KitchenCommand> kitchenServiceCommandProducer;
    private final Producer<AccountingCommand> accountingServiceCommandProducer;
    private final Consumer<CreateOrderSagaResult> createOrderSageResultConsumer;
    

    public void setup() {
        // createOrderSageResultConsumer.receive();


    }
}
