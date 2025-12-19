---
sidebar_position: 3
---

# Expressions & Literals

Basic values and variable access in templates.

## Literals

Output literal values directly:

```
{42}              // Integer
{3.14}            // Double
{"hello"}         // String
{true}            // Boolean
{false}           // Boolean
{null}            // Null
```

### Quotes & String Escaping

There are four different double quote characters recognized by the templating language: `"`, `”`, `„`, `“`

All four double quote characters can be mixed and matched when defining string literals.

```
{„My start and end quotes are different.“}  // "My start and end quotes are different."
{„He said \"Escape the quotes!\"“}          // 'He said "Escape the quotes!"'
```

## Variables

Reference variables from your data, define new ones, or assign existing ones:

```
{name}
{invoice.total}
{var totalWithTax = invoice.total * 1.2}
{do invoice.total = totalWithTax} // This works for mutable properties or properties with setters.
```

## Property Access

Use dot notation to access nested properties:

```
{person.address.street}
{company.contact.email}
```

This works uniformly for maps, data classes, records, and POJOs with getters.

## Optional Access

Use `.?` for safe navigation that returns `null` instead of throwing when a property doesn't exist:

```
{person.?address.?street}
```

If `address` is null, the entire expression returns `null` rather than failing.

## Truthiness

Values are evaluated as truthy or falsy in conditionals:

**Falsy values:**
- `null`
- `false`
- Empty strings `""`
- Zero numbers (`0`, `0.0`)
- Empty collections

**Truthy values:**
- Everything else
