---
sidebar_position: 11
---

# Images

:::info Preview Feature
This module is currently in preview. [Request access](https://docs.google.com/forms/d/e/1FAIpQLSfhK4WRdPvCurk1B3MI4t1vDgfZg4GIaet8Y7LfypOqSovW_w/viewform?usp=dialog) to try it out.
:::

Inserting images into documents using the docstencil-docx-pro module.

## Adding the Module

```java
OfficeTemplateOptions options = new OfficeTemplateOptions()
    .addModule(new ImageModule());
```

```kotlin
val options = OfficeTemplateOptions()
    .addModule(ImageModule())
```

## Inline Images

Use `$image` to insert an image inline with text:

```
Product: {insert $image(product.photo)}[img]{end}
```

The placeholder text (`[img]`) is replaced by the image.

## Block Images

Use `$imageBlock` to insert a standalone image on its own line:

```
{insert $imageBlock(report.chart)}[chart]{end}
```

## Image Data Sources

The argument to `$image` / `$imageBlock` is either a raw byte array
(`byte[]` / `ByteArray`) or a `TemplateImage` (see [Sizing](#sizing) below).
DocStencil does not load files or URLs for you — read the bytes yourself and
put them in your template context:

```kotlin
val data = mapOf(
    "photo" to File("photo.jpg").readBytes()
)
```

```java
Map<String, Object> data = Map.of(
    "photo", Files.readAllBytes(Path.of("photo.jpg"))
);
```

When you pass raw bytes, the image dimensions are detected automatically (and
capped at 640px wide). To use a remote image, fetch its bytes with your HTTP
client of choice and pass the resulting `byte[]`.

## Sizing

To control dimensions, wrap the bytes in a `TemplateImage` using
`TemplateImage.create(...)` instead of passing raw bytes. `TemplateImage` lives
in `com.docstencil.docx.pro.modules.image.model`.

```kotlin
val data = mapOf(
    "photo" to TemplateImage.create(
        File("photo.jpg").readBytes(),
        widthPx = 200,   // width in pixels
        heightPx = 150,  // height in pixels
    )
)
```

```java
Map<String, Object> data = Map.of(
    "photo", TemplateImage.create(
        Files.readAllBytes(Path.of("photo.jpg")),
        200,   // width in pixels
        150    // height in pixels
    )
);
```

If only one dimension is specified, the other scales proportionally to preserve
aspect ratio. To set the width only, pass just `widthPx` (Kotlin) or call
`TemplateImage.create(bytes, 200)` (Java). To set the height only, pass `null`
for the width — `TemplateImage.create(bytes, null, 150)`.

:::note
`create` also accepts an optional fourth argument, `maxWidthPx`, which caps the
rendered width (default `640`). Both Kotlin and Java can omit any trailing
arguments they don't need.
:::

## Conditional Images

Combine with conditionals:

```
{if product.hasPhoto}
{insert $image(product.photo)}[img]{end}
{end}
```

## Images in Loops

Generate multiple images from a collection:

```
{for item in gallery}
{insert $imageBlock(item.image)}[image]{end}
{item.caption}
{end}
```
