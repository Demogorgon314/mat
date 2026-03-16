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

### Skills

If you use the skills CLI from [`vercel-labs/skills`](https://github.com/vercel-labs/skills), install the MAT heap-dump investigation skill with:

```bash
npx skills add https://github.com/Demogorgon314/mat-cli --skill mat-cli-heapdump-investigator
```

### Release Zip

1. Download the latest archive from [GitHub Releases](https://github.com/Demogorgon314/mat-cli/releases/latest).
2. Unzip it.
3. Run `./mat-cli --help` from the extracted directory.

### Shell Completion

`mat-cli` ships first-party `bash` and `zsh` completion scripts in the release archive:

- `completion/bash/mat-cli`
- `completion/zsh/_mat-cli`

You can also generate the same scripts at runtime:

```bash
./mat-cli completion bash
./mat-cli completion zsh
```

Temporary activation in the current shell:

```bash
source <(./mat-cli completion bash)
source <(./mat-cli completion zsh)
```

Persistent `bash` installation:

```bash
mkdir -p ~/.local/share/bash-completion/completions
cp ./completion/bash/mat-cli ~/.local/share/bash-completion/completions/mat-cli
```

Or generate it directly:

```bash
mkdir -p ~/.local/share/bash-completion/completions
./mat-cli completion bash > ~/.local/share/bash-completion/completions/mat-cli
```

Persistent `zsh` installation:

```bash
mkdir -p ~/.zfunc
cp ./completion/zsh/_mat-cli ~/.zfunc/_mat-cli
```

Or generate it directly:

```bash
mkdir -p ~/.zfunc
./mat-cli completion zsh > ~/.zfunc/_mat-cli
```

If your `zsh` setup does not already load `~/.zfunc`, add this once to your shell startup file before `compinit`:

```zsh
fpath=(~/.zfunc $fpath)
autoload -Uz compinit
compinit
```

## Requirements

- Java 17 or newer to run the standalone release
- Java 17 and Maven 3.9.x to build from source

## Quick Start

```bash
mat-cli --help
mat-cli histogram --help
mat-cli summary path/to/heap.hprof
mat-cli histogram path/to/heap.hprof --limit 10
mat-cli oql path/to/heap.hprof --query "SELECT * FROM java.lang.String s"
```

Useful discovery commands:

- `mat-cli <command> --help`
- `mat-cli describe <command>`
- `mat-cli schema <command> --format json`
- `mat-cli list-queries --format json`
- `mat-cli describe-query <query-id> --format json`
- `mat-cli completion <bash|zsh>`

## Example Workflows

A typical heap-dump workflow is:

`summary` -> `histogram` / `top-consumers` -> `instances` -> `inspect-object` -> `path2gc`

The sample outputs below were generated from a small test heap bundled in this repository. Your object addresses, counts, retained sizes, and field values will differ on real dumps.

### Quick Overview

Use `summary` to understand the snapshot, then `histogram` or `top-consumers` to see which classes and objects dominate memory:

```bash
mat-cli summary path/to/heap.hprof
mat-cli histogram path/to/heap.hprof --limit 20
mat-cli top-consumers path/to/heap.hprof --limit 20 --depth 3
```

Example output:

```text
Path: /path/to/heap.hprof
Format: hprof
Objects: 1261
Classes: 309
Class Loaders: 3
GC Roots: 291
Identifier Size: 4
Used Heap: 117248 bytes (114.5 KB)
Creation Date: 2010-02-05T14:27:34.534Z
```

```text
Class Name         | Objects | Shallow Heap | Retained Heap
-------------------+---------+--------------+---------------+
byte[]             | 11      | 43,264       | >= 43,264
char[]             | 148     | 43,120       | >= 43,120
java.lang.Object[] | 203     | 7,600        | >= 7,968
java.lang.String   | 154     | 3,696        | >= 12,232
java.lang.Class    | 317     | 3,640        | >= 88,200
... 304 more rows
```

### Find Instances Of A Suspicious Class

Once a class looks suspicious, use `instances` to list matching objects and get their addresses for follow-up inspection:

```bash
mat-cli instances path/to/heap.hprof --class java.lang.String --limit 20
mat-cli instances path/to/heap.hprof --class-regex 'java\\.lang\\..*String' --limit 20
mat-cli instances path/to/heap.hprof --class-contains String --include-subclasses --limit 30
```

Example output:

```text
Object Address | Class Name       | Preview                       | Shallow Heap | Retained Heap
---------------+------------------+-------------------------------+--------------+---------------+
0x27c30000     | java.lang.String | de                            | 24           | 40
0x27c30030     | java.lang.String | java.home                     | 24           | 56
0x27c30060     | java.lang.String | java.vm.specification.version | 24           | 96
... 151 more rows
```

Use `--class` for exact matches, `--class-regex` when the package or suffix is uncertain, and `--class-contains` for quick discovery.

### Inspect A Single Object

Use `inspect-object` when you already have an object address and want to see its fields, nested values, or the real payload inside a wrapper object:

```bash
mat-cli inspect-object path/to/heap.hprof --object 0x27c36000 --depth 2
mat-cli inspect-object path/to/heap.hprof --object 0x27c36000 --select-fields path
mat-cli inspect-object path/to/heap.hprof --object 0x27c36000 --field-paths path.value
```

Example output:

```text
object <object>: java.io.File @0x27c36000 [shallow=16B retained=184B]
  .prefixLength = 3 : int
  .path = "C:\\_Projects\\Project_MemoryAnalyzer\\instance_size_tests\\classes" : java.lang.String
```

`--select-fields` is best for direct fields on the root object. `--field-paths` is better when you want one or two concrete nested values such as `path.value` without expanding the whole object graph.

### Explain Why An Object Is Still Alive

If one object looks suspicious, use `path2gc` to see the shortest retaining path back to a GC root:

```bash
mat-cli path2gc path/to/heap.hprof --object 0x27c30000 --depth 6 --limit 5
```

Example output:

```text
- java.lang.String @ 0x27c30000  de [Shallow Heap=24 Retained Heap=40]
  - value java.util.Hashtable$Entry @ 0x27c4a938 [Shallow Heap=24 Retained Heap=320]
    - [24] java.util.Hashtable$Entry[95] @ 0x27c41430 [Shallow Heap=392 Retained Heap=8,800]
      - table java.util.Properties @ 0x27c3e438 [Shallow Heap=48 Retained Heap=8,848]
        - props class java.lang.System @ 0x2c59b178 System Class [Shallow Heap=24 Retained Heap=67,312]
```

This is especially useful after `instances` and `inspect-object`, when you know which object matters and want to understand why it is still retained.

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
