package com.abumuhab.tuneplayer.repositories

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.abumuhab.tuneplayer.models.Audio

class AudioRepository(private val application: Application) {
    fun listAudioFiles(): MutableList<Audio> {
        val collection = MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Artists.ARTIST
        )

        val selection = "${MediaStore.Audio.Media.MIME_TYPE} = ? OR ${MediaStore.Audio.Media.MIME_TYPE} = ? "
        val selectionArgs = arrayOf("audio/mp4", "audio/mpeg") // TODO 张靓颖 《U&I》（m4a文件）放到了/sdcard/Music目录下，可以扫出来
        val sortOrder = MediaStore.Audio.Media.DISPLAY_NAME


        val query =
            application.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

        val audios = mutableListOf<Audio>()

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artisteColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)

            /**
             * iterate through query to get details of each audio retrieved
             */
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val artiste = cursor.getString(artisteColumn) ?: "artiste null"
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                audios.add(Audio(name, id.toString(), contentUri, artiste))
            }
        }

        return audios
    }
}