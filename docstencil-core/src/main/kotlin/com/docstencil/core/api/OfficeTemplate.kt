package com.docstencil.core.api

import com.docstencil.core.api.model.OfficeTemplateData
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.render.Globals
import com.docstencil.core.render.TemplateArchiveRepairer
import com.docstencil.core.utils.FileTypeDetector
import com.docstencil.core.utils.ZipUtils
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Main entry point for generating Office documents from templates.
 *
 * Use the factory methods ([fromFile], [fromBytes], [fromInputStream], [fromResource])
 * to load a template, then call [render] to produce the output document.
 *
 * Example usage:
 * ```kotlin
 * val template = OfficeTemplate.fromFile("template.docx")
 * val result = template.render(mapOf("name" to "John", "items" to listOf("A", "B", "C")))
 * result.writeToFile("output.docx")
 * ```
 *
 * Currently only supports DOCX files.
 *
 * @see OfficeTemplateOptions for configuration options
 * @see OfficeTemplateResult for handling the rendered output
 */
class OfficeTemplate private constructor(
    private val files: MutableMap<String, ByteArray>,
    private val fileTypeConfig: FileTypeConfig,
    private val detectionResult: FileTypeDetector.DetectionResult,
    private val options: OfficeTemplateOptions = OfficeTemplateOptions(),
) {
    private val templateArchiveRepairer = TemplateArchiveRepairer()

    companion object {
        /**
         * Creates a template from raw bytes of a DOCX file.
         *
         * @param zipData The binary content of the DOCX file.
         * @param options Configuration options for template processing.
         * @return A new [OfficeTemplate] instance ready for rendering.
         * @throws UnsupportedOperationException if the file type is not supported.
         */
        @JvmStatic
        @JvmOverloads
        fun fromBytes(
            zipData: ByteArray,
            options: OfficeTemplateOptions = OfficeTemplateOptions(),
        ): OfficeTemplate {
            val files = ZipUtils.readFile(zipData)

            val fileTypeConfig = FileTypeConfig.docx()
            val detectionResult = FileTypeDetector.detect(files, fileTypeConfig.fallbackPaths)

            val fileType = detectionResult.fileType
            if (fileType != OfficeFileType.DOCX) {
                throw UnsupportedOperationException(
                    "File type '$fileType' is not yet supported. " +
                            "Only file type 'docx' is supported at the moment.",
                )
            }

            return OfficeTemplate(
                files = files,
                fileTypeConfig = fileTypeConfig,
                detectionResult = detectionResult,
                options = options,
            )
        }

        /**
         * Creates a template from a file path.
         *
         * @param path The path to the DOCX file.
         * @param options Configuration options for template processing.
         * @return A new [OfficeTemplate] instance ready for rendering.
         */
        @JvmStatic
        @JvmOverloads
        fun fromFile(
            path: String,
            options: OfficeTemplateOptions = OfficeTemplateOptions(),
        ): OfficeTemplate {
            return fromFile(Paths.get(path), options)
        }

        /**
         * Creates a template from a [Path].
         *
         * @param path The path to the DOCX file.
         * @param options Configuration options for template processing.
         * @return A new [OfficeTemplate] instance ready for rendering.
         */
        @JvmStatic
        @JvmOverloads
        fun fromFile(
            path: Path,
            options: OfficeTemplateOptions = OfficeTemplateOptions(),
        ): OfficeTemplate {
            return fromBytes(Files.readAllBytes(path), options)
        }

        /**
         * Creates a template from an [InputStream].
         *
         * The stream is fully read and can be closed after this method returns.
         *
         * @param inputStream The input stream containing the DOCX file data.
         * @param options Configuration options for template processing.
         * @return A new [OfficeTemplate] instance ready for rendering.
         */
        @JvmStatic
        @JvmOverloads
        fun fromInputStream(
            inputStream: InputStream,
            options: OfficeTemplateOptions = OfficeTemplateOptions(),
        ): OfficeTemplate {
            return fromBytes(inputStream.readBytes(), options)
        }

        /**
         * Creates a template from a classpath resource.
         *
         * @param resourcePath The path to the resource (e.g., "templates/invoice.docx").
         * @param options Configuration options for template processing.
         * @return A new [OfficeTemplate] instance ready for rendering.
         * @throws IllegalArgumentException if the resource is not found.
         */
        @JvmStatic
        @JvmOverloads
        fun fromResource(
            resourcePath: String,
            options: OfficeTemplateOptions = OfficeTemplateOptions(),
        ): OfficeTemplate {
            val classLoader =
                Thread.currentThread().contextClassLoader ?: OfficeTemplate::class.java.classLoader
            val inputStream = classLoader.getResourceAsStream(resourcePath)
                ?: throw IllegalArgumentException("Resource not found: $resourcePath")
            return inputStream.use { fromInputStream(it, options) }
        }
    }

    /**
     * Renders the template with the provided data.
     *
     * Template expressions like `{name}` or `{items}` are replaced with values from the data map.
     * The data map can contain strings, numbers, booleans, lists, nested maps, classes, records,
     * data classes, functions, and any combination thereof.
     *
     * @param data A map of variable names to values for template substitution.
     * @return An [OfficeTemplateResult] containing the rendered document bytes and any errors.
     */
    fun render(data: Map<String, Any>): OfficeTemplateResult {
        val globals = Globals.builder(options, fileTypeConfig, detectionResult.targetFiles).build()

        val newFiles = OfficeTemplateData(files, detectionResult)
        val errors = mutableListOf<TemplaterException>()

        for (module in globals.modules) {
            module.registerManagers(newFiles)
        }

        for (module in globals.modules) {
            module.preRender(newFiles)
        }

        for (filePath in detectionResult.targetFiles) {
            try {
                newFiles.update(filePath) {
                    OfficeFileTemplate(
                        xmlContent = it.toString(Charsets.UTF_8),
                        globals = globals,
                        fileTypeConfig = fileTypeConfig,
                        filePath = filePath,
                        delimiters = options.delimiters,
                        stripInvalidXmlChars = options.stripInvalidXmlChars,
                    )
                        .render(data).toByteArray(Charsets.UTF_8)
                }
            } catch (e: TemplaterException) {
                if (options.printExceptionStackTraces) {
                    e.printStackTrace()
                }
                errors.add(e)
                globals.errorLocationModule.registerError(e, filePath)
            }
        }

        for (module in globals.modules) {
            module.postRender(newFiles)
        }

        val zipBytes = ZipUtils.writeFile(newFiles.getData(), options.compressionLevel)
        val fixedZipBytes = templateArchiveRepairer.repair(zipBytes)

        return OfficeTemplateResult(fixedZipBytes, errors)
    }
}
