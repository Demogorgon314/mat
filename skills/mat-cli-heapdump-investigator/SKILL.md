---
name: mat-cli-heapdump-investigator
description: Investigate Java heap dumps with Eclipse MAT CLI. Use when Codex needs to analyze `.hprof`, `.phd`, or companion javacore-backed heap dumps to diagnose memory leaks, find the biggest retained objects or dominators, inspect object field values for a class instance, trace paths to GC roots, review thread state or stack snippets available in the dump, or run MAT reports and OQL queries for deeper heap forensics.
---

# MAT CLI Heapdump Investigator

## Overview

Use `mat-cli` as the first-line tool for Java heap-dump triage. Prefer the dedicated CLI commands with stable JSON contracts, then escalate to `query` or `oql` only when a direct command cannot answer the question cleanly.

## Start Here

- Confirm `mat-cli` is reachable with `mat-cli --help`.
- Prefer `--format json` while reasoning, then rerun one or two focused commands with `--format text` when you need readable excerpts for the user.
- Start broad and narrow gradually: `summary` -> `top-consumers` or `histogram` -> `instances` -> `inspect-object` -> `path2gc`.
- Use absolute heap paths and keep every suspect object's `0x...` address in your notes.
- Keep companion files beside the heap when possible. For OpenJ9 `.phd` dumps, a nearby `javacore` can improve thread analysis.
- Use `--query-file` or `--command-file` for long expressions, embedded quotes, regexes, baseline paths, or inner class names containing `$`.
- If `mat-cli` is not installed, install it first with `brew install demogorgon314/mat-cli/mat-cli`. If Homebrew is unavailable but the current workspace is the MAT source tree, build it from the `parent/` module with `mvn clean package -DskipTests -Dmat-product=mat-cli`.

## Core Workflow

1. Establish the snapshot baseline.

```bash
mat-cli summary <heap> --format json
mat-cli threads <heap> --format json --limit 20
```

Use `summary` for heap format, object count, class count, and used heap. Use `threads` early when the leak might be thread-local, blocked, or finalizer-related. Remember that thread output is best-effort from the heap dump, not a full `jstack`.

2. Identify dominant retainers and the biggest objects.

```bash
mat-cli top-consumers <heap> --format json --limit 20 --depth 3
mat-cli histogram <heap> --format json --limit 50
```

Use `top-consumers` to find the biggest retained objects, classes, class loaders, and packages. Use `histogram` to spot explosive class counts and approximate retained sizes. Keep the `objectAddress` values from `biggestObjects`, `classes`, and `classLoaders` for follow-up drills.

3. Narrow to a suspect class or instance set.

```bash
mat-cli instances <heap> --class com.example.CacheEntry --format json --limit 50
mat-cli instances <heap> --class-regex 'com\\.example\\..*Cache.*' --format json --limit 50
mat-cli instances <heap> --class-contains ThreadLocal --include-subclasses --format json --limit 50
```

Use `instances` when the user names a class or when `histogram` points at one. Prefer `--class` for exact matches. Use regex or contains matching only when the package or suffix is uncertain. Add `--include-subclasses` only when inheritance matters.

4. Inspect concrete object state and field values.

```bash
mat-cli inspect-object <heap> --object 0x1234abcd --format json --depth 4 --limit 20
mat-cli inspect-object <heap> --object 0x1234abcd --select-field value --format json --limit 20
mat-cli inspect-object <heap> --object 0x1234abcd --field-path cleaner.offsetMap --format json
```

Use `inspect-object` to answer questions like "what value is stored in this object's fields?" Prefer `--field-path` when you need one nested value quickly. Prefer `--select-field` when one direct field is the real payload. Increase `--depth` carefully; the default is intentionally conservative. Use `--show-nulls` only when missing references are relevant to the diagnosis.

5. Explain why a suspect survives GC.

```bash
mat-cli path2gc <heap> --object 0x1234abcd --format json --depth 8 --limit 20
mat-cli query <heap> --command "show_dominator_tree 0x1234abcd" --format json --limit 20 --depth 4
```

Use `path2gc` for the shortest retaining path to GC roots. Use `show_dominator_tree` when `path2gc` is too narrow and you need local dominator context. If `path2gc` says the object is already a GC root, say that explicitly and pivot back to `inspect-object`, `threads`, or a dominator query.

6. Run advanced MAT queries only after the direct commands.

```bash
mat-cli list-queries --format json
mat-cli describe-query thread_overview --format json
mat-cli query <heap> --command "thread_overview" --format json
mat-cli query <heap> --command "finalizer_thread" --format json
mat-cli query <heap> --command "default_report org.eclipse.mat.api:suspects" --format json
```

Use `query` for registered MAT reports and analyses that do not have dedicated CLI wrappers. Inspect the query id first if you are unsure about syntax or arguments.

## Reporting Rules

- Separate shallow heap from retained heap.
- Quote exact class names and object addresses.
- Treat histogram retained sizes as approximate unless the command guarantees otherwise.
- Tie each leak suspect to at least two pieces of evidence, such as a top-consumers row, a retained path, a field value, a thread, or a MAT report section.
- Call out uncertainty clearly when stack frames are missing or when the dump only offers best-effort thread metadata.
- Prefer a short evidence chain over dumping raw JSON or huge tables back to the user.

## References

- Read [references/command-playbook.md](references/command-playbook.md) for ready-to-run commands and advanced MAT query patterns.
- Read [references/oql-recipes.md](references/oql-recipes.md) when built-in commands are not enough and you need validated OQL snippets.
