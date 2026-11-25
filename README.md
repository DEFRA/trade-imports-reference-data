# trade-imports-reference-data

Spring Boot service that serves countries, ports of entry and commodity
reference data from MDM: `GET /countries`, `GET /ports-of-entry`,
`GET /commodities` and `POST /commodities/species-check`. It listens on port
8086.

* [Local stack](#local-stack)
* [MongoDB](#mongodb)
* [Inspect MongoDB](#inspect-mongodb)
* [Testing](#testing)
* [Running](#running)
* [Dependabot](#dependabot)


### Local stack

The full local environment (MongoDB, Floci, Redis, the stubs, and every
trade-imports service including this one) is the workspace stack in
[DEFRA/trade-imports-workspace](https://github.com/DEFRA/trade-imports-workspace):

```bash
# from the workspace root
tim docker up                    # full stack from published images
tim docker up -e reference-data  # everything except this service (run it from your IDE)
tim docker down                  # tear down and wipe volumes
```

### MongoDB

#### MongoDB via Docker

Run the workspace stack's infrastructure tiers (MongoDB, Floci, Redis and
`cdp-uploader`):

```bash
# from the workspace root
tim docker up --profile database --profile infrastructure
```

#### MongoDB locally

Alternatively install MongoDB locally:

- Install [MongoDB](https://www.mongodb.com/docs/manual/tutorial/#installation) on your local machine
- Start MongoDB:
```bash
sudo mongod --dbpath ~/mongodb-cdp
```

#### MongoDB in CDP environments

In CDP environments a MongoDB instance is already set up
and the credentials exposed as environment variables.


### Inspect MongoDB

To inspect the Database and Collections locally:
```bash
mongosh
```

You can use the CDP Terminal to access the environments' MongoDB.

### Testing

Unit tests (`*Test.java`) run with Surefire:

```bash
mvn test
```

Integration tests (`*IT.java`) start the full Spring Boot application with
[Testcontainers](https://testcontainers.com/): MongoDB, and MockServer standing
in for MDM and the Trade Platform token endpoint. They need Docker. Failsafe
runs them after the unit tests, then Checkstyle runs:

```bash
mvn verify
```

### Running

Run the application:
```bash
mvn spring-boot:run
```

By default it calls MDM and the Trade Platform token endpoint on
`http://localhost:8087`, where `trade-imports-stub` listens. Set
`MDM_SERVICE_URL` and `TRADE_PLATFORM_AUTH_URL` to call something else.

### Ports of entry

Port lists are grouped in `PortType` declaration order: airports, then seaports, then rail ports. A port whose MDM traffic matches no type comes last, and the first sight of each such port and traffic is logged as a warning.

### Commodity search

MDM `/search` returns two `resultType`s on one page. A species row is one species under one code. A commodity row is one code with nested `scientificNames`, and counts as one result however many names it holds. Common-name and Latin-name searches come back as species rows (one per code that lists that species). An exact code comes back as one commodity row. A description can mix both, so the nested species of that commodity come back too. `CommoditySearchService` flattens both into groups of Latin names. The nested list on a commodity search row is not the species catalogue: `01061900` nested 100 names on search when `GET /mdm/trade/product/commodities/{code}/species` holds 1,234. Captures that show each shape are the keys `cattle`, `Bos taurus`, `0102`, `01061900`, `010` and `ostrich` in `trade-imports-stub`'s `mdmProductSearchResponses.json`.

### Metrics

- Which meters reach CloudWatch, and why external calls are also logged, is in [doc/observability.md](doc/observability.md).
- `external.dependency.available` is for the dashboard only. It is not a Spring `HealthIndicator`, because CDP uses `/health` to decide whether a task is alive and would restart every task whenever a dependency went down.
- Dependencies are not probed, because the MDM caches exist to limit how often the service calls MDM. `external.dependency.available` is therefore the last real outcome, whatever its age. Countries and ports are cached for `cache.mdm.ttl-minutes` (60 by default); commodity search and species check are not, so the MDM reading reflects the latest commodity search or species check. The token is reused until it expires. The value stays at 1 after a dependency goes down until the next call, and one failed call holds it at 0 until a call succeeds.
- The metrics, and how and why external calls are measured at the Feign transport, are in [doc/observability.md](doc/observability.md).
- The "dependency unavailable" alert is built on `external.call.failure` over a time window, per `dependency` and `operation`, which reports only calls that happened.
- `AWS_EMF_ENVIRONMENT` is left blank on CDP, so the EMF library finds the platform's agent. Set it to `Local` to write the EMF documents to stdout.
- MongoDB is left out: it is configured, but nothing reads or writes through it.

### Configuration notes

- When `HTTP_PROXY` is set to `http://host:port`, every outbound HTTP and HTTPS call goes through it, as CDP requires for egress; without it, calls go direct. A malformed value is logged with its credentials masked and without the parse exception, whose message repeats the URL.
- `GlobalExceptionHandler` answers application exceptions, invalid request input (a constraint violation, an unreadable body or a parameter of the wrong type) as 400, and a 503 when MDM returns no body, content or total, answers 502, 503 or 504, or is not reachable, or when the token endpoint answers 502, 503 or 504 or is not reachable. Any other MDM error is 500. Spring's other exceptions, such as a 404 for an unknown path, keep Spring's answers.

### SonarCloud

The SonarCloud scan is
[.github/workflows/sonarcloud.yml](.github/workflows/sonarcloud.yml). It runs
on every pull request and on every merge to `main`.

### Dependabot

[.github/dependabot.yml](.github/dependabot.yml) proposes updates to direct
Maven dependencies and GitHub Actions every Monday.


### About the licence

The Open Government Licence (OGL) was developed by the Controller of Her Majesty's Stationery Office (HMSO) to enable
information providers in the public sector to license the use and re-use of their information under a common open
licence.

It is designed to encourage use and re-use of information freely and flexibly, with only a few conditions.
