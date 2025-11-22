import uuid
import json
from datetime import datetime, timezone


def convert_github_payload(payload):
    """
    Converts a GitHub webhook payload (dict) into the standardized schema.
    """

    # Safely access nested dictionaries using .get() to prevent errors if keys are missing
    head_commit = payload.get("head_commit", {})
    repository = payload.get("repository", {})
    sender = payload.get("sender", {})
    pusher = payload.get("pusher", {})

    # 1. Determine Timestamp
    # GitHub sends timestamps like "2025-11-22T10:47:30+01:00".
    # We attempt to convert to UTC "Z" format to match your schema.
    raw_ts = head_commit.get("timestamp")
    formatted_ts = raw_ts
    try:
        if raw_ts:
            dt_obj = datetime.fromisoformat(raw_ts)
            # Convert to UTC
            dt_utc = dt_obj.astimezone(timezone.utc)
            # Format as YYYY-MM-DDTHH:MM:SSZ
            formatted_ts = dt_utc.strftime("%Y-%m-%dT%H:%M:%SZ")
    except ValueError:
        pass  # Keep original string if parsing fails

    # 2. Construct the Output Dictionary
    output = {
        "_id": str(uuid.uuid4()),
        # Prefer sender login (github username), fallback to pusher name
        "user_identifier": sender.get("login") or pusher.get("name"),
        "provider": "github",
        "activity_type": "push",  # Inferred, as this payload structure is specific to pushes
        "timestamp": formatted_ts,
        "raw_payload": payload,
        "metadata": {
            "commit_msg": head_commit.get("message"),
            "repo_name": repository.get("name"),
            "ref": payload.get("ref"),
            "ticket_id": None,  # GitHub push payloads don't explicitly separate ticket IDs
            "channel": None,  # Specific to Slack
        },
    }

    return output
