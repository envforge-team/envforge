import os
import time
import urllib.error
import urllib.request


TARGET_URL = os.getenv(
    "TARGET_URL",
    "http://reliability-demo-api/work",
)

REQUESTS_PER_SECOND = float(
    os.getenv("REQUESTS_PER_SECOND", "2"),
)

REQUEST_TIMEOUT_SECONDS = float(
    os.getenv("REQUEST_TIMEOUT_SECONDS", "5"),
)

if REQUESTS_PER_SECOND <= 0:
    raise ValueError(
        "REQUESTS_PER_SECOND must be greater than zero"
    )

interval = 1.0 / REQUESTS_PER_SECOND

success_count = 0
error_count = 0


def send_request() -> None:
    global success_count
    global error_count

    try:
        with urllib.request.urlopen(
            TARGET_URL,
            timeout=REQUEST_TIMEOUT_SECONDS,
        ) as response:
            status = response.status

            if 200 <= status < 400:
                success_count += 1
            else:
                error_count += 1

    except urllib.error.HTTPError as error:
        error_count += 1

        print(
            f"HTTP {error.code} from {TARGET_URL}",
            flush=True,
        )

    except Exception as error:
        error_count += 1

        print(
            f"Request failed: {error}",
            flush=True,
        )


print(
    (
        "EnvForge traffic generator started: "
        f"target={TARGET_URL}, "
        f"rps={REQUESTS_PER_SECOND}"
    ),
    flush=True,
)

last_report = time.monotonic()

while True:
    started = time.monotonic()

    send_request()

    now = time.monotonic()

    if now - last_report >= 10:
        print(
            (
                "Traffic summary: "
                f"success={success_count}, "
                f"errors={error_count}"
            ),
            flush=True,
        )

        last_report = now

    elapsed = time.monotonic() - started
    sleep_time = interval - elapsed

    if sleep_time > 0:
        time.sleep(sleep_time)
