# Repository Guidelines

## Project Structure & Module Organization
MAT is a multi-module Maven/Tycho build rooted at [`parent/`](./parent). Core bundles live under [`plugins/`](./plugins), Eclipse features under [`features/`](./features), product packaging under [`org.eclipse.mat.product/`](./org.eclipse.mat.product), target platform definitions under [`org.eclipse.mat.targetdef/`](./org.eclipse.mat.targetdef), and the p2 update site under [`org.eclipse.mat.updatesite/`](./org.eclipse.mat.updatesite). Build and contributor docs are in [`dev-doc/`](./dev-doc). Main test bundles are [`plugins/org.eclipse.mat.tests`](./plugins/org.eclipse.mat.tests) and [`plugins/org.eclipse.mat.ui.rcp.tests`](./plugins/org.eclipse.mat.ui.rcp.tests).

MAT CLI lives in [`plugins/org.eclipse.mat.cli`](./plugins/org.eclipse.mat.cli), with packaging in [`features/org.eclipse.mat.cli.feature`](./features/org.eclipse.mat.cli.feature) and [`org.eclipse.mat.product/mat-cli.product`](./org.eclipse.mat.product/mat-cli.product). GitHub tag releases are handled by [` .github/workflows/release-cli.yaml`](./.github/workflows/release-cli.yaml) and publish only the standalone `mat-cli` zip.

## Build, Test, and Development Commands
Run builds from `parent/`:

- `mvn clean install` — full Tycho build, tests, features, update site, and products.
- `mvn clean install spotbugs:spotbugs` — full build plus SpotBugs reports.
- `mvn clean package -DskipTests -Dmat-product=mat-cli` — build the standalone CLI package.
- `mvn verify -Dtest=org.eclipse.mat.tests.cli.CliTests` — run a focused PDE test class when iterating.

Generated artifacts are written to each module’s `target/` directory; CLI zips are under [`org.eclipse.mat.product/target/extraArtifacts/`](./org.eclipse.mat.product/target/extraArtifacts). The standalone release artifact is `mat-cli.zip`, which unpacks to a directory containing `mat-cli` and `mat-cli.bat`.

## Coding Style & Naming Conventions
Use Java 17 and Maven 3.9.x. Follow the surrounding Eclipse style instead of reformatting unrelated code. Keep package names under `org.eclipse.mat.*`, class names in `UpperCamelCase`, methods and fields in `lowerCamelCase`, and constants in `UPPER_SNAKE_CASE`. Prefer small, focused classes and reuse existing MAT query and snapshot APIs before adding new infrastructure.

## Testing Guidelines
Add or update tests for any public behavior change. Place non-UI coverage in `plugins/org.eclipse.mat.tests` and UI/PDE coverage in `plugins/org.eclipse.mat.ui.rcp.tests`. Name test classes `*Test` and group suites explicitly when needed. Validate both happy paths and failure modes, especially around snapshot parsing, query output, and serialization.

## Commit & Pull Request Guidelines
Keep commit subjects short, imperative, and descriptive. Existing history uses plain subjects and optional prefixes such as `feat:`, `fix:`, and `nit:`. Sign commits with `git commit -s` to satisfy Eclipse contribution requirements. PRs should target `master`, summarize user-visible changes, mention test coverage, and link related issues. Include screenshots only for UI changes.

## Contributor Notes
Do not commit `target/` outputs or other generated artifacts. Prefer small, reviewable changes and update docs when build, packaging, or contributor workflows change. For MAT CLI work, keep the runtime headless: avoid adding `org.eclipse.mat.ui*` dependencies unless they are strictly required.

## Skills
- `group-metadata-members`: Retrieve and inspect members from an io.streamnative.pulsar.handlers.kop.coordinator.group.GroupMetadata object in a MAT heap dump using OQL queries and the inspect-object command.
