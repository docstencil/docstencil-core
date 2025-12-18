package com.docstencil.core.modules.hyperlink

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.api.model.OfficeTemplateData
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.managers.RelationshipManager
import com.docstencil.core.modules.TemplateModule
import com.docstencil.core.render.Globals

private const val HYPERLINK_REL_TYPE =
    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink"

class HyperlinkModule : TemplateModule {
    private var relationshipManager: RelationshipManager? = null

    override fun registerGlobals(
        builder: Globals.Builder,
        options: OfficeTemplateOptions,
        config: FileTypeConfig,
    ) {
        builder.registerRewriter($$"$link", HyperlinkRewriter.Factory(this))
    }

    override fun registerManagers(files: OfficeTemplateData) {
        relationshipManager = files.requireManager<RelationshipManager>()
    }

    /**
     * Creates a hyperlink relationship for the given URL and returns the relationship ID.
     *
     * @param url The target URL for the hyperlink
     * @return The relationship ID (e.g., "rId5")
     * @throws TemplaterException.FatalError if called before registerManagers
     */
    fun createHyperlinkRelationship(url: String): String {
        val manager = relationshipManager
            ?: throw TemplaterException.FatalError(
                "HyperlinkModule.createHyperlinkRelationship called before registerManagers hook",
            )

        return manager.ensureRelationship(
            HYPERLINK_REL_TYPE,
            url,
            targetMode = "External",
        )
    }
}
