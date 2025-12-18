package com.docstencil.core.render

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

class TemplateArchiveRepairer {
    fun repair(zipBytes: ByteArray): ByteArray {
        return fixDrawingIds(zipBytes)
    }

    private fun fixDrawingIds(zipBytes: ByteArray): ByteArray {
        try {
            val entries = mutableMapOf<String, ByteArray>()
            val zipInput = ZipInputStream(ByteArrayInputStream(zipBytes))
            var entry: ZipEntry? = zipInput.nextEntry

            while (entry != null) {
                val content = zipInput.readBytes()
                entries[entry.name] = content
                entry = zipInput.nextEntry
            }
            zipInput.close()

            var prId = 1

            // Process each XML file
            val processedEntries = entries.mapValues { (filename, content) ->
                if (!filename.endsWith(".xml")) {
                    return@mapValues content
                }

                try {
                    val factory = DocumentBuilderFactory.newInstance()
                    factory.isNamespaceAware = true
                    val docBuilder = factory.newDocumentBuilder()
                    val doc = docBuilder.parse(ByteArrayInputStream(content))

                    // Find all wp:docPr elements (drawing properties), which contain the id
                    // attribute that must be unique.
                    val docPrElements = doc.getElementsByTagNameNS(
                        "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing",
                        "docPr",
                    )

                    for (i in 0 until docPrElements.length) {
                        val element = docPrElements.item(i)
                        if (element != null && element is org.w3c.dom.Element) {
                            element.setAttribute("id", prId.toString())
                            prId++
                        }
                    }

                    val transformer =
                        javax.xml.transform.TransformerFactory.newInstance().newTransformer()
                    transformer.setOutputProperty(
                        javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION,
                        "yes",
                    )
                    val writer = java.io.StringWriter()
                    transformer.transform(
                        javax.xml.transform.dom.DOMSource(doc),
                        javax.xml.transform.stream.StreamResult(writer),
                    )

                    writer.toString().toByteArray(Charsets.UTF_8)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If parsing fails, return original content.
                    content
                }
            }

            // Repackage into ZIP
            val output = ByteArrayOutputStream()
            val zipOutput = ZipOutputStream(output)

            for ((filename, fileContent) in processedEntries) {
                zipOutput.putNextEntry(ZipEntry(filename))
                zipOutput.write(fileContent)
                zipOutput.closeEntry()
            }

            zipOutput.close()
            return output.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            // If anything fails, return original bytes. It is better to have a document with
            // duplicate IDs than no document.
            return zipBytes
        }
    }
}
