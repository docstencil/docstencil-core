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
    .withModule(new ImageModule());
```

```kotlin
val options = OfficeTemplateOptions()
    .withModule(ImageModule())
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

Provide image data in your template context:

```kotlin
val data = mapOf(
    "photo" to File("photo.jpg").readBytes(),           // byte array
    "logo" to ImageModule.fromFile("logo.png"),         // from file path
    "chart" to ImageModule.fromUrl("https://...")       // from URL
)
```

```java
Map<String, Object> data = Map.of(
    "photo", Files.readAllBytes(Path.of("photo.jpg")),  // byte array
    "logo", ImageModule.fromFile("logo.png"),           // from file path
    "chart", ImageModule.fromUrl("https://...")         // from URL
);
```

## Sizing

Specify dimensions with the image data:

```kotlin
val data = mapOf(
    "photo" to ImageModule.fromFile("photo.jpg")
        .withWidth(200)      // width in pixels
        .withHeight(150)     // height in pixels
)
```

If only one dimension is specified, the other scales proportionally to preserve aspect ratio.

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
