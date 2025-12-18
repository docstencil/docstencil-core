---
sidebar_position: 4
---

# Operators

Arithmetic, comparison, logical, and special operators.

## Arithmetic

```
{a + b}     // Addition
{a - b}     // Subtraction
{a * b}     // Multiplication
{a / b}     // Division
{a % b}     // Modulo (remainder)
```

## Comparison

```
{a == b}    // Equal
{a != b}    // Not equal
{a < b}     // Less than
{a <= b}    // Less than or equal
{a > b}     // Greater than
{a >= b}    // Greater than or equal
```

## Logical

```
{a and b}   // Logical AND
{a or b}    // Logical OR
{!a}        // Logical NOT
```

## Null Coalescing

Use `??` to provide a default value when an expression is null:

```
{nickname ?? name}
{user.phone ?? "Not provided"}
```

## Pipe Operator

Use `|` to chain function calls, passing the left side as the first argument:

```
{items | $filter((x) => x.active)}
{items | $filter((x) => x.active) | $map((x) => x.name)}
```

## Grouping

Use parentheses to control evaluation order:

```
{(a + b) * c}
{!(active and verified)}
```

## Operator Precedence

From highest to lowest:

1. `()` — Grouping
2. `.` `.?` — Property access
3. `!` — Logical NOT
4. `*` `/` `%` — Multiplication, division, modulo
5. `+` `-` — Addition, subtraction
6. `<` `<=` `>` `>=` — Comparison
7. `==` `!=` — Equality
8. `and` — Logical AND
9. `or` — Logical OR
10. `??` — Null coalescing
11. `|` — Pipe
