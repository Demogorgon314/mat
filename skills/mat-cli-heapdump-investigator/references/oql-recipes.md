# MAT CLI Heapdump OQL Recipes

Use these only after the dedicated commands fail to answer the question cleanly. For one object and one field, `inspect-object` is usually simpler and safer than OQL.

## Useful Pseudo Fields

MAT OQL exposes metadata fields that are useful in leak analysis:

- `@objectAddress`
- `@usedHeapSize`
- `@retainedHeapSize`
- `@name`

Use them to connect OQL results back to `inspect-object`, `path2gc`, or `top-consumers`.

## List Objects with Address and Readable Value

Validated in the CLI tests:

```sql
select s.@objectAddress as ADDRESS, toString(s) as VALUE
from java.lang.String s
```

Run it with:

```bash
mat-cli oql <heap> --query-file strings.oql --format json --limit 20
```

Use this pattern when you need both an address and a human-readable rendering.

## Jump to One Object by Address

```sql
SELECT * FROM OBJECTS 0x1234abcd
```

Use this to verify that an address from another command is valid before running `path2gc` or a more specific OQL expression.

## Find Large or Suspicious String Objects

Validated in the OQL tests:

```sql
SELECT * FROM java.lang.String s
WHERE s.count > 100 AND s.@retainedHeapSize > s.@usedHeapSize
```

Use this pattern to find payload-heavy strings whose retention looks disproportionate.

## Filter by Rendered Value

Validated in the OQL tests:

```sql
SELECT * FROM java.lang.String s
WHERE toString(s) LIKE "java.*"
```

This is useful when class identity is not enough and the content matters.

## Ask for Dominators from OQL

Validated in the OQL tests:

```sql
select objects dominators(s)
from objects 0x1234abcd s
```

Use this when you need a quick dominator set in OQL form. For user-facing explanations, `path2gc` or `show_dominator_tree` is often easier to interpret.

## Collapse to Distinct Classes

Validated in the OQL tests:

```sql
select distinct objects classof(s)
from java.lang.String s
```

Use this pattern when a broader instance query returns mixed runtime types and you want the concrete classes involved.

## Inspect Map-Like Structures

Validated in the OQL tests:

```sql
SELECT m AS map,
       classof(m).@name AS type,
       m[0:-1].size() AS size
FROM INSTANCEOF java.util.AbstractMap m
```

Use this to find oversized maps before drilling into their entries with `inspect-object` or a more specific OQL query.

## Quoting Guidance

- Prefer `--query-file` for multi-line OQL.
- Prefer `--query-file` when the expression contains `$`, regexes, nested quotes, or long baseline-dependent fragments.
- If MAT parses the query but execution fails, narrow the scope instead of making the expression more complex immediately.
