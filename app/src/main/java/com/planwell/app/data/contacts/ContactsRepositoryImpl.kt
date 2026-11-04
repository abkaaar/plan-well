package com.planwell.app.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.core.content.ContextCompat
import com.planwell.app.domain.model.Contact
import com.planwell.app.domain.repository.ContactsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ContactsRepository {

    override suspend fun search(query: String): List<Contact> = withContext(Dispatchers.IO) {
        if (!hasReadContactsPermission()) return@withContext emptyList()
        queryContacts(query.trim())
    }

    override suspend fun byId(id: String): Contact? = withContext(Dispatchers.IO) {
        if (!hasReadContactsPermission()) return@withContext null
        queryContacts(query = null, contactId = id).firstOrNull()
    }

    private fun hasReadContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

    private fun queryContacts(query: String?, contactId: String? = null): List<Contact> {
        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (contactId != null) {
            selectionParts += "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?"
            selectionArgs += contactId
        }
        if (!query.isNullOrBlank()) {
            selectionParts +=
                "(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ? OR " +
                    "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?)"
            selectionArgs += "%$query%"
            selectionArgs += "%$query%"
        }

        val selection = selectionParts.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
        val args = selectionArgs.takeIf { it.isNotEmpty() }?.toTypedArray()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PROJECTION,
            selection,
            args,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        ) ?: return emptyList()

        return cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx =
                c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val numberIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx =
                c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

            val seenByContact = mutableMapOf<String, MutableSet<String>>()
            val results = mutableListOf<Contact>()

            while (c.moveToNext()) {
                val rawNumber = c.getString(numberIdx)?.trim().orEmpty()
                if (rawNumber.isEmpty()) continue

                val normalized = normalizeNumber(rawNumber) ?: continue
                val id = c.getLong(idIdx).toString()
                val seen = seenByContact.getOrPut(id) { mutableSetOf() }
                if (!seen.add(normalized)) continue

                results += Contact(
                    id = id,
                    displayName = c.getString(nameIdx)?.trim().orEmpty().ifBlank { normalized },
                    phoneNumber = normalized,
                    photoUri = c.getString(photoIdx),
                )
            }
            results
        }
    }

    private fun normalizeNumber(raw: String): String? {
        val region = Locale.getDefault().country.takeIf { it.isNotBlank() } ?: "US"
        val e164 = PhoneNumberUtils.formatNumberToE164(raw, region)
        if (!e164.isNullOrBlank()) return e164
        val digits = raw.filter { it.isDigit() || it == '+' }
        return digits.takeIf { it.length >= 3 }
    }

    companion object {
        private val PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
        )
    }
}
