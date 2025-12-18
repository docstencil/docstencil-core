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
 * Generates an Office document from a template.
 */
class OfficeTemplate private constructor(
    private val files: MutableMap<String, ByteArray>,
    private val fileTypeConfig: FileTypeConfig,
    private val detectionResult: FileTypeDetector.DetectionResult,
    private val options: OfficeTemplateOptions = OfficeTemplateOptions(),
) {
    private val templateArchiveRepairer = TemplateArchiveRepairer()

    companion object {
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

        @JvmStatic
        @JvmOverloads
        fun fromFile(
            path: String,
            options: OfficeTemplateOptions = OfficeTemplateOptions(),
        ): OfficeTemplate {
            return fromFile(Paths.get(path), options)
        }

        @JvmStatic
        @JvmOverloads
        fun fromFile(
            path: Path,
            options: OfficeTemplateOptions = OfficeTemplateOptions(),
        ): OfficeTemplate {
            return fromBytes(Files.readAllBytes(path), options)
        }

        @JvmStatic
        @JvmOverloads
        fun fromInputStream(
            inputStream: InputStream,
            options: OfficeTemplateOptions = OfficeTemplateOptions(),
        ): OfficeTemplate {
            return fromBytes(inputStream.readBytes(), options)
        }

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
