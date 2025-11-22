from pymongo import MongoClient, DESCENDING
from typing import List, Literal, Optional, Union, TypedDict

from celery import Celery
from celery.schedules import crontab

import redis
import json
from google import genai
from datetime import datetime, timedelta
from google.genai import types
from google.genai.types import (
    GenerateContentConfig,
    GoogleMaps,
    Tool,
)

MAPPED_NAMES = {
    "": "",
    "github": "GitHub",
    "slack": "Slack",
    "teams": "Teams",
    "jira": "Jira",
}

LLMClient = genai.Client(api_key="AIzaSyAQxwUDf8mZcrsiGmyR97suGQn4el_XW0M")

# Setup mongo
mongoClient = MongoClient("mongodb://root:jsdusdbabsduroo4t@34.32.62.187:27017/")
db = mongoClient["clarity"]
collection_Tracker = db["tracker"]

# Setup Redis
redisClient = redis.Redis(
    host="34.32.62.187", port=6379, db=0, password="yourpasd2ddsword"
)


# ---- Types
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


FontStyle = Literal["heading", "normal", "thin"]
Align = Literal["center", "left", "right"]


class TextElement(TypedDict):
    type: Literal["text"]
    content: str
    fontStyle: FontStyle
    align: Optional[Align]


class SpacerElement(TypedDict):
    type: Literal["spacer"]


ContentElement = Union[TextElement, SpacerElement]


class CardElement(TypedDict):
    type: Literal["card"]
    content: List[ContentElement]


Element = Union[TextElement, SpacerElement, CardElement]


class Layout(TypedDict):
    elements: List[Element]


class Report(TypedDict):
    workSummary: str
    conclusion: str
    recommendation: str
    mapsCallToAction: bool
    mapsCallToActionLocation: str


# ---- Code
app = Celery(broker="amqp://user:passwordadhahsd7@34.32.62.187:5672//")


@app.on_after_configure.connect
def setup_periodic_tasks(sender: Celery, **kwargs):
    sender.add_periodic_task(60.0, build_recommendations.s())


@app.task
def build_recommendations():
    print("[thinker] Got activation")
    activity = agentAnalyseActivity()
    print(
        f"[thinker] Condensed activity down to {activity['conclusion']} with recommendation {activity.get('mapsCallToActionLocation', 'None')}"
    )
    widget = agentGenerateUI(activity)
    print("[thinker] Generated UI")

    # Store widget in Redis
    redisClient.set("widget", json.dumps(widget))


def agentAnalyseActivity() -> Report:
    # Get stuff done today
    # Compute today's start and end (midnight to next midnight) in ms
    now = datetime.now()
    start_of_day = datetime(year=now.year, month=now.month, day=now.day)
    end_of_day = start_of_day + timedelta(days=1)
    start_ms = int(start_of_day.timestamp() * 1000)
    end_ms = int(end_of_day.timestamp() * 1000)

    # Query only items started today
    query = {
        "timestamp_start": {"$gte": start_ms, "$lt": end_ms},
    }

    results: List[TimelineEntry] = (
        collection_Tracker.find(query).sort("_id", DESCENDING).limit(10).to_list()
    )

    # Render results into a nice list
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

    rendered = ""

    for result in results:
        rendered += f"- {MAPPED_NAMES[result['type']]} {format_duration(result['timestamp_start'], result['timestamp_end'])}: {result['punchline']}\n"

    response = LLMClient.models.generate_content(
        model="gemini-2.5-pro",
        contents="""
# Context
- **Current Time:** """
        + datetime.now().strftime("%A, %B %d, %Y %I:%M %p")
        + """
- **User Location:** Garching Forschungszentrum (TU Munich Campus), Germany.
- **Role:** You are a smart, friendly Workplace Wellness Assistant.

# Input Data
The user has performed the following activities today (Format: <Platform/Activity> <Duration>: <Punchline>):
"""
        + rendered
        + """

# Your Task
Analyze the user's activity log to determine their current mental and physical state. Generate a JSON response based on the following logic:

1.  **Analyze:** specific work duration, intensity, and recent physical/social activity.
2.  **Decide:**
    *   **Case A (Validation):** If the user has worked for a short time, OR has already engaged in healthy activities (e.g., cycling to work, walking to lunch, socializing), honor their effort. Your recommendation should simply be to keep up the good work or enjoy their current flow.
    *   **Case B (Intervention):** If the user has been sedentary or working intensely without a break, suggest a specific, wellness-improving activity.

# Guidelines for Recommendations
*   **Be Specific & Local:** Since the user is at Garching Forschungszentrum, recommend location-specific activities (e.g., "Ride the Parabola slide in the Math building," "Walk to the Isar river," "Grab a coffee at Chicco di Caffè"). - You can use the google maps tool to find local restaurants/coffees and so on
*   **Be Fun:** Avoid generic advice like "Take a break." Suggest engaging activities like foosball, table tennis, or a specific social interaction.
*   **Language:** Always respond in English.
*   **Be Smart:** Always reflect on your recommendations if the actually make sense and if a human would like them - For example: Don't recommend going for a coffee in the evening, instead recommend going home and watch some relaxing series or recommend going to a cinema with colleagues to watch a new movie

# Output Format
You must respond exclusively with a single valid JSON object. DO NOT include markdown formatting (like ```json) or conversational filler.

**JSON Schema:**
{
    "conclusion": "A summary of what the user has achieved so far (e.g., 'You've crushed those frontend bugs...')",
    "recommendation": "The specific activity suggestion or validation message.",
    "workSummary": "Super short sentence about how much time the user has bend on which platform",
    "mapsCallToAction": boolean // Set to true ONLY if the recommendation is a specific physical location navigable via Google Maps which you also found using the google maps tool.
    "mapsCallToActionLocation": string // A search term google maps can use to find the recommended location, for example: "google.navigation:q=Englischer+Garten+Munich"
}

# Example Response (for reference only)
{
    "workSummary": "3.5h on Github & Slack",
    "conclusion": "You have been laser-focused on the MetaViewer Native app for 3 hours straight.",
    "recommendation": "Your eyes need a break! Walk over to the Math & CS building and take a ride down the Parabola Slide.",
    "mapsCallToAction": false,
    "mapsCallToActionLocation": ""
}
        """,
        config=GenerateContentConfig(
            tools=[
                # Use Google Maps Tool
                Tool(google_maps=GoogleMaps(enable_widget=False))
            ],
            tool_config=types.ToolConfig(
                retrieval_config=types.RetrievalConfig(
                    lat_lng=types.LatLng(latitude=48.2635398, longitude=11.6696901),
                    language_code="de_DE",
                ),
            ),
        ),
    )

    firstResponse = json.loads(response.text.replace("```json", "").replace("```", ""))
    return firstResponse


def agentGenerateUI(activity: Report):
    response = LLMClient.models.generate_content(
        model="gemini-3-pro-preview",
        contents="""
  This version fixes typos, clarifies the role, explicitly defines the tone, and breaks the instructions down into logical steps. This decreases the chance of the AI hallucinating or missing the logic rule.

    System Role: You are a Data Transformation Expert specializing in UI generation.

    Objective: Convert the provided "Input Report" into a valid JSON object based on the "Target Schema."

    Tone & Style Guidelines:

        Tone: Friendly, motivational, and casual.
        Length: Keep text strings concise (similar to a push notification or widget).
        Structure: Follow the exact structure of the schema provided.

    Logic Rules:

        Heading: Summarize the main achievement.
        Card Content: Provide a warm greeting followed by a concise insight.
        CTA Link: If the input report indicates mapsCallToAction is true, set the CTA link value to the provided mapsCallToActionLocation. Otherwise, remove the whole last cta section entirely.

    Target Schema:
     {
        "elements": [
            {
                "type": "text",
                "content": "3 Commits on GitHub",
                "fontStyle": "heading"
            },
            {
                "type": "spacer"
            },
            {
                "type": "card",
                "content": [
                    {
                        "type": "text",
                        "content": "Hey David, great work on GitHub today!",
                        "fontStyle": "normal"
                    },
                    {
                        "type": "spacer"
                    },
                    {
                        "type": "text",
                        "content": "You've been coding for 6 hours. Time to get outside.",
                        "fontStyle": "thin"
                    }   
                ]
            },
            {
                "type": "cta",
                "content": "Go to Starbucks",
                "link": "https://google.com/"
            }
        ]
    }


    Input Report:\n"""
        + json.dumps(activity),
    )

    finalRes = json.loads(response.text.replace("```json", "").replace("```", ""))

    return finalRes


if __name__ == "__main__":
    app.worker_main(["worker", "--beat", "--loglevel=info"])
