from flask import Flask, redirect, request, jsonify
import os
import json
import requests
import hmac
import hashlib
import glob
import sys

import logging

app = Flask(__name__)
app.logger.setLevel(logging.DEBUG)


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

    app.logger.error(f"Received GitHub webhook event: {event}")

    # You can process the payload here
    app.logger.info(payload)

    return jsonify({"status": "ok"}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
