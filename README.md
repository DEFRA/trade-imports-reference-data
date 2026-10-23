# trade-imports-reference-data

Core delivery Java Spring Boot backend template.

* [Install MongoDB](#install-mongodb)
* [Inspect MongoDB](#inspect-mongodb)
* [Testing](#testing)
* [Running](#running)
* [Dependabot](#dependabot)


### Local stack

The full local environment (MongoDB, Floci, Redis, the stubs, and every
trade-imports-animals service including this one) is the workspace stack in
[DEFRA/trade-imports-animals-workspace](https://github.com/DEFRA/trade-imports-animals-workspace):

```bash
# from the workspace root
./scripts/stack/run-stack.sh                  # full stack from published images
./scripts/stack/run-stack.sh -e reference-data # everything except this service (run it from your IDE)
./scripts/stack/stop-stack.sh                 # tear down and wipe volumes
```

### MongoDB

#### MongoDB via Docker

Run the workspace stack's infrastructure tiers (MongoDB, Floci, Redis):

```bash
# from the workspace root
./scripts/stack/run-stack.sh --profile database --profile infrastructure
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
and the credentials exposed as enviromment variables.


### Inspect MongoDB

To inspect the Database and Collections locally:
```bash
mongosh
```

You can use the CDP Terminal to access the environments' MongoDB.

### Testing

Run the tests with:

Tests run by running a full Spring Boot application backed by [Testcontainers](https://testcontainers.com/).
Tests do not use mocking of any sort and read and write from the containerized database.

```bash
mvn test
```

### Running

Run the application:
```bash
mvn spring-boot:run
```

### Ports of entry

Port lists are grouped in `PortType` declaration order: airports, then seaports, then rail ports. A port whose MDM traffic matches no type comes last, and the first sight of each such port and traffic is logged as a warning.

### Metrics

- Every call to MDM and the Trade token endpoint is logged as well as sent as an EMF document, because EMF documents go to CloudWatch and cannot be searched in OpenSearch.
- `DependencyAvailable` is for the dashboard only. It is not a Spring `HealthIndicator`, because CDP uses `/health` to decide whether a task is alive and would restart every task whenever a dependency went down.
- Dependencies are not probed, because the MDM caches exist to limit how often the service calls MDM. `DependencyAvailable` is therefore the last real outcome, whatever its age. Countries and ports are cached for `cache.mdm.ttl-minutes` (60 by default); commodity search and species check are not, so `DependencyAvailable` for MDM reflects the latest search. The token is reused until it expires. The value stays at 1 after a dependency goes down until the next call, and one failed call holds it at 0 until a call succeeds.
- How external calls are measured, and why at the Feign transport, is in [doc/observability.md](doc/observability.md).
- The "dependency unavailable" alert is built on `ExternalCallFailure` over a time window, per `Dependency` and `Operation`, which reports only calls that happened.
- MongoDB is left out: it is configured, but nothing reads or writes through it.

### Configuration notes

- When `HTTP_PROXY` is set to `http://host:port`, every outbound HTTP and HTTPS call goes through it, as CDP requires for egress; without it, calls go direct. A malformed value is logged with its credentials masked and without the parse exception, whose message repeats the URL.
- `GlobalExceptionHandler` answers application exceptions, an MDM answer with no body, content or total as 503, and invalid request input (a constraint violation, an unreadable body or a parameter of the wrong type) as 400. Spring's other exceptions, such as a 404 for an unknown path, keep Spring's answers.

### SonarCloud

Example SonarCloud configuration are available in the GitHub Action workflows.

### Dependabot

We have added an example dependabot configuration file to the repository. You can enable it by renaming
the [.github/example.dependabot.yml](.github/dependabot.yml) to `.github/dependabot.yml`


### About the licence

The Open Government Licence (OGL) was developed by the Controller of Her Majesty's Stationery Office (HMSO) to enable
information providers in the public sector to license the use and re-use of their information under a common open
licence.

It is designed to encourage use and re-use of information freely and flexibly, with only a few conditions.
