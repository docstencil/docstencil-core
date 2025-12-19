---
sidebar_position: 6
---

# Functions & Lambdas

Calling functions and creating anonymous functions.

## Function Calls

Call functions with parentheses:

```
{$format(date, "yyyy-MM-dd")}
{$join(names, ", ")}
{customFunction(arg1, arg2)}
```

Built-in functions are prefixed with `$`. You can also call methods on your data objects.

## Lambda Expressions

Create anonymous functions with arrow syntax:

```
{(x) => x * 2}
{(a, b) => a + b}
{(item) => item.price > 100}
```

## Pipe Chaining

Use the pipe operator `|` to chain function calls. The left side becomes the first argument of the function call on the right:

```
{items | $filter((x) => x.active)}
```

Is equivalent to:

```
{$filter(items, (x) => x.active)}
```

Chain multiple operations:

```
{items
  | $filter((x) => x.active)
  | $map((x) => x.name)
  | $join(", ")}
```

## Higher-Order Functions

Pass lambdas to functions that accept them:

```
{$filter(products, (p) => p.inStock)}
{$map(users, (u) => u.email)}
{$reduce(numbers, (sum, n) => sum + n, 0)}
```

## Closures

Lambdas capture variables from their surrounding scope:

```
{var minPrice = 50}
{products | $filter((p) => p.price >= minPrice)}
```

The lambda remembers `minPrice` even when executed inside `$filter`.
