package com.planwell.app.domain.repository

import com.planwell.app.domain.model.Contact

interface ContactsRepository {
    suspend fun search(query: String): List<Contact>
    suspend fun byId(id: String): Contact?
}
