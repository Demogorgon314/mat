---
name: mat-cli
description: Investigate Java heap dumps with Eclipse MAT CLI. Use when Codex needs to triage `.hprof` or `.phd` dumps, find dominant retainers or exploding classes, inspect object fields or nested values, trace paths to GC roots, review heap-derived thread state, run MAT reports, or escalate to OQL for targeted heap forensics.
---

# Heap Dump Analysis with mat-cli

Use `mat-cli` as the first-line tool for Java heap-dump triage. Prefer the dedicated CLI commands with readable Markdown output, then escalate to `query` or `oql` only when a direct command cannot answer the question cleanly.

If `mat-cli` is unavailable, install it with `brew install demogorgon314/mat-cli/mat-cli`. Inside the MAT source tree, you can also build it from `parent/` with `mvn clean package -DskipTests -Dmat-product=mat-cli`.

## Quick start

- Confirm `mat-cli` is reachable with `mat-cli --help`.
- Prefer `--format markdown` for agent-facing work. Use `--format text` only when you explicitly want the raw terminal layout.
- Use absolute heap paths and keep every suspect object's `0x...` address in your notes.
- Keep companion files beside the heap when possible. For OpenJ9 `.phd` dumps, a nearby `javacore` can improve thread analysis.
- Use `--query-file` or `--command-file` for long expressions, embedded quotes, regexes, baseline paths, or inner class names containing `$`.

```bash
mat-cli summary <heap> --format markdown
mat-cli biggest-objects <heap> --format markdown --limit 20 --depth 3
mat-cli objects <heap> --by class --format markdown --limit 30
mat-cli instances <heap> --class com.example.CacheEntry --format markdown --limit 20
mat-cli inspect-object <heap> --object 0x1234abcd --field-paths value --format markdown
mat-cli path2gc <heap> --object 0x1234abcd --format markdown --depth 8 --limit 20
```

If the leak might be thread-local, blocked, or finalizer-related, insert `mat-cli threads <heap> --format markdown --limit 20` right after `summary`.

## Command map

### Baseline

```bash
mat-cli summary <heap> --format markdown
mat-cli threads <heap> --format markdown --limit 20
```

Use `summary` for heap format, object count, class count, and used heap. Use `threads` early when the leak might be thread-local, blocked, or finalizer-related. Thread output is best-effort heap metadata, not a live `jstack`.

### Dominators

```bash
mat-cli biggest-objects <heap> --format markdown --limit 20 --depth 3
mat-cli objects <heap> --by class --format markdown --limit 30
mat-cli objects <heap> --by package --format markdown --limit 20
mat-cli objects <heap> --by class-loader --format markdown --limit 20
```

Use `biggest-objects` to find the top retained dominators. Use `objects --by class` for class growth, `objects --by package` for retained package trees, and `objects --by class-loader` for class-loader ownership. Keep the object addresses shown in `biggest-objects` rows for follow-up inspection.

### Class drilldown

```bash
mat-cli instances <heap> --class com.example.CacheEntry --format markdown --limit 20
mat-cli instances <heap> --class-regex 'com\\.example\\..*Cache.*' --format markdown --limit 20
mat-cli instances <heap> --class-contains ThreadLocal --include-subclasses --format markdown --limit 30
```

Use `instances` when the user names a class or when `objects --by class` points at one. Prefer `--class` for exact matches. Use regex or contains matching only when the package or suffix is uncertain. Add `--include-subclasses` only when inheritance matters.

### Object inspection

```bash
mat-cli inspect-object <heap> --object 0x1234abcd --format markdown --depth 4 --limit 20
mat-cli inspect-object <heap> --object 0x1234abcd --select-fields value --format markdown --limit 20
mat-cli inspect-object <heap> --object 0x1234abcd --field-paths cleaner.offsetMap --format markdown
mat-cli inspect-object <heap> --object 0x1234abcd --format markdown --field-paths count
```

Use `inspect-object` when you need concrete field values from one object. Reach for `--select-fields` when direct fields are the payload, `--field-paths` for one or more nested values, and `--show-nulls` only when null references are part of the bug story. Increase `--depth` carefully; the default is intentionally conservative.

### Retention

```bash
mat-cli path2gc <heap> --object 0x1234abcd --format markdown --depth 8 --limit 20
mat-cli query <heap> --command "show_dominator_tree 0x1234abcd" --format markdown --limit 20 --depth 4
mat-cli query <heap> --command "merge_shortest_paths -groupby FROM_GC_ROOTS com.example.CacheEntry" --format markdown --limit 20 --depth 4
```

Use `path2gc` for the shortest retaining path to GC roots. Use `show_dominator_tree` when `path2gc` is too narrow and you need local dominator context. Use `merge_shortest_paths` when many leaking instances appear to flow through one shared retaining structure. If `path2gc` says the object is already a GC root, say that explicitly and pivot back to `inspect-object`, `threads`, or a dominator query.

### Advanced MAT queries

```bash
mat-cli query <heap> --command "thread_overview" --format markdown
mat-cli query <heap> --command "finalizer_thread" --format markdown
mat-cli query <heap> --command "default_report org.eclipse.mat.api:suspects" --format markdown
mat-cli query <heap> --command "default_report org.eclipse.mat.api:overview2 -params baseline=/abs/path/baseline.hprof" --format markdown
mat-cli oql <heap> --query-file suspects.oql --format markdown --limit 20
```

Use `query` for registered MAT reports and analyses that do not have dedicated CLI wrappers. Use `oql` only after `objects`, `biggest-objects`, `instances`, `inspect-object`, or `path2gc` stop being expressive enough. Prefer `--command-file` or `--query-file` when quoting gets awkward.

## Command discovery

```bash
mat-cli describe summary --format markdown
mat-cli describe objects --format markdown
mat-cli schema inspect-object --format markdown
mat-cli list-queries --format markdown
mat-cli describe-query histogram --format markdown
```

Use `describe` and `schema` before depending on a command's output shape in an agent workflow. Use `list-queries` and `describe-query` before reaching for less familiar MAT query ids or arguments. `histogram` is still useful as a MAT query id, but it is no longer the preferred first-line CLI command for class aggregation.

## Reporting rules

- Separate shallow heap from retained heap.
- Quote exact class names and object addresses.
- Treat `objects --by class` and `objects --by class-loader` retained sizes as approximate unless the command guarantees otherwise.
- Tie each leak suspect to at least two pieces of evidence, such as a `biggest-objects` row, an `objects` view, a `path2gc` chain, a field value, a thread entry, or a MAT report section.
- Call out uncertainty clearly when stack frames are missing or when the dump only offers best-effort thread metadata.
- Prefer a short evidence chain over dumping large raw command output or huge tables back to the user.

## Specific tasks

- Ready-to-run command sequences and advanced MAT query patterns: [references/command-playbook.md](references/command-playbook.md)
- Validated OQL snippets when direct commands are not enough: [references/oql-recipes.md](references/oql-recipes.md)
