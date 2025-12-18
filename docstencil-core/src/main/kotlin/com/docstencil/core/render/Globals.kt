package com.docstencil.core.render

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.modules.CoreModule
import com.docstencil.core.modules.ErrorLocationModule
import com.docstencil.core.modules.NamespaceEnsurerModule
import com.docstencil.core.modules.TemplateModule
import com.docstencil.core.modules.hyperlink.HyperlinkModule
import com.docstencil.core.rewrite.XmlStreamRewriterFactory
import com.docstencil.core.scanner.model.XmlInputToken

/**
 * This is a wrapper around the global environment that contains all global functions.
 *
 * We need this wrapper because some global functions are not stateless, but need to register data
 * with a `TemplateModule`, which handles post-processing.
 * Post-processing takes care of non-local changes.
 *
 * Example: A hyperlink is not just an XML tag, but it requires an additional entry into the
 * relationships file.
 */
class Globals private constructor(
    val env: Environment,
    val errorLocationModule: ErrorLocationModule,
    val modules: List<TemplateModule>,
    val rewriteFnsDefaultTargets: Map<String, XmlInputToken.ExpansionTarget>,
    val parallelRendering: Boolean,
) {
    class Builder(
        private val parallelRendering: Boolean = false,
    ) {
        private var env: Environment = Environment()
        private val rewriteFnsDefaultTargets: MutableMap<String, XmlInputToken.ExpansionTarget> =
            mutableMapOf()
        private val modules: MutableList<TemplateModule> = mutableListOf()

        fun build(): Globals {
            val errorLocationModule =
                modules.firstOrNull { it is ErrorLocationModule } as ErrorLocationModule?
            if (errorLocationModule == null) {
                throw TemplaterException.FatalError("No error location module registered.")
            }
            return Globals(
                env,
                errorLocationModule,
                modules,
                rewriteFnsDefaultTargets,
                parallelRendering,
            )
        }

        fun registerGlobal(name: String, value: Any?): Builder {
            env.define(name, value)
            return this
        }

        fun registerRewriter(name: String, value: XmlStreamRewriterFactory): Builder {
            registerGlobal(name, value.getRewriterFactoryFn())
            rewriteFnsDefaultTargets[name] = value.defaultTarget()
            return this
        }

        fun registerModule(module: TemplateModule): Builder {
            modules.add(module)
            return this
        }
    }

    companion object {
        fun builder(
            options: OfficeTemplateOptions,
            config: FileTypeConfig,
            targetFiles: List<String>,
        ): Builder {
            val builder = Builder(options.parallelRendering)

            val coreModule = CoreModule()
            val errorLocationModule = ErrorLocationModule()
            val hyperlinkModule = HyperlinkModule()
            val namespaceEnsurerModule = NamespaceEnsurerModule(config, targetFiles)

            val allModules = listOf(coreModule, errorLocationModule, hyperlinkModule) +
                    options.modules +
                    listOf(namespaceEnsurerModule)

            for (module in allModules) {
                module.registerGlobals(builder, options, config)
                builder.registerModule(module)
            }

            return builder
        }
    }
}
