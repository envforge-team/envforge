# Monitoring API Contracts

The monitoring API exposes the latest health and metric information together
with recent operational events for an EnvForge environment.

The endpoints described here are contracts for the implementation planned in
week 3. They are not implemented during week 2.

## Base resource

```text
/api/environments/{environmentId}/monitoring
```

`environmentId` is the UUID of an existing EnvForge environment.

---

## Get latest metrics

```http
GET /api/environments/{environmentId}/monitoring/metrics
```

Returns the latest available monitoring snapshot for the selected environment.

### Successful response

Status:

```text
200 OK
```

Body:

```json
{
  "environmentId": "7a7c9477-aab7-4a29-bf5c-4f9952907fb2",
  "environmentName": "reliability-demo",
  "namespace": "env-reliability-demo",
  "status": "HEALTHY",
  "cpuUsagePercent": 23.7,
  "memoryUsageBytes": 384827392,
  "requestRatePerSecond": 18.4,
  "errorRatePercent": 0.6,
  "capturedAt": "2026-07-30T10:40:00Z"
}
```

### Metric response fields

| Field | Type | Nullable | Description |
|---|---|---:|---|
| `environmentId` | UUID | No | Environment identifier |
| `environmentName` | string | No | Environment name |
| `namespace` | string | No | Kubernetes namespace |
| `status` | enum | No | Aggregated health status |
| `cpuUsagePercent` | number | Yes | CPU usage from 0 to 100 |
| `memoryUsageBytes` | integer | Yes | Memory usage in bytes |
| `requestRatePerSecond` | number | Yes | Requests per second |
| `errorRatePercent` | number | Yes | HTTP error percentage |
| `capturedAt` | ISO-8601 timestamp | No | Snapshot creation time |

Allowed status values:

```text
HEALTHY
DEGRADED
UNHEALTHY
UNKNOWN
```

Metric values are nullable because Prometheus may return incomplete data.

A partial response still uses `200 OK`. Unavailable metrics are returned as
`null`.

### No snapshot available

If the environment exists but no monitoring snapshot has been collected:

```text
204 No Content
```

The portal maps this response to a `null` snapshot.

### Environment not found

```text
404 Not Found
```

Uses the common EnvForge `ApiError` response.

---

## Get environment events

```http
GET /api/environments/{environmentId}/monitoring/events
```

Returns environment events ordered from newest to oldest.

### Successful response

Status:

```text
200 OK
```

Body:

```json
[
  {
    "id": "39c4dc61-963c-4192-a807-d3bd06f9b73b",
    "environmentId": "7a7c9477-aab7-4a29-bf5c-4f9952907fb2",
    "eventType": "HEALTH_RECOVERED",
    "severity": "INFO",
    "source": "monitoring-service",
    "message": "The environment returned to a healthy state.",
    "occurredAt": "2026-07-30T10:39:12Z"
  }
]
```

If no events exist, the endpoint returns:

```json
[]
```

### Event response fields

| Field | Type | Nullable | Description |
|---|---|---:|---|
| `id` | UUID | No | Event identifier |
| `environmentId` | UUID | No | Affected environment |
| `eventType` | enum | No | Type of operational event |
| `severity` | enum | No | Event severity |
| `source` | string | No | Component that created the event |
| `message` | string | No | Human-readable description |
| `occurredAt` | ISO-8601 timestamp | No | Event creation time |

Allowed severity values:

```text
INFO
WARNING
ERROR
```

Allowed event type values:

```text
CREATED
DEPLOYMENT_STARTED
DEPLOYMENT_SUCCEEDED
DEPLOYMENT_FAILED
POD_RESTARTED
HEALTH_DEGRADED
HEALTH_RECOVERED
EXPIRED
DELETED
```

### Environment not found

```text
404 Not Found
```

Uses the common EnvForge `ApiError` response.

---

## Error format

Monitoring endpoints use the existing EnvForge error contract:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Environment not found: 7a7c9477-aab7-4a29-bf5c-4f9952907fb2",
  "path": "/api/environments/7a7c9477-aab7-4a29-bf5c-4f9952907fb2/monitoring/metrics",
  "timestamp": "2026-07-30T10:45:00Z"
}
```

## Implementation boundary

Week 2 defines only these contracts.

The following work belongs to week 3:

- Java `MetricResponse`;
- Java `EventResponse`;
- `MonitoringService`;
- monitoring controllers;
- Prometheus queries;
- real portal API requests.
