---
sidebar_position: 4
---

# Performance

DocStencil is an in-process JVM library. Rendering happens entirely in memory inside your application. There is no server, no queue, and no network round-trip. This page reports measured throughput for representative templates and explains how performance scales.

## Headline numbers

| Scenario | Output size | Median time per document | Throughput |
|---|---|---|---|
| Minimal template (framework overhead floor) | 11 KB | 2.8 ms | ~350 docs/sec per core |
| One-page invoice (loops, calculations, date/currency formatting) | 26 KB | 6 ms | ~165 docs/sec per core (~10,000/min) |
| One-page invoice, rendered concurrently on 16 cores | 26 KB | n/a | ~1,400 docs/sec (~84,000/min) |
| Large generated report (3,905 sections, ~19,500 paragraphs, ~11,700 tables) | 484 KB | 1.7 s | ~34 docs/min per core |

Measured on an AMD Ryzen 9 9950X (16 cores / 32 threads) running Linux, on Eclipse Temurin JDK 8, the *minimum* Java version DocStencil supports. Newer JVMs are typically faster.

## What one render includes

Every number above covers the **full pipeline** for a document: reading the DOCX archive, parsing the template expressions, rendering with the provided data, and writing a valid DOCX archive back out. There is no hidden warm cache of compiled templates behind these figures.

## Documents per minute from a single template

A template is loaded once (`OfficeTemplate.fromBytes`) and can then be rendered any number of times with different data. Rendering is CPU-bound and stateless, so throughput scales linearly with cores. It also scales horizontally with application instances, since there is no shared state or external service.

For capacity planning: a typical one-to-few-page business document costs **single-digit milliseconds per document per core**. A single mid-range 8-core server conservatively renders several hundred such documents per second.

## Many different templates

DocStencil has no separate template-compilation step to amortize: template expressions are parsed as part of every render, and that parse is included in all per-document times above. Loading a *different* template instead of reusing one adds only the cost of reading its ZIP archive, measured at **well under 1 ms** for the templates above (within measurement noise).

Practically, this means throughput across N distinct templates is the same as rendering N documents from one template. There is no per-template setup penalty to plan around.

## Framework overhead vs. template cost

The minimal-template scenario measures DocStencil's fixed overhead: ~2.8 ms to unzip, parse, render, and rezip a document with almost no content. Everything beyond that floor scales with the *content volume* of your template and data: the number of paragraphs, table rows, and expressions actually produced. A 484 KB report with ~19,500 paragraphs and ~11,700 tables takes about 1.7 seconds; a one-page invoice takes 6 ms.

This is why the most meaningful benchmark is your own template. The benchmark suite is part of the open-source repository and takes one command to run against any template.

## Reproducing these numbers

The benchmarks are ordinary tests in the public [docstencil-core repository](https://github.com/docstencil/docstencil-core):

```bash
./gradlew :docstencil-core:test --tests "*ThroughputBenchmarkTest*"
```

Results are printed as `BENCH |` lines in the test output (captured in the Gradle test report). Each scenario runs a JIT warmup phase followed by up to 300 timed iterations and reports mean, median, and p95 times.
