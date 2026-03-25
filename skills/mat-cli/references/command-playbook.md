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
mat-cli summary <heap> --format markdown
mat-cli describe objects --format markdown
mat-cli schema threads --format markdown
```

Use `describe` or `schema` when you need a readable outline of a command's output shape before chaining commands or handing the result to another agent. Prefer dedicated commands over free-form MAT queries whenever possible.

## Biggest Objects and Dominators

Start with retained heap, not just object count:

```bash
mat-cli biggest-objects <heap> --format markdown --limit 20 --depth 3
mat-cli objects <heap> --by class --format markdown --limit 30
mat-cli objects <heap> --by package --format markdown --limit 20
mat-cli objects <heap> --by class-loader --format markdown --limit 20
```

Interpret the output like this:

- `biggest-objects` answers "what dominates retained memory right now?"
- `objects --by class` answers "which classes are numerous or large in aggregate?"
- `objects --by package` answers "which package tree dominates retained memory?"
- `objects --by class-loader` answers "which loaders own the most retained memory and classes?"
- The object address shown in `biggest-objects` rows is the best bridge into `inspect-object`, `path2gc`, and `show_dominator_tree`.
- `objects --by class` is a table result. Look for `class`, instance count, shallow size, and retained size columns.

Use a smaller `--limit` when you only need the top suspects. Keep `objects --by class` tight enough to stay readable, then rerun with a larger limit only if the tail still looks interesting. Increase `--depth` on `biggest-objects` when you need deeper dominator levels.

## Class-Focused Drilldown

Use `instances` once a class looks suspicious:

```bash
mat-cli instances <heap> --class com.example.CacheEntry --format markdown --limit 20
mat-cli instances <heap> --class-regex 'com\\.example\\..*Cache.*' --format markdown --limit 20
mat-cli instances <heap> --class-contains ThreadLocal --include-subclasses --format markdown --limit 30
```

Choose the selector deliberately:

- Use `--class` for exact FQCN matches.
- Use `--class-regex` for uncertain package names or multiple related classes.
- Use `--class-contains` for fast discovery when the class name fragment is all you know.
- Use `--include-subclasses` only when the leak might live in subtype instances.

## Object Inspection and Value Extraction

Use `inspect-object` for single-object truth:

```bash
mat-cli inspect-object <heap> --object 0x1234abcd --format markdown --depth 4 --limit 20
mat-cli inspect-object <heap> --object 0x1234abcd --select-fields value --format markdown --limit 20
mat-cli inspect-object <heap> --object 0x1234abcd --field-paths cleaner.offsetMap --format markdown
mat-cli inspect-object <heap> --object 0x1234abcd --format markdown --field-paths count
```

Use it this way:

- Reach for `--field-paths` when the user asks for one or more concrete nested values.
- Reach for `--select-fields` when the root object is just a wrapper and one or more direct fields are the real payload.
- Watch for preview-style values in the rendered output. MAT can surface text previews or byte-array previews without walking the whole subtree.
- Use `--show-nulls` only when null references are part of the bug story.

If a field path fails, fix the path instead of assuming the value is null. MAT reports missing fields explicitly.

## Retention Paths and Merged Paths

Use the simplest retention question first:

```bash
mat-cli path2gc <heap> --object 0x1234abcd --format markdown --depth 8 --limit 20
mat-cli query <heap> --command "show_dominator_tree 0x1234abcd" --format markdown --limit 20 --depth 4
mat-cli query <heap> --command "merge_shortest_paths -groupby FROM_GC_ROOTS com.example.CacheEntry" --format markdown --limit 20 --depth 4
```

Pick the command based on the question:

- `path2gc` finds the shortest retention chain for one object.
- `show_dominator_tree` shows the local dominator neighborhood for one suspect.
- `merge_shortest_paths` is useful when many instances of the same class appear to leak through the same retaining structure.

If `path2gc` reports that the object is already a GC root, stop searching for an upstream holder and inspect the root object's own fields, thread ownership, or role.

## Threads, Finalizers, and Reports

Use dedicated thread support first:

```bash
mat-cli threads <heap> --format markdown --limit 20
mat-cli query <heap> --command "thread_overview" --format markdown
mat-cli query <heap> --command "finalizer_thread" --format markdown
mat-cli query <heap> --command "finalizer_thread_locals" --format markdown
```

Use MAT reports when you need a broader narrative:

```bash
mat-cli query <heap> --command "default_report org.eclipse.mat.api:overview" --format markdown
mat-cli query <heap> --command "default_report org.eclipse.mat.api:suspects" --format markdown
mat-cli query <heap> --command "default_report org.eclipse.mat.api:top_components" --format markdown
```

Remember:

- `threads` and `thread_overview` are best-effort views of heap data, not a runtime thread dump.
- `default_report` commands often return section-style results. Summarize the important sections instead of replaying the full structure.
- Use `list-queries` and `describe-query` before calling obscure MAT query ids.

## Comparing Two Dumps

When you have a baseline and a current dump from comparable workloads, use MAT's compare reports:

```bash
mat-cli query <new-heap> --command "default_report org.eclipse.mat.api:overview2 -params baseline=/abs/path/baseline.hprof" --format markdown
mat-cli query <new-heap> --command "default_report org.eclipse.mat.api:suspects2 -params baseline=/abs/path/baseline.hprof" --format markdown
```

Prefer `--command-file` if the baseline path contains spaces or the command grows longer.

## Failure Handling

Use these recovery moves:

- `Unknown MAT query`: run `mat-cli list-queries --format markdown` and `mat-cli describe-query <id> --format markdown`.
- OQL syntax problems: move the expression into a UTF-8 file and use `--query-file`.
- MAT query syntax problems: move the command into a UTF-8 file and use `--command-file`.
- Invalid `path2gc` address: verify the address with `mat-cli oql <heap> --query "SELECT * FROM OBJECTS 0x1234abcd" --format markdown`.
- Too much data: lower `--limit`, lower `--depth`, or pivot from `query` to a dedicated command.
