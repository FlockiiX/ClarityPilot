// These are the dependencies for this file.
//
// You installed the `dotenv` and `octokit` modules earlier. The `@octokit/webhooks` is a dependency of the `octokit` module, so you don't need to install it separately. The `fs` and `http` dependencies are built-in Node.js modules.
import dotenv from "dotenv";
import { App } from "octokit";
import { createNodeMiddleware } from "@octokit/webhooks";
import fs from "fs";
import http from "http";

// This reads your `.env` file and adds the variables from that file to the `process.env` object in Node.js.

// This assigns the values of your environment variables to local variables.
const appId = 2333819;
const webhookSecret = "test";

// This reads the contents of your private key file.
const privateKey = fs.readFileSync("./claritypilot.2025-11-22.private-key.pem", "utf8");

// This creates a new instance of the Octokit App class.
const app = new App({
    appId: appId,
    privateKey: privateKey,
    webhooks: {
        secret: webhookSecret
    },
});


// This adds an event handler that your code will call later. When this event handler is called, it will log the event to the console. Then, it will use GitHub's REST API to add a comment to the pull request that triggered the event.
async function handlePullRequestOpened({ octokit, payload }) {
    console.log(`Received a pull request event for #${payload}`);
};

// This sets up a webhook event listener. When your app receives a webhook event from GitHub with a `X-GitHub-Event` header value of `pull_request` and an `action` payload value of `opened`, it calls the `handlePullRequestOpened` event handler that is defined above.
app.webhooks.onAny(handlePullRequestOpened);

// This logs any errors that occur.
app.webhooks.onError((error) => {
    if (error.name === "AggregateError") {
        console.error(`Error processing request: ${error.event}`);
    } else {
        console.error(error);
    }
});

// This determines where your server will listen.
//
const port = 5000;
const path = "/gh/webhooks";
const localWebhookUrl = `https://clarity-pilot.com/gh/webhooks`;

// This sets up a middleware function to handle incoming webhook events.
//
// Octokitddd's `createNodeMiddleware` function takes care of generating this middleware function for you. The resulting middleware function will:
//
// - Check the signature of the incoming webhook event to make sure that it matches your webhook secret. This verifies that the incoming webhook event is a valid GitHub event.
// - Parse the webhook event payload and identify the type of event.
// - Trigger the corresponding webhook event handler.
const middleware = createNodeMiddleware(app.webhooks, { path });

// This creates a Node.js server that listens for incoming HTTP requests (including webhook payloads from GitHub) on the specified port. When the server receives a request, it executes the `middleware` function that you defined earlier. Once the server is running, it logs messages to the console to indicate that it is listening.
http.createServer(middleware).listen(port, () => {
    console.log(`Server is listening for events at: ${localWebhookUrl}`);
    console.log('Press Ctrl + C to quit.')
});
