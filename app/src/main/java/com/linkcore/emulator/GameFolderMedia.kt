package com.linkcore.emulator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log

/** Prevent Android galleries from indexing assets in a user-selected game folder. */
internal object GameFolderMedia {
    fun ensureHidden(context: Context, tree: Uri) {
        // Some older selections persisted read-only even though the picker offered write.
        if (!context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission }) {
            try {
                context.contentResolver.takePersistableUriPermission(tree,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } catch (_: SecurityException) {
                return // The user must reselect a folder if Android requires a new grant.
            }
        }
        try {
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
            context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(0) == ".nomedia") return
                }
            } ?: return
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
            DocumentsContract.createDocument(context.contentResolver, root, "application/octet-stream", ".nomedia")
                ?: Log.w("BrumaGallery", "Folder provider did not create .nomedia")
        } catch (e: Exception) {
            // Read-only or unsupported providers must not interrupt game discovery.
            Log.w("BrumaGallery", "Unable to hide game assets from gallery", e)
        }
    }
}