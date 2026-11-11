# Observability

## External-call metric

Every call to a system outside the INS boundary is recorded by `ExternalCallRecorder` as two Micrometer meters, tagged `dependency` and `operation`:

- `external.call.duration`: a timer of how long the call took, published in milliseconds
- `external.call.failure`: 1 for a failed call (an exception, or an HTTP status of 400 or more), 0 for a successful one

`EmfMeterRegistry` publishes every meter to CloudWatch as EMF once a minute (`aws.emf.step`), with its tags as the only dimensions. A meter is published only when its name is on the `management.metrics.enable` allowlist in `application.yml`.

Each call is also logged, because EMF documents go to CloudWatch and cannot be searched in OpenSearch.

## Other metrics

| Metric | Tag | What it counts |
|---|---|---|
| `external.dependency.available` | `dependency` | 1 after the dependency's most recent call succeeded, 0 after it failed; not published until it has been called |
| `cache.hit`, `cache.miss` | `cache` | Each lookup in an MDM cache, counted by the cache's own stats counter, so `@Cacheable` lookups are counted too |
| `lookup` | `lookup.type` | Each request for `countries`, `ports-of-entry`, `commodity-search` or `species-check` |
| `controller.<operation>.time`, `service.<operation>.time` | `class`, `method`, `exception` | Each call to a `@Timed` endpoint or service method |

`ExternalCall` names every call the metric covers:

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
