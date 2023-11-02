import pulsar
from pulsar import InitialPosition
import logging
import time

pulsar_logger = logging.Logger("pulsar-logger", level=logging.WARNING)
client = pulsar.Client('pulsar://172.29.193.164:6650', logger=pulsar_logger)


topics = [
    "order-service-command-channel", 
    "consumer-service-command-channel",
    "kitchen-service-command-channel",
    "accounting-service-command-channel",
    "create-order-saga-reply-channel",
    ]

consumer = client.subscribe(topics, 'python_test_subscription', initial_position=InitialPosition.Earliest)

current_time = int(time.time() * 1000)
consumer.seek(current_time)

f = open("tmp/message.csv", "w", encoding="utf-8")

while True:
    
    msg = consumer.receive()
    try:
        info = f"{msg.topic_name()}, {msg.data().decode()}"
        print(f"Received message : {info}")
        # Acknowledge successful processing of the message
        f.write(info)
        f.write("\n")
        f.flush()
        consumer.acknowledge(msg)
    except Exception:
        # Message failed to be processed
        consumer.negative_acknowledge(msg)