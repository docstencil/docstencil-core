package com.docstencil.core.modules

import com.docstencil.core.api.model.OfficeTemplateData
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.utils.XmlNamespaceEnsurer

/**
 * Module that ensures main document XML files have proper namespace declarations
 * on their root elements.
 *
 * Only applies to main Word document files (document.xml, headers, footers, etc.),
 * not to metadata files like docProps/app.xml.
 */
class NamespaceEnsurerModule(
    private val fileTypeConfig: FileTypeConfig,
    private val targetFiles: List<String>,
) : TemplateModule {
    override fun postRender(files: OfficeTemplateData) {
        if (fileTypeConfig.defaultNamespaces.isEmpty()) {
            return
        }

        for (filePath in targetFiles) {
            // Only process Word document files, not metadata files
            if (!filePath.startsWith("word/")) {
                continue
            }

            files.update(filePath) { data ->
                val xmlContent = data.toString(Charsets.UTF_8)
                val xmlWithNamespaces = XmlNamespaceEnsurer.ensure(
                    xmlContent,
                    fileTypeConfig.defaultNamespaces,
                )
                xmlWithNamespaces.toByteArray(Charsets.UTF_8)
            }
        }
    }
}
