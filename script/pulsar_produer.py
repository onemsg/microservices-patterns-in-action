import pulsar
import json
import logging
import time

pulsar_logger = logging.Logger("pulsar-logger", level=logging.ERROR)
client = pulsar.Client('pulsar://172.29.193.164:6650', logger=pulsar_logger)

producer = client.create_producer("create-order-saga-reply-channel", "python-test-produer")

message = {
    "name": "CONSUMER_VERIFIED_FAILED",
    "orderId": "8"
}

def to_bytes(message):
    return json.dumps(message).encode()

producer.send(to_bytes(message))

print(f"Send {message} to {producer.topic()}")

client.close()