package com.docstencil.core.managers

interface ArchiveFileManager {
    fun render(): ByteArray?
    fun getFilePath(): String
    fun isDirty(): Boolean
}
