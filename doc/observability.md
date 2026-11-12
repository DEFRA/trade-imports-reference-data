# Observability

## Publishing

`EmfMeterRegistry` publishes timers, distribution summaries, counters and gauges to CloudWatch as EMF once a minute (`aws.emf.step`), with a meter's tags as its only dimensions. It does not publish function counters, function timers, long task timers or time gauges. A meter is registered, and so published, only when its name or a dot-separated prefix of it is enabled under `management.metrics.enable` in `application.yml`: `controller: true` lets through every `controller.*` meter.

`aws.emf.enabled` is the one switch for publishing. When it is true, a blank or invalid `aws.emf.namespace` stops start-up. The EMF library reads `AWS_EMF_AGENT_ENDPOINT` from the environment itself.

## External-call metrics

Every call to a system outside the INS boundary is recorded by `ExternalCallRecorder` as two Micrometer meters, tagged `dependency` and `operation`:

- `external.call.duration`: a timer of how long the call took, published in milliseconds
- `external.call.failure`: a distribution summary of 1 for a failed call (an exception, or an HTTP status of 400 or more) and 0 for a successful one, so its average is the failure rate

Each call is also logged, because EMF documents go to CloudWatch and cannot be searched in OpenSearch. A failed call's line carries the exception, with `error.type` and `error.message`, or the dependency's `http.response.status_code`.

The volumetrics interface id (`Interface`, `SYN-19`) the old EMF documents carried is no longer recorded (EUDPA-674, decision D2). A Micrometer meter has no free-form EMF property, and as a tag it would become a CloudWatch dimension.

## Other metrics

| Metric | Type | Tag | What it records |
|---|---|---|---|
| `external.dependency.available` | gauge | `dependency` | 1 after the dependency's most recent call succeeded, 0 after it failed; not published until it has been called |
| `cache.hit`, `cache.miss` | counter | `cache` | Each read of an MDM cache, counted by the cache's own stats counter |
| `lookup` | counter | `lookup.type` | Each request for `countries`, `ports-of-entry`, `commodity-search` or `species-check` |
| `controller.<operation>.time`, `service.<operation>.time` | timer | `class`, `method`, `exception` | Each call to a `@Timed` endpoint or service method |
| `jvm.memory.committed`, `jvm.memory.used`, `jvm.memory.max` | gauge | `area`, `id` | JVM memory, in bytes |
| `jvm.threads.live`, `jvm.threads.peak` | gauge | — | JVM thread counts |

`lookup` counts the same requests as the controller timers' sample counts. It is kept because it is on the agreed metric list, with every lookup type under one metric and one `lookup.type` dimension (EUDPA-674, decision D8).

`ExternalCall` names every call recorded in `external.call.duration` and `external.call.failure`; its dependencies are also the `external.dependency.available` gauges:

| Dependency | Operation | Feign client method |
|---|---|---|
| `mdm` | `get-countries` | `mdm-client#getCountries` |
| `mdm` | `get-ports-of-entry` | `mdm-client#getPorts` |
| `mdm` | `search-products` | `mdm-client#searchProducts` |
| `mdm` | `get-commodities` | `mdm-client#getCommodities` |
| `mdm` | `get-commodity-species` | `mdm-client#getCommoditySpecies` |
| `trade-token` | `client-credentials-token` | `trade-client#getTradeAuthToken` |

A new Feign client method is measured only once `ExternalCallFeignCapability.CALLS_BY_CLIENT_METHOD` maps it to an `ExternalCall`.

## Why calls are timed at the Feign transport

`ExternalCallFeignCapability` is a Feign `Capability` that wraps the transport `Client`, so it times each call in `Client.execute`, not in `MdmService`.

`MdmClientInterceptor` fetches the Trade token inside the MDM request's interceptor. Timing in `MdmService` would add the token call's time to MDM's figures on every call made with an expired token. Interceptors have already run before `Client.execute`, so the transport times each call on its own, and the token call is measured as its own `trade-token` call.

One application-level capability covers every Feign client, so a new client is measured without code of its own.
