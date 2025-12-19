package com.docstencil.core.api

import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.parser.TemplateParser
import com.docstencil.core.render.*
import com.docstencil.core.scanner.*


internal class OfficeFileTemplate(
    private val xmlContent: String,
    private val globals: Globals,
    private val fileTypeConfig: FileTypeConfig,
    private val filePath: String,
    private val delimiters: OfficeTemplateOptions.Delimiters,
    private val stripInvalidXmlChars: Boolean,
) {
    fun render(data: Map<String, Any>): String {
        val rawInputTokens =
            RawXmlScanner(delimiters, fileTypeConfig.tagsText.toSet()).scan(xmlContent)
        val tokenGroups = TemplateTokenGroupScanner(delimiters).scan(rawInputTokens)
        val inputTokens =
            TemplateXmlTokenMerger(fileTypeConfig.tagNicknames).merge(rawInputTokens, tokenGroups)
        val fixedInputTokens = TemplateTokenGroupFixer().fix(inputTokens)
        val expandedInputTokens = TemplateXmlTokenExpander(
            fileTypeConfig.expansionRules,
            fileTypeConfig.defaultTagRawXmlExpansion,
            globals.rewriteFnsDefaultTargets,
        ).expand(fixedInputTokens)
        val tokens = TemplateTokenConverter().convert(expandedInputTokens)
        val program = TemplateParser().parse(tokens)

        val renderedXmlTokens =
            Renderer(globals.env, globals.parallelRendering).render(program, data)

        val cleanRenderedXmlTokens =
            TemplateOutputCleaner(fileTypeConfig.tagsDeletable.toSet()).clean(renderedXmlTokens)

        val repairedTokens = TemplateFileRepairer(
            fileType = fileTypeConfig.fileType,
            repairRules = fileTypeConfig.repairRules,
        ).repair(cleanRenderedXmlTokens)

        val output = TemplateOutputJoiner(stripInvalidXmlChars).join(repairedTokens)

        return output
    }
}
