# MAT CLI Heapdump Command Playbook

## Contents

- Baseline and schemas
- Biggest objects and dominators
- Class-focused drilldown
- Object inspection and value extraction
- Retention paths and merged paths
- Threads, finalizers, and reports
- Comparing two dumps
- Failure handling

## Baseline and Schemas

If `mat-cli` is missing, install it with:

```bash
brew install demogorgon314/mat-cli/mat-cli
```

If Homebrew is unavailable and you are already inside the MAT source tree, fall back to building the standalone CLI from `parent/`.

Use these first:

```bash
mat-cli summary <heap> --format json
mat-cli describe top-consumers --format json
mat-cli schema threads --format json
```

Use `describe` or `schema` when you need to know the stable JSON payload before scripting against a command. The dedicated commands already return stable `mat-cli/v2` envelopes, so prefer them over free-form MAT queries whenever possible.

## Biggest Objects and Dominators

Start with retained heap, not just object count:

```bash
mat-cli top-consumers <heap> --format json --limit 20 --depth 3
mat-cli histogram <heap> --format json --limit 50
```

Interpret the output like this:

- `top-consumers` answers "what dominates retained memory right now?"
- `histogram` answers "which classes are numerous or large in aggregate?"
- `top-consumers.biggestObjects[].objectAddress` is the best bridge into `inspect-object`, `path2gc`, and `show_dominator_tree`.
- `histogram` is a table result. Look for `class_name`, instance count, shallow heap, and retained heap columns.

Use a smaller `--limit` when you only need the top suspects. Increase `--depth` on `top-consumers` when package aggregation matters.

## Class-Focused Drilldown

Use `instances` once a class looks suspicious:

```bash
mat-cli instances <heap> --class com.example.CacheEntry --format json --limit 20
mat-cli instances <heap> --class-regex 'com\\.example\\..*Cache.*' --format json --limit 20
mat-cli instances <heap> --class-contains ThreadLocal --include-subclasses --format json --limit 50
```

Choose the selector deliberately:

- Use `--class` for exact FQCN matches.
- Use `--class-regex` for uncertain package names or multiple related classes.
- Use `--class-contains` for fast discovery when the class name fragment is all you know.
- Use `--include-subclasses` only when the leak might live in subtype instances.

## Object Inspection and Value Extraction

Use `inspect-object` for single-object truth:

```bash
mat-cli inspect-object <heap> --object 0x1234abcd --format json --depth 4 --limit 20
mat-cli inspect-object <heap> --object 0x1234abcd --select-field value --format json --limit 20
mat-cli inspect-object <heap> --object 0x1234abcd --field-path cleaner.offsetMap --format json
mat-cli inspect-object <heap> --object 0x1234abcd --format text --field-path count
```

Use it this way:

- Reach for `--field-path` when the user asks for one concrete nested value.
- Reach for `--select-field` when the root object is just a wrapper and one direct field is the real payload.
- Watch for `_meta.value.kind` previews in JSON. MAT can surface text previews or byte-array previews without walking the whole subtree.
- Use `--show-nulls` only when null references are part of the bug story.

If a field path fails, fix the path instead of assuming the value is null. MAT reports missing fields explicitly.

## Retention Paths and Merged Paths

Use the simplest retention question first:

```bash
mat-cli path2gc <heap> --object 0x1234abcd --format json --depth 8 --limit 20
mat-cli query <heap> --command "show_dominator_tree 0x1234abcd" --format json --limit 20 --depth 4
mat-cli query <heap> --command "merge_shortest_paths -groupby FROM_GC_ROOTS com.example.CacheEntry" --format json --limit 20 --depth 4
```

Pick the command based on the question:

- `path2gc` finds the shortest retention chain for one object.
- `show_dominator_tree` shows the local dominator neighborhood for one suspect.
- `merge_shortest_paths` is useful when many instances of the same class appear to leak through the same retaining structure.

If `path2gc` reports that the object is already a GC root, stop searching for an upstream holder and inspect the root object's own fields, thread ownership, or role.

## Threads, Finalizers, and Reports

Use dedicated thread support first:

```bash
mat-cli threads <heap> --format json --limit 50
mat-cli query <heap> --command "thread_overview" --format json
mat-cli query <heap> --command "finalizer_thread" --format json
mat-cli query <heap> --command "finalizer_thread_locals" --format json
```

Use MAT reports when you need a broader narrative:

```bash
mat-cli query <heap> --command "default_report org.eclipse.mat.api:overview" --format json
mat-cli query <heap> --command "default_report org.eclipse.mat.api:suspects" --format json
mat-cli query <heap> --command "default_report org.eclipse.mat.api:top_components" --format json
```

Remember:

- `threads` and `thread_overview` are best-effort views of heap data, not a runtime thread dump.
- `default_report` commands often return section-style results. Summarize the important sections instead of replaying the full structure.
- Use `list-queries` and `describe-query` before calling obscure MAT query ids.

## Comparing Two Dumps

When you have a baseline and a current dump from comparable workloads, use MAT's compare reports:

```bash
mat-cli query <new-heap> --command "default_report org.eclipse.mat.api:overview2 -params baseline=/abs/path/baseline.hprof" --format json
mat-cli query <new-heap> --command "default_report org.eclipse.mat.api:suspects2 -params baseline=/abs/path/baseline.hprof" --format json
```

Prefer `--command-file` if the baseline path contains spaces or the command grows longer.

## Failure Handling

Use these recovery moves:

- `Unknown MAT query`: run `mat-cli list-queries --format json` and `mat-cli describe-query <id> --format json`.
- OQL syntax problems: move the expression into a UTF-8 file and use `--query-file`.
- MAT query syntax problems: move the command into a UTF-8 file and use `--command-file`.
- Invalid `path2gc` address: verify the address with `mat-cli oql <heap> --query "SELECT * FROM OBJECTS 0x1234abcd" --format json`.
- Too much data: lower `--limit`, lower `--depth`, or pivot from `query` to a dedicated command.
