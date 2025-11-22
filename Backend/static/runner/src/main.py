import pika


def callback(ch, method, properties, body):
    print("Received:", body.decode())
    ch.basic_ack(delivery_tag=method.delivery_tag)


connection = pika.BlockingConnection(
    pika.ConnectionParameters(
        host="34.32.62.187",
        port=5672,
        credentials=pika.credentials.PlainCredentials("user", "passwordadhahsd7"),
    )
)

channel = connection.channel()

channel.queue_declare(queue="task_queue", durable=True)

# Fair dispatch: give one message at a time
channel.basic_qos(prefetch_count=5)

channel.basic_consume(queue="task_queue", on_message_callback=callback)

print("Waiting for messages...")
channel.start_consuming()
