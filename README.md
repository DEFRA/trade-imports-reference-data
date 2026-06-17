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
