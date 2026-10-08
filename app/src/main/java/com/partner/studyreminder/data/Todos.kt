package com.partner.studyreminder.data

import android.content.Context
import java.io.File

object Todos {
    @Volatile
    private var repository: TodoRepository? = null

    fun of(context: Context): TodoRepository {
        repository?.let { return it }
        return synchronized(this) {
            repository ?: TodoRepository(
                File(context.applicationContext.filesDir, "todos.json"),
            ).also { repository = it }
        }
    }
}
