package com.example.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** A person pickable for a favourite-contact widget slot. */
data class ContactInfo(
    val name: String,
    val number: String,
    val photoUri: Uri?
)

/** Reads contacts from the system provider; only the fields the widgets show. */
object ContactRepository {

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** Contacts starred as favourites in the phone's Contacts app; empty without permission. */
    fun starred(context: Context): List<ContactInfo> = contacts(context, starredOnly = true)

    /** All contacts that have a phone number, sorted by name; empty without permission. */
    fun contacts(context: Context, starredOnly: Boolean = false): List<ContactInfo> {
        if (!hasPermission(context)) return emptyList()
        val out = mutableListOf<ContactInfo>()
        val seen = mutableSetOf<String>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                if (starredOnly) "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1" else null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                while (c.moveToNext()) {
                    val name = c.getString(nameIdx) ?: continue
                    val number = c.getString(numIdx) ?: continue
                    val key = "$name|$number"
                    if (!seen.add(key)) continue
                    val photo = c.getString(photoIdx)?.let(Uri::parse)
                    out += ContactInfo(name, number, photo)
                }
            }
        }
        return out
    }

    /** Look up one contact by number, for showing the current name/photo in a widget. */
    fun byNumber(context: Context, number: String): ContactInfo? {
        if (!hasPermission(context)) return null
        val normalized = number.filter { it.isDigit() }
        return contacts(context).firstOrNull { it.number.filter { ch -> ch.isDigit() } == normalized }
    }
}
