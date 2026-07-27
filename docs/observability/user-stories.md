# Observability User Stories

## Metrics

As an operator, I want to view CPU and memory usage so that I can identify environments with resource problems.

As an operator, I want to view request rate, latency and error rate so that I can evaluate application health.

As a developer, I want application metrics to be collected automatically so that I do not need to inspect each container manually.

## Events

As an operator, I want to view environment events so that I can understand deployments, restarts and failures.

As a developer, I want recent environment events to be available through an API so that they can be displayed in the portal.

## Logs

As an operator, I want to inspect application and Kubernetes logs so that I can investigate incidents.

As a developer, I want logs to contain useful context such as environment name, application and timestamp.

## Alerts

As an operator, I want to receive an alert when the application produces too many HTTP 5xx responses.

As an operator, I want to receive an alert when an application becomes unavailable.

As an operator, I want to receive an alert when CPU or memory usage remains high for a sustained period.

As an operator, I want alerts to contain enough information to identify the affected environment.
