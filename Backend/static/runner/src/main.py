import pika
import json

from pymongo import MongoClient, DESCENDING
from typing import TypedDict, Optional


class TimelineMetadata(TypedDict):
    subtask: str
    project: str
    other: str


class TimelineEntry(TypedDict):
    _id: str
    type: str
    icon: str
    timestamp_start: str
    timestamp_end: str
    punchline: str
    metadata: TimelineMetadata


class RabbitMetadata(TypedDict):
    commit_msg: str
    repo_name: str
    ref: str
    ticket_id: Optional[str]
    channel: Optional[str]


class RabbitMessage(TypedDict):
    _id: str
    user_identifier: str
    provider: str
    activity_type: str
    timestamp: str
    metadata: RabbitMetadata


def callback(ch, method, properties, body):
    print("Received:", body.decode())
    jsonBody: RabbitMessage = json.loads(body.decode())

    query = {"type": RabbitMessage["provider"]}
    results = collection.find(query).sort("_id", DESCENDING).limit(5).to_list()

    # Throw into ai

    ch.basic_ack(delivery_tag=method.delivery_tag)


connection = pika.BlockingConnection(
    pika.ConnectionParameters(
        host="34.32.62.187",
        port=5672,
        credentials=pika.credentials.PlainCredentials("user", "passwordadhahsd7"),
    )
)


# Setup mongo
client = MongoClient("mongodb://root:jsdusdbabsduroo4t@34.32.62.187:27017/")
db = client["clarity"]
collection = db["tracker"]


# Setup rabbit
channel = connection.channel()

channel.queue_declare(queue="task_queue", durable=True)

channel.basic_qos(prefetch_count=5)
channel.basic_consume(queue="task_queue", on_message_callback=callback)

print("Waiting for messages...")
channel.start_consuming()
