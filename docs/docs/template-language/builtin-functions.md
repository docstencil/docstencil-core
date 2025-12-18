---
sidebar_position: 7
---

# Built-in Functions

Reference for all `$`-prefixed built-in functions.

## Formatting

### $format

Format dates and numbers with patterns:

```
{$format(date, "yyyy-MM-dd")}        // 2024-03-15
{$format(date, "MMMM dd, yyyy")}     // March 15, 2024
{$format(number, "0.00")}            // 3.14
{$format(number, "#,##0.00")}        // 1,234.56
```

Uses Java's [DateTimeFormatter](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) for dates and [DecimalFormat](https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html) for numbers.

### $formatDate

Format dates explicitly:

```
{$formatDate(invoice.date, "MMMM dd, yyyy")}
```

### $formatNumber

Format numbers explicitly:

```
{$formatNumber(price, "#,##0.00")}
```

## Collections

### $filter

Filter items by a predicate:

```
{$filter(items, (x) => x.active)}
{items | $filter((x) => x.price > 100)}
```

### $map

Transform each item:

```
{$map(users, (u) => u.name)}
{users | $map((u) => u.email)}
```

### $reduce

Reduce a collection to a single value:

```
{$reduce(numbers, (sum, n) => sum + n, 0)}
{items | $reduce((total, item) => total + item.price, 0)}
```

### $sum

Sum up numbers:

```
{$sum(numbers)}
{items | $sum()}
```

### $filterNotNull

Remove null values from a collection:

```
{$filterNotNull(items)}
```

### $enumerate

Wrap items with index and position info:

```
{for item in $enumerate(items)}
  {item.index}: {item.value.name}
{end}
```

Each wrapped item has:
- `value` — the original item
- `index` — zero-based position (0, 1, 2, ...)
- `isFirst` / `isLast` — boolean flags
- `isEven` / `isOdd` — boolean flags based on index

### $reverse

Reverse a collection:

```
{$reverse(items)}
```

### $range

Generate a sequence of numbers:

```
{$range(5)}         // 0, 1, 2, 3, 4
{$range(1, 5)}      // 1, 2, 3, 4
```

### $join

Join items with a separator:

```
{$join(names, ", ")}              // Alice, Bob, Charlie
{names | $join(" | ")}            // Alice | Bob | Charlie
```

## Type Casting

Convert values between numeric types:

```
{$asByte(value)}
{$asShort(value)}
{$asInt(value)}
{$asLong(value)}
{$asFloat(value)}
{$asDouble(value)}
```

## Utilities

### $now

Get the current date and time:

```
{$format($now(), "yyyy-MM-dd HH:mm")}
```

### $notNull

Check if a value is not null (returns boolean):

```
{if $notNull(user.email)}
  Email: {user.email}
{end}
```
