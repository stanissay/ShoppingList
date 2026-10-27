/*
 * Shopping List For Android and Wear Os
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.shopping.shared

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class SharedSyncService {
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun userId(): Flow<String?> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(l)
        awaitClose { auth.removeAuthStateListener(l) }
    }.distinctUntilChanged()

    private fun <T> DatabaseReference.observe(map: (DataSnapshot) -> T): Flow<T> = callbackFlow {
        val l = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { trySend(map(s)) }
            override fun onCancelled(e: DatabaseError) { close(SyncCancelledException(e.code, e.message)) }
        }
        addValueEventListener(l)
        awaitClose { removeEventListener(l) }
    }

    fun snapshots(): Flow<ListSnapshot> = combine(
        db.getReference("items").observe(::parseItems),
        db.getReference("categories").observe(::parseCategories)
    ) { items, cats -> ListSnapshot(categories = cats, items = items.filter { !it.done }) }

    fun activations(startFrom: Long, myId: String): Flow<Long> = callbackFlow {
        val tracker = ActivationTracker(startFrom, myId)
        val q = db.getReference("items").orderByChild("activatedAt").startAt((startFrom + 1).toDouble())
        val l = object : ChildEventListener {
            private fun handle(s: DataSnapshot) {
                val key = s.key ?: return
                val item = runCatching { s.getValue(ShoppingItem::class.java) }.getOrNull() ?: return
                if (tracker.accept(key, item)) trySend(item.activatedAt!!)
            }
            override fun onChildAdded(s: DataSnapshot, p: String?) = handle(s)
            override fun onChildChanged(s: DataSnapshot, p: String?) = handle(s)
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(e: DatabaseError) { close(SyncCancelledException(e.code, e.message)) }
        }
        q.addChildEventListener(l)
        awaitClose { q.removeEventListener(l) }
    }

    suspend fun markDone(id: String) {
        db.getReference("items/$id/done").setValue(true).await()
    }
}