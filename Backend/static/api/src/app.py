from flask import Flask, request, jsonify
from pymongo import MongoClient, DESCENDING, ASCENDING
from typing import TypedDict, List

import json
import hmac
import hashlib
import pika

import logging
from datetime import datetime, timezone, timedelta

import github_parser

MAPPED_ICONS = {
    "": "https://avatars.githubusercontent.com/u/109746326?s=48&v=4",
    "github": "https://cdn-icons-png.flaticon.com/512/3291/3291695.png",
    "slack": "https://cdn-icons-png.flaticon.com/512/3800/3800024.png",
    "teams": "https://cdn-icons-png.flaticon.com/512/15047/15047490.png",
    "jira": "https://cdn-icons-png.flaticon.com/512/5968/5968875.png",
}

MAPPED_NAMES = {
    "": "",
    "github": "GitHub",
    "slack": "Slack",
    "teams": "Teams",
    "jira": "Jira",
}

ICON_SIZES = {"slack": 26, "teams": 26, "jira": 26}


class TimelineMetadata(TypedDict):
    subtask: str
    project: str
    other: str


class TimelineEntry(TypedDict):
    _id: str
    type: str
    timestamp_start: str
    timestamp_end: str
    punchline: str
    metadata: TimelineMetadata


# Mongo Setup
mongoClient = MongoClient("mongodb://root:jsdusdbabsduroo4t@34.32.62.187:27017/")
db = mongoClient["clarity"]
collection = db["tracker"]


app = Flask(__name__)
app.logger.setLevel(logging.DEBUG)


# Android timeline
@app.route("/user/1/timeline/android", methods=["GET"])
def timeline_android():
    # Get current time and 7 days ago in ms
    now = datetime.now(timezone.utc)
    seven_days_ago = now - timedelta(days=7)
    seven_days_ago_ms = int(seven_days_ago.timestamp() * 1000)

    # Query MongoDB for entries in the last 7 days, sorted by start timestamp desc
    cursor = collection.find({"timestamp_start": {"$gte": seven_days_ago_ms}}).sort(
        "timestamp_end", DESCENDING
    )

    entries: List[TimelineEntry] = list(cursor)  # type: ignore[assignment]

    # Helper to convert ms -> date label
    def day_label(ts_ms: int) -> str:
        dt = datetime.fromtimestamp(ts_ms / 1000.0, tz=timezone.utc).astimezone()
        today = now.astimezone().date()
        entry_day = dt.date()
        if entry_day == today:
            return "Today"
        if entry_day == today - timedelta(days=1):
            return "Yesterday"
        return dt.strftime("%Y-%m-%d")

    # Helper to format duration between start and end ms
    def format_duration(start_ms: int, end_ms: int) -> str:
        seconds = max(0, int((end_ms - start_ms) / 1000))
        hours, rem = divmod(seconds, 3600)
        minutes, _ = divmod(rem, 60)
        if hours and minutes:
            return f"{hours}h {minutes}m"
        if hours:
            return f"{hours}h"
        if minutes:
            return f"{minutes}m"
        return "0m"

    grouped: dict[str, list[dict]] = {}

    for e in entries:
        ts_start = int(e.get("timestamp_start", 0))
        ts_end = int(e.get("timestamp_end", ts_start))
        label = day_label(ts_start)

        item = {
            "type": MAPPED_NAMES[e.get("type", "")],
            "icon": MAPPED_ICONS[e.get("type", "")],
            "label": e.get("punchline", ""),
            "duration": format_duration(ts_start, ts_end),
        }

        if ICON_SIZES.get(e.get("type", "")) != None:
            item["iconSize"] = ICON_SIZES[e.get("type", "")]

        grouped.setdefault(label, []).append(item)

    return json.dumps(grouped)


# Android widget
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


# Debugging
@app.route("/")
def hello():
    return "online"


# Input Handling
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
