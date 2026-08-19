---
status: accepted
date: 2026-08-18
decision-makers: Chrisime (repo owner)
consulted: n/a
informed: n/a
---

# Publish releases to GitHub Packages on version tags

## Context and Problem Statement

The release pipeline published to Bintray — shut down in 2021 — triggered by repository stars, used the deprecated `actions/create-release@v1` with a placeholder body, and left consumers no live artifact repository. The project needed a publishing destination that is live, org-scoped, and free, with release notes that describe the actual changes.

## Decision Drivers

- A live, journaled publication repository for consumers
- Pricing/ops: free and org-scoped without Sonatype/GPG ceremony
- Publishing must be repeatable and triggered by tags, not repository stars

## Considered Options

- GitHub Packages via `maven-publish` (`packageRegistry`), tag-gated
- Maven Central (Sonatype)
- JitPack (build-on-demand)

## Decision Outcome

Chosen option: "GitHub Packages via `maven-publish` (`packageRegistry`), tag-gated", because it provides a free, org-scoped live repository with `GITHUB_TOKEN`-auth and no external publishing setup, and satisfies the tag-driven ("publishing on version tags only") contract.

### Consequences

- Good, because consumers get a live artifact repository.
- Good, because no Sonatype/GPG setup; auth is the workflow's `GITHUB_TOKEN`.
- Good, because star-triggered and dead-Bintray publishing are removed.
- Bad, because releases are only added on tag pushes; the workflow needs `permissions: packages: write`.
- Bad, because a version tag must carry a real changelog-derived body (no placeholders).

### Confirmation

The `reliable-release-pipeline` spec asserts a version-tag push builds and publishes to GitHub Packages, and that repository stars do not trigger publishing.

## Pros and Cons of the Options

### GitHub Packages

- Good, because free, org-scoped, live, `GITHUB_TOKEN`-auth.
- Bad, because gated to GitHub as the host.

### Maven Central (Sonatype)

- Good, because universal consumer reach.
- Bad, because Sonatype + GPG setup and coordination burden; rejected in DQ1.

### JitPack

- Good, because zero publishing config.
- Bad, because build-on-demand is not a maintainer-controlled published artifact registry; rejected in DQ1.

## More Information

Resolved as Decision Question 1 in the `modernize-build-release` proposal (design D1, D4).