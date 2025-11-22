## Inspiration 
In today’s high-pressure academic and professional environments, especially for students, researchers, and developers mental overload has become the norm. 
We constantly juggle tasks across platforms like Jira, Slack, GitHub, Notion, and email. 
Meanwhile, our wearables collect tons of health data, but fail to turn it into meaningful, actionable guidance.

**We asked ourselves:**\
Why do our tools know so much about us but do so little for us?
Where is the companion that understands our work patterns and cares for our cognitive well-being?

That question became the spark for **ClarityPilot** - a proactive health and productivity companion that bridges the critical gap between raw data and real support.

## What it does 
ClarityPilot is a health companion that lives right on your home screen. 
Its mission: help you conquer cognitive overload with actionable, personalized recommendations.

**Key features**:
- **Unified Workload Intelligence**: Connect Jira, Slack, GitHub and more. Using webhooks and integrations, ClarityPilot tracks how long you’ve been working, what you’re working on, and when your workload peaks.
- **Holistic Cognitive State Estimation**: We combine:
  - Activity and biometrics from wearables
  - Screen-time and focus information
  - Work context from productivity tools
  - …to build a real-time model of your mental and cognitive state.
- **Proactive, AI-powered Interventions**: Not just data dashboards. ClarityPilot nudges you before burnout hits.
"You’ve been debugging for 97 minutes straight—how about a 10-minute reset?"
- **Accessibility-first Home Screen Widget**: A clean, minimalist, always-visible widget provides:
  - Instant insights
  - Personalized nudges
  - Micro-suggestions for mood, recovery, and productivity

ClarityPilot isn’t passive. It’s your co-pilot! Anticipating your state and guiding you toward healthier, more sustainable work habits.
## How we built it 
## Tech Stack
### Android app (100% Native Kotlin): 
Chosen for performance, accessibility capabilities, and deep OS-level integration for widgets and background services.

### Tracking Integrations:
- Jira: Webhooks + OAuth to track issue transitions, time-in-status
- Slack: Event API to detect messaging bursts, work intensity
- GitHub: Push events + commit frequency + PR activity
- Google Fit: Activity and biometrics

### AI-powered Recommendation Engine
A lightweight backend service processes sensor & productivity data, generating context-aware recommendations using:

### Proactive Delivery System
Uses Android’s foreground services + widget updates + notifications to deliver time-sensitive nudges.

## Challenges we ran into 
- Integrating heterogeneous APIs (Slack, GitHub, Jira, wearables) with different auth workflows and event structures
- Balancing helpfulness vs. intrusiveness when crafting proactive recommendations
- Building a real-time cognitive load estimation model from short-term data streams
- Widget refresh constraints on Android (rate limits, update cycles, battery impact)
- Ensuring privacy and security while handling sensitive health and work data
- Time constraints—packing AI, integrations, a widget system, and a polished UI into the hackathon timeline

## Accomplishments that we're proud of 
- We built a fully functional Kotlin-native Android app with a polished home-screen widget.
- Achieved live workload monitoring from multiple developer tools through webhooks.
- Developed an AI-backed recommendation engine that generates meaningful, context-aware suggestions.
- Successfully merged health + work data into a unified cognitive state signal.
- Delivered a clean, accessibility-focused UI that feels like a true digital companion.
- Built something students, developers, researchers—and we ourselves—would genuinely use every day.

## What we learned 
- Cognitive overload is measurable—but only when combining both physiological and digital work signals.
- Even small nudges (break suggestions, micro-recovery prompts) can dramatically reduce stress accumulation.
- Android’s widget and accessibility ecosystems are powerful but require careful optimization.
- Integrations are everything: productivity tools hold the missing half of the mental health equation.
- Building humane technology requires empathy, not just data.

## What's next for ClarityPilot
ClarityPilot is just taking off. Here’s where we want to go:
- Expanded integration ecosystem (Notion, Linear, VS Code telemetry, email clients)
- Smarter AI models using daily routines, chronotypes, and personalized stress signatures
- Gamified recovery streaks to encourage healthy habits
- In-app journal and reflection insights
- On-device inference for maximum privacy
- iOS version once the Android companion is fully refined
- Enterprise dashboard for burnout-prevention analytics (with strong anonymity protections)

Our long-term vision:
A cognitive-health operating system that supports knowledge workers everywhere—before burnout ever begins.