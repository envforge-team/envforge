# EnvForge Traffic Generator

Small HTTP workload generator used by the Monitoring & Reliability
module to generate repeatable traffic against the Reliability Demo API.

## Configuration

- `TARGET_URL` - HTTP endpoint receiving generated requests.
- `REQUESTS_PER_SECOND` - target request rate.
- `REQUEST_TIMEOUT_SECONDS` - timeout for each request.

Default target:

```text
http://reliability-demo-api/work

The /work endpoint supports controlled monitoring scenarios such as
HTTP 5xx responses and artificial latency.

CPU load can be triggered independently through the Reliability Demo
API incident-control endpoint.
