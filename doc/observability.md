# Observability

## External-call metric

Every call to a system outside the INS boundary is sent as one EMF document, with the dimensions `Dependency` and `Operation`, carrying:

- `ExternalCallDuration`: how long the call took, in milliseconds
- `ExternalCallFailure`: 1 for a failed call (an exception, or an HTTP status of 400 or more), 0 for a successful one

Each call is also logged, because EMF documents go to CloudWatch and cannot be searched in OpenSearch.

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
