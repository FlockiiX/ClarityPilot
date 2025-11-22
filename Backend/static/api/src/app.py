from flask import Flask, redirect, request, jsonify
import os
import json
import requests
import hmac
import hashlib
import glob
import sys

app = Flask(__name__)

# TODO: Replace with your GitHub App's credentials
GITHUB_CLIENT_ID = os.environ.get("GITHUB_CLIENT_ID", "Iv23liiAuoYbOIoyHROA")

# Load client secret from .pem file
pem_files = glob.glob("*.private-key.pem")
if not pem_files:
    print("Error: Private key file (.pem) not found.", file=sys.stderr)
    sys.exit(1)

with open(pem_files[0], "r") as f:
    private_key = f.read()

GITHUB_CLIENT_SECRET = os.environ.get("GITHUB_CLIENT_SECRET", private_key)
WEBHOOK_SECRET = os.environ.get("WEBHOOK_SECRET", "test")

TOKEN_FILE = "/persistent/github_token.json"


@app.route("/")
def hello():
    return "Hello from the Flask Fake Backend!"


@app.route("/gh/signin")
def github_signin():
    github_auth_url = (
        f"https://github.com/login/oauth/authorize?client_id={GITHUB_CLIENT_ID}"
        "&redirect_uri=https://clarity-pilot.com/sso/gh/cb/auth"
    )
    return redirect(github_auth_url)


@app.route("/sso/gh/cb/auth")
def github_auth_callback():
    code = request.args.get("code")
    if not code:
        return "Error: No code provided.", 400

    token_url = "https://github.com/login/oauth/access_token"
    payload = {
        "client_id": GITHUB_CLIENT_ID,
        "client_secret": GITHUB_CLIENT_SECRET,
        "code": code,
    }
    headers = {"Accept": "application/json"}
    response = requests.post(token_url, json=payload, headers=headers)

    if response.status_code != 200:
        return f"Error getting access token: {response.text}", 500

    token_data = response.json()

    with open(TOKEN_FILE, "w") as f:
        json.dump(token_data, f)

    return "Successfully authenticated and stored token."


@app.route("/gh/webhooks", methods=["POST"])
def github_webhooks():
    print(request)
    signature_header = request.headers.get("X-Hub-Signature-256")
    if not signature_header:
        return "Forbidden", 403

    signature = signature_header.split("=")[1]

    mac = hmac.new(WEBHOOK_SECRET.encode(), msg=request.data, digestmod=hashlib.sha256)

    if not hmac.compare_digest(mac.hexdigest(), signature):
        return "Forbidden", 403

    event = request.headers.get("X-GitHub-Event")
    payload = request.json

    print(f"Received GitHub webhook event: {event}")

    # You can process the payload here
    print(json.dumps(payload, indent=2))

    return jsonify({"status": "ok"}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
