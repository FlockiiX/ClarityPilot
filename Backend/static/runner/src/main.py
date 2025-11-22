import pika
import json

from pymongo import MongoClient, DESCENDING
from typing import TypedDict, Optional, List
import time

from google import genai
from google.genai import types

LLMClient = genai.Client(api_key="AIzaSyAQxwUDf8mZcrsiGmyR97suGQn4el_XW0M")


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


class RabbitMetadata(TypedDict):
    commit_msg: str
    repo_name: str
    ref: str
    ticket_id: Optional[str]
    channel: Optional[str]


class RabbitMessage(TypedDict):
    _id: str
    provider: str
    activity_type: str
    raw_payload: str
    timestamp: str
    metadata: RabbitMetadata


def callback(ch, method, properties, body):
    parsedBody: RabbitMessage = json.loads(body.decode())
    del parsedBody["raw_payload"]

    query = {"type": parsedBody["provider"]}
    results: List[TimelineEntry] = (
        collection.find(query).sort("_id", DESCENDING).limit(5).to_list()
    )

    # Find relevant last tasks
    relevantTasks = []

    thirty_minutes_ago = int(time.time() * 1000) - (
        60 * 60 * 1000 * 6
    )  # Current time in ms - 6 hours in ms

    for result in results:
        if (
            result["timestamp_end"]
            and int(result["timestamp_end"]) > thirty_minutes_ago
        ):
            relevantTasks.append(result)
            break

    # AI magic
    contents = [
        types.Content(
            role="user",
            parts=[
                types.Part.from_text(
                    text="""# System Role
You are an intelligent Activity Classifier and Data Handling Expert. Your goal is to analyze a user's behavior to organize their timeline effectively.

# Objective
Analyze the `Just Completed Task` in the context of the user's `Previous Activities`. Determine if the user is continuing an existing activity or starting a completely new one.

# Decision Logic
1. **Continuing:** If the task shares the same context (e.g., same repository, same project, specific follow-up action) as one of the active previous activities, link it to that activity.
2. **New Activity:** If the task represents a context switch, a different project, or a standalone action unrelated to recent history, create a new activity.

# Output Format
You must output **exclusively** a valid JSON object based on your decision. Do not output markdown formatting (like ```json) or conversational text.

## Schema A: Action is Continuing
Use this if the task belongs to an existing activity.
```json
{
  "action": "continuing",
  "activity_id": <Integer | The _id of the matched previous activity>
}
```

## Schema B: Action is New Activity
Use this if the task is new.
```json
{
  "action": "newActivity",
  "type": "<String | The platform the user has used for the activity - always lower case. E.g., 'github', 'slack'>",
  "punchline": "<String | A concise, punchy summary. E.g., 'Project: iOS Widget Improvements'>",
  "metadata": {
    "key": "<value | Extract relevant grouping tags, e.g., 'repo_name', 'slack_call_channel_name'>"
  }
}
```
Input Data

## Just Completed Task
"""
                    + json.dumps(parsedBody)
                    + """

## Previous Activities
"""
                    + json.dumps(relevantTasks)
                )
            ],
        )
    ]

    response = LLMClient.models.generate_content(
        model="gemini-3-pro-preview", contents=contents
    )
    res = response.text
    parsedRes = json.loads(res)

    if parsedRes["action"] == "continuing":
        print(f"[runner] Decided {parsedBody['provider']} is a continuation")
        # Get the last entry for the given type (provider) for this user
        query = {
            "type": parsedBody["provider"],
            "_id": parsedRes["activity_id"],
        }
        last_entry = collection.find(query).sort("_id", DESCENDING).limit(1)

        last_entry_list = list(last_entry)
        if last_entry_list:
            entry = last_entry_list[0]
            current_ms = int(time.time() * 1000)
            collection.update_one(
                {"_id": entry["_id"]},
                {"$set": {"timestamp_end": str(current_ms)}},
            )
    elif parsedRes["action"] == "newActivity":
        print(f"[runner] Decided {parsedBody['provider']} is a new activity")
        current_ms = int(time.time() * 1000)

        new_entry: TimelineEntry = {
            "_id": str(current_ms),
            "type": parsedBody["provider"],
            "timestamp_start": current_ms - 1000 * 60 * 10,
            "timestamp_end": current_ms,
            "punchline": parsedRes.get("punchline", ""),
            "metadata": {
                "subtask": (
                    parsedBody["metadata"].get("commit_msg", "")
                    if "metadata" in parsedBody
                    else ""
                ),
                "project": (
                    parsedBody["metadata"].get("repo_name", "")
                    if "metadata" in parsedBody
                    else ""
                ),
                "other": json.dumps(parsedRes.get("metadata", {})),
            },
        }

        collection.insert_one(new_entry)

    ch.basic_ack(delivery_tag=method.delivery_tag)


# Setup mongo
mongoClient = MongoClient("mongodb://root:jsdusdbabsduroo4t@34.32.62.187:27017/")
db = mongoClient["clarity"]
collection = db["tracker"]


# Setup rabbit
connection = pika.BlockingConnection(
    pika.ConnectionParameters(
        host="34.32.62.187",
        port=5672,
        credentials=pika.credentials.PlainCredentials("user", "passwordadhahsd7"),
    )
)

channel = connection.channel()

channel.queue_declare(queue="task_queue", durable=True)

channel.basic_qos(prefetch_count=1)
channel.basic_consume(queue="task_queue", on_message_callback=callback)

print("Waiting for messages...")
channel.start_consuming()
