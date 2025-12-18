---
sidebar_position: 2
---

# Control Flow

Conditional rendering and loops in templates.

## Conditionals

Use `{if}...{end}` to conditionally render content:

```
{if user.premium}
Premium member benefits apply.
{end}
```

With else:

```
{if order.shipped}
Your order is on the way!
{else}
Your order is being processed.
{end}
```

## For Loops

Use `{for ... in ...}...{end}` to iterate over collections:

```
{for item in items}
- {item.name}: {item.price}
{end}
```

### Loop Context

Loops automatically repeat the containing element:
- **Inline:** Loop repeats within the paragraph
- **Paragraph:** Loop repeats the entire paragraph
- **Table row:** Loop repeats the entire row

Table example:

```
| Name | Price |
|------|-------|
| {for item in items}{item.name} | {item.price}{end} |
```

## Case Expressions

Use `case` for multi-branch conditionals:

```
{case
  when status == "pending" then "Awaiting review"
  when status == "approved" then "Ready to ship"
  when status == "shipped" then "On the way"
  else "Unknown"
end}
```

Single-line form:

```
{case when score >= 90 then "A" when score >= 80 then "B" else "C" end}
```

## Element Targeting with @

Use `@"tag"` to specify which XML element a loop or conditional should repeat:

```
{@"w:tc" for cell in row.cells}
{cell.value}
{end}
```

This targets the table cell (`w:tc`) element, useful when the default element detection isn't what you need.
