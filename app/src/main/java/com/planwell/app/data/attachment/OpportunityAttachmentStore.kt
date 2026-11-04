package com.planwell.app.data.attachment

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores opportunity binary attachments under app-private files, separate from Room metadata.
 * Relative paths are persisted in `opportunity_attachments.relative_path`.
 */
interface OpportunityAttachmentStore {
    /** Root directory for opportunity files (e.g. filesDir/opportunities). */
    fun rootDir(): File

    fun resolve(relativePath: String): File

    /**
     * Copies [input] into `opportunities/{opportunityId}/{uuid}_{safeName}` and
     * returns the relative path to store in the database.
     */
    fun save(
        opportunityId: Long,
        displayName: String?,
        input: InputStream,
    ): String

    fun delete(relativePath: String)

    fun deleteAllForOpportunity(opportunityId: Long)
}

@Singleton
class FileOpportunityAttachmentStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : OpportunityAttachmentStore {

    override fun rootDir(): File =
        File(context.filesDir, ROOT_DIR_NAME).also { it.mkdirs() }

    override fun resolve(relativePath: String): File =
        File(rootDir(), relativePath)

    override fun save(
        opportunityId: Long,
        displayName: String?,
        input: InputStream,
    ): String {
        val dir = File(rootDir(), opportunityId.toString()).also { it.mkdirs() }
        val safeName = (displayName ?: "attachment")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(80)
        val relative = "$opportunityId/${UUID.randomUUID()}_$safeName"
        val target = File(rootDir(), relative)
        input.use { src ->
            target.outputStream().use { dst -> src.copyTo(dst) }
        }
        return relative
    }

    override fun delete(relativePath: String) {
        val file = resolve(relativePath)
        if (file.exists()) {
            file.delete()
        }
    }

    override fun deleteAllForOpportunity(opportunityId: Long) {
        val dir = File(rootDir(), opportunityId.toString())
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    companion object {
        const val ROOT_DIR_NAME = "opportunities"
    }
}
