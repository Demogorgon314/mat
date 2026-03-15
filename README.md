# mat-cli

Standalone CLI-focused fork of [Eclipse Memory Analyzer (MAT)](https://github.com/eclipse-mat/mat).

This repository is forked from [`eclipse-mat/mat`](https://github.com/eclipse-mat/mat). It keeps the upstream MAT codebase as its foundation, but this fork is maintained specifically for the standalone `mat-cli` experience: headless heap-dump analysis, CLI packaging, GitHub Releases, and Homebrew distribution.

If you are looking for the full Eclipse MAT desktop/RCP distribution, official project website, or upstream project governance, use the upstream project:

- Upstream repository: <https://github.com/eclipse-mat/mat>
- Upstream website: <https://eclipse.dev/mat/>

## Why This Fork Exists

- Publish standalone `mat-cli` builds from GitHub Releases
- Make `mat-cli` easy to install with Homebrew
- Improve the headless CLI workflow for scripting and local debugging
- Keep CLI packaging and release automation moving independently from the upstream distribution

## Features

- Analyze Java heap dumps from the command line
- Text and JSON output modes
- Built-in commands such as `summary`, `threads`, `histogram`, `instances`, `inspect-object`, `top-consumers`, `path2gc`, `oql`, and `query`
- Standalone zip releases and Homebrew installation
- Built on top of Eclipse MAT internals and query engine

## Install

### Homebrew

```bash
brew install Demogorgon314/mat-cli/mat-cli
```

### Release Zip

1. Download the latest archive from [GitHub Releases](https://github.com/Demogorgon314/mat-cli/releases/latest).
2. Unzip it.
3. Run `./mat-cli --help` from the extracted directory.

## Requirements

- Java 17 or newer to run the standalone release
- Java 17 and Maven 3.9.x to build from source

## Quick Start

```bash
mat-cli --help
mat-cli summary path/to/heap.hprof
mat-cli histogram path/to/heap.hprof --format json
mat-cli oql path/to/heap.hprof --query "SELECT * FROM java.lang.String s"
```

Useful discovery commands:

- `mat-cli describe <command>`
- `mat-cli schema <command> --format json`
- `mat-cli list-queries --format json`
- `mat-cli describe-query <query-id> --format json`

## Build From Source

Run builds from [`parent/`](parent):

```bash
cd parent
mvn clean package -DskipTests -Dmat-product=mat-cli
```

The standalone CLI zip is produced at:

```text
org.eclipse.mat.product/target/extraArtifacts/mat-cli.zip
```

## Project Scope

This fork is primarily for:

- `mat-cli` behavior and usability
- Standalone CLI packaging
- GitHub release automation
- Homebrew tap integration
- Headless workflows and documentation for CLI users

This fork is not primarily for:

- The Eclipse MAT desktop/RCP product
- Upstream-wide project governance
- General upstream release management for all MAT distributions

## Contributing

Contributions are welcome, especially for:

- CLI UX improvements
- Packaging and release automation
- Homebrew integration
- Documentation for CLI workflows
- Bug fixes that directly affect `mat-cli`

Before opening a change:

- Read [CONTRIBUTING.md](CONTRIBUTING.md)
- Follow the build and module guidance in [dev-doc/Building_MAT_with_Maven.md](dev-doc/Building_MAT_with_Maven.md)
- Add or update tests when behavior changes

## Issues and Support

Open issues in this repository for:

- `mat-cli` packaging or installation problems
- GitHub Release or Homebrew integration issues
- CLI-specific behavior in this fork

If you hit a bug that clearly belongs to upstream MAT rather than this fork's CLI-focused work, prefer filing it with the upstream project:

- Upstream issues: <https://github.com/eclipse-mat/mat/issues>

## License

This project remains available under the Eclipse Public License 2.0. See [LICENSE](LICENSE) for details.
