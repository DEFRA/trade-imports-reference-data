# 2. Use-reference-data-for-multiple-domains-until-they-get-too-large

Date: 2026-05-07

## Status

Accepted

## Context

Reference data is required across multiple domains within the trade imports system. Rather than creating separate services for each domain from the outset, we need a pragmatic approach that avoids premature service proliferation while still allowing domains to be extracted when warranted.

The `trade-imports-reference-data` service currently manages the countries domain, sourcing country data from MDM (Master Data Management). Other domains, such as commodity codes, will also require reference data management in the future.

## Decision

We will use `trade-imports-reference-data` as a shared service to manage multiple reference data domains until any individual domain grows large enough to justify extraction into its own dedicated service.

Currently managed domains:
- **Countries** — data sourced from MDM

Planned future domains:
- **Commodity codes** — to be added to this service initially
- **BCPs/POEs** — to be added to this service initially

When a domain's complexity, data volume, or operational requirements become significant enough that managing it within this shared service becomes a burden, it will be extracted into its own service at that point.

## Consequences

**Easier:**
- Simpler initial setup — new reference data domains can be added without bootstrapping a new service
- Reduced operational overhead while domains are small
- Shared infrastructure (deployment, monitoring, MDM connectivity) across domains

**More difficult / risks:**
- As multiple domains grow, the service may become harder to reason about and deploy independently
- Extraction of a domain into its own service later will require effort and coordination; this should be treated as a planned activity rather than an afterthought
- Care must be taken to keep domain concerns well-separated within the codebase to make future extraction straightforward
