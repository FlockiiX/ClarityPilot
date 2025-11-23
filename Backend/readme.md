# ClarityPilot Backend

Our 3 individual, containerized and scalable backend services & required services.

Checkout our deployment endpoints:

- <https://clarity-pilot.com/user/1/widget/android>
- <https://clarity-pilot.com/user/1/timeline/android>

Required env variables for docker containers (set in docker swarm, etc):

```yaml
MONGO_SRV=""
GEMINI_TOKEN=""
REDIS_HOST=""
REDIS_PASSWD=""
RABBITMQ_PASSWD=""
RABBITMQ_HOST="" 
```
