package com.docstencil.core.scanner

import com.docstencil.core.scanner.model.TemplateTokenGroup
import com.docstencil.core.scanner.model.TemplateTokenType

class TemplateTokenGroupHelper {
    fun isInsertStatement(group: TemplateTokenGroup): Boolean {
        val tokens = group.tokens

        val basicInsert = tokens.size >= 3 &&
                tokens[0].type == TemplateTokenType.DELIMITER_OPEN &&
                tokens[1].type == TemplateTokenType.INSERT

        val prefixInsert = tokens.size >= 5 &&
                tokens[0].type == TemplateTokenType.DELIMITER_OPEN &&
                tokens[1].type == TemplateTokenType.AT &&
                tokens[2].type == TemplateTokenType.STRING &&
                tokens[3].type == TemplateTokenType.INSERT

        return basicInsert || prefixInsert
    }
}
