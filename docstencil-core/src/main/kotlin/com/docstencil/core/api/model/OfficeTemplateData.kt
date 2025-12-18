package com.docstencil.core.api.model

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.managers.ArchiveFileManager
import com.docstencil.core.managers.CommentManager
import com.docstencil.core.managers.ContentTypesManager
import com.docstencil.core.managers.RelationshipManager
import com.docstencil.core.utils.FileTypeDetector
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * The simple representation of an office document is a `Map<String, ByteArray>`, mapping each
 * filePath inside the archive to the raw data of the file. This class wraps this map.
 *
 * Some internal files are changed repeatedly (e.g., relationships). To not reparse these files
 * repeatedly and to allow for shared helper methods, this wrapper smartly wraps certain files.
 */
class OfficeTemplateData(
    files: Map<String, ByteArray>,
    detectionResult: FileTypeDetector.DetectionResult? = null,
) {
    private val detectionResult = detectionResult ?: FileTypeDetector.detect(files)
    private val newFiles: MutableMap<String, ByteArray> = ConcurrentHashMap(files)
    private val managers: MutableMap<KClass<out ArchiveFileManager>, ArchiveFileManager> =
        ConcurrentHashMap()

    fun getData(): Map<String, ByteArray> {
        for ((_, manager) in managers) {
            if (!manager.isDirty()) continue
            val data = manager.render()
            val path = manager.getFilePath()
            if (data != null) {
                newFiles[path] = data
            } else {
                newFiles.remove(path)
            }
        }
        return newFiles
    }

    fun update(filePath: String, updateFn: (data: ByteArray) -> ByteArray?) {
        invalidateManagerIfNeeded(filePath)

        val currData = newFiles[filePath]
            ?: throw TemplaterException.FatalError("Archive file for update not found: $filePath")
        val newData = updateFn(currData)
        if (newData == null) {
            newFiles.remove(filePath)
        } else {
            newFiles[filePath] = newData
        }
    }

    fun updateIfExists(filePath: String?, updateFn: (data: ByteArray) -> ByteArray?) {
        if (filePath == null) {
            return
        }

        invalidateManagerIfNeeded(filePath)

        if (newFiles.containsKey(filePath)) {
            update(filePath, updateFn)
        }
    }

    fun <T> calcIfExists(filePath: String?, fn: (data: ByteArray) -> T): T? {
        if (filePath == null) {
            return null
        }
        val data = newFiles[filePath] ?: return null
        return fn.invoke(data)
    }

    @Synchronized
    fun insert(filePath: String, data: ByteArray) {
        invalidateManagerIfNeeded(filePath)

        if (newFiles.containsKey(filePath)) {
            throw TemplaterException.FatalError("Archive file to insert already exists: $filePath")
        }
        newFiles[filePath] = data
    }

    @Synchronized
    fun <T : ArchiveFileManager> registerManager(klass: KClass<T>, manager: T) {
        if (managers.containsKey(klass)) {
            throw TemplaterException.FatalError("Manager already registered: ${klass.simpleName}")
        }
        managers[klass] = manager
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ArchiveFileManager> getManager(klass: KClass<T>): T? = managers[klass] as? T

    @Suppress("UNCHECKED_CAST")
    fun <T : ArchiveFileManager> requireManager(klass: KClass<T>): T =
        managers[klass] as? T
            ?: throw TemplaterException.FatalError("Manager not registered: ${klass.simpleName}")

    inline fun <reified T : ArchiveFileManager> requireManager(): T = requireManager(T::class)

    fun getFiles(): Map<String, ByteArray> = newFiles.toMap()

    fun getDetectionResult(): FileTypeDetector.DetectionResult = detectionResult

    fun relationshipManager(): RelationshipManager = requireManager()

    fun commentManager(): CommentManager = requireManager()

    fun contentTypesManager(): ContentTypesManager = requireManager()

    private fun invalidateManagerIfNeeded(filePath: String) {
        val toInvalidate = managers.entries.filter { it.value.getFilePath() == filePath }
        for ((klass, manager) in toInvalidate) {
            val rendered = manager.render()
            if (rendered != null) {
                newFiles[filePath] = rendered
            }
            managers.remove(klass)
        }
    }
}
