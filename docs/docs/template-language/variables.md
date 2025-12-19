---
sidebar_position: 5
---

# Variables & Assignment

Declaring and modifying variables in templates.

## Variable Declaration

Use `var` to declare a new variable:

```
{var total = 0}
{var greeting = "Hello, " + name}
```

## Assignment

Use `do` to assign a new value to an existing variable:

```
{do total = total + item.price}
{do counter = counter + 1}
```

The `do` keyword executes the expression without producing output.

## Property Assignment

Assign values to object properties:

```
{do person.nickname = "Bob"}
{do settings.theme = "dark"}
```

This works with:
- Object properties (calls setter if available)
- Map entries

## Side-Effect Statements

Use `do` to execute any expression without output:

```
{do $log(debugInfo)}
{do processItem(item)}
```

## Scoping Rules

Variables follow block scope and lexical scoping:

```
{var x = 1}
{if condition}
  {var x = 2}    // New variable, shadows outer x
  {x}            // Outputs: 2
{end}
{x}              // Outputs: 1
```

Variables declared inside a block are not visible outside it.

## Closures

Lambdas capture variables from their enclosing scope:

```
{var multiplier = 2}
{items | $map((x) => x * multiplier)}
```

The lambda can access `multiplier` even when executed later.
