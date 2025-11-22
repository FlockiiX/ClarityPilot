import json.scanner
from flask import Flask, redirect, request, jsonify
import os
import json
import requests
import hmac
import hashlib
import glob
import sys

import github_parser


# Rabbit MQ
import pika


import logging

app = Flask(__name__)
app.logger.setLevel(logging.DEBUG)


@app.route("/user/1/widget/android", methods=["GET"])
def widget_android():
    return json.dumps(
        {
            "elements": [
                {
                    "type": "row",
                    "columns": [
                        {
                            "type": "text",
                            "content": "Never gonna give",
                            "size": 14,
                            "color": "#888888",
                            "isBold": True,
                        },
                        {
                            "type": "text",
                            "content": "you",
                            "size": 14,
                            "color": "#888888",
                            "isBold": True,
                        },
                        {
                            "type": "text",
                            "content": "LIVE",
                            "size": 12,
                            "color": "up",
                            "align": "right",
                        },
                    ],
                }
            ],
        }
    )


@app.route("/")
def hello():
    return "Hello from the Flask Fake Backend!"


@app.route("/gh/webhooks", methods=["POST"])
def github_webhooks():
    signature_header = request.headers.get("X-Hub-Signature-256")
    if not signature_header:
        return "Forbidden", 403

    signature = signature_header.split("=")[1]

    mac = hmac.new("test".encode(), msg=request.data, digestmod=hashlib.sha256)

    if not hmac.compare_digest(mac.hexdigest(), signature):

        return "Forbiddden", 403

    event = request.headers.get("X-GitHub-Event")
    payload = request.json

    app.logger.info(f"Received GitHub webhook event: {event}")

    # Dump payload into rabitmq
    connection = pika.BlockingConnection(
        pika.ConnectionParameters(
            host="34.32.62.187",
            port=5672,
            credentials=pika.credentials.PlainCredentials("user", "passwordadhahsd7"),
        )
    )

    channel = connection.channel()
    channel.queue_declare(queue="task_queue", durable=True)

    channel.basic_publish(
        exchange="",
        routing_key="task_queue",
        body=json.dumps(github_parser.convert_github_payload(payload)).encode("utf-8"),
        properties=pika.BasicProperties(delivery_mode=1),
    )

    connection.close()

    app.logger.debug(f"Pushed into rabbit")

    return jsonify({"status": "ok"}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
    connection.close()
