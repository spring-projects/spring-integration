Spring Integration agent guidelines
===================================

Concise extract of CONTRIBUTING.md, which remains the reference for humans.

Build
-----

* JDK 17+, Gradle wrapper: `./gradlew clean test` (`testAll` includes `@LongRunningIntegrationTest`); all tests must pass before a PR.
* Scope to a module or class: `./gradlew :spring-integration-core:test --tests "<FQCN>"`.
* Test reports: `build/reports/tests/test/` (or `testAll/`) under each module.
* Javadoc: `./gradlew api`. Reference docs: `./gradlew antora`. Local install: `./gradlew build publishToMavenLocal`.

Code style
----------

* Follow the [code style](https://github.com/spring-projects/spring-integration/wiki/Spring-Integration-Framework-Code-Style); match surrounding code, do not reformat unrelated code.
* Tabs, LF, UTF-8, no trailing whitespace, ~120 characters per line, K&R braces.
* Member order: static fields, fields, constructors, static factory methods, getters/setters, interface implementations, template methods, other methods, then `equals()`/`hashCode()`/`toString()`.
* Two blank lines before fields, constructors, static blocks and inner classes; one blank line before the closing class brace.
* Every file: Apache 2.0 license header (copy from an existing file), package, imports, one top-level class.
* Imports, blank line between groups: `java.*`, `javax.*` + `jakarta.*`, others, `org.springframework.*`, static (tests only). No wildcards.
* No `foo`/`bar`/`baz` names anywhere; use domain names like `payload`, `channel`, `handler`.
* `@since` on new public API; your real name as `@author` on changed classes.
* Add or update JUnit tests for every behavior change.

Commits and pull requests
-------------------------

* Search existing GitHub issues first; opening a PR directly without an issue is fine.
* Topic branch `GH-<issue>`, rebased on `main`, linear history.
* Headline `GH-<issue>: <imperative summary>`, then `Fixes: <issue_url>`, then a short "why?" body with `*` items.
* Backticks around annotations and code; `Signed-off-by` trailer (`git commit -s`); real first and last name as author.
* Squash only before opening the PR; keep commits added during review.
* `Auto-cherry-pick to <branch>` sentences are normally added by committers on merge.
