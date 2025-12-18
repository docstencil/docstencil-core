package com.docstencil.core.modules

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.api.model.OfficeTemplateData
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.render.Globals

/**
 * A `TemplateModule` does post-processing for non-local changes.
 *
 * Example: The user does `{insert fetchImage(url)}`. The initial call to `fetchImage` would create
 * the appropriate XML in the primary XML file. It will also register the fetched image with the
 * fictional `ImageModule`. During post-processing the module would add the actual image file to the
 * docx archive.
 */
interface TemplateModule {
    fun registerGlobals(
        builder: Globals.Builder,
        options: OfficeTemplateOptions,
        config: FileTypeConfig,
    ) = Unit

    fun registerManagers(files: OfficeTemplateData) = Unit
    fun preRender(files: OfficeTemplateData) = Unit
    fun postRender(files: OfficeTemplateData) = Unit
}
