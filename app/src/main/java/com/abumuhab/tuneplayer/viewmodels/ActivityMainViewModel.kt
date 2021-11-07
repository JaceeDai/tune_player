package com.abumuhab.tuneplayer.viewmodels

import android.app.Application
import android.content.ComponentName
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.abumuhab.tuneplayer.models.Audio
import com.abumuhab.tuneplayer.services.MediaPlaybackService
import com.abumuhab.tuneplayer.util.findItemPositionInList

class ActivityMainViewModel(private val application: Application) : ViewModel() {
    lateinit var mediaBrowser: MediaBrowserCompat
    var mediaController: MediaControllerCompat? = null
    lateinit var connectionCallBack: MediaBrowserCompat.ConnectionCallback
    var queue: MutableList<MediaSessionCompat.QueueItem>? = null

    val showMusicControls = MutableLiveData<Boolean>()
    var nowPlaying = MutableLiveData<Audio>()
    var isPaused = MutableLiveData<Boolean>()
    var nowPlayingProgress = MutableLiveData<Float>()


    init {
//        showMusicControls.value = false
        connectToMediaPlaybackService()
    }

    fun skipToNext() {
        mediaController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        mediaController?.transportControls?.skipToPrevious()
    }

    fun pausePLay() {
        if (isPaused.value == false) {
            mediaController?.transportControls?.pause()
        } else {
            mediaController?.transportControls?.play()
        }
    }

    private fun connectToMediaPlaybackService() {
        connectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                mediaBrowser.sessionToken.also { token ->
                    mediaController =
                        MediaControllerCompat(application.applicationContext, token)
                    mediaController!!.registerCallback(object : MediaControllerCompat.Callback() {
                        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                            val bundle = state!!.extras
                            isPaused.value =
                                (state.playbackState as PlaybackState).state == PlaybackStateCompat.STATE_PAUSED

                            /**
                             * When the queue is null, this signifies that an already existing
                             * existing connection to the service was running before user closed app.
                             * Hence, get the current playing or paused mediaItem data from the state extras and refetch queue
                             */
                            if (showMusicControls.value == null && queue == null) {
                                showMusicControls.value = true
                                val audio = Audio(
                                    bundle!!.getString("title").toString(),
                                    bundle.getString("id").toString(),
                                    Uri.parse(bundle.getString("uri").toString()),
                                    bundle.getString("subtitle").toString()
                                )
                                nowPlaying.value = audio

                                //TODO: fetch queue
                            }

                            /**
                             * Compute progress percentage
                             */
                            nowPlayingProgress.value =
                                state.position.toFloat() / bundle!!.getInt("duration").toFloat()

                            queue?.let { queue ->
                                state.activeQueueItemId
                                val nowPlayingQueuePosition =
                                    findItemPositionInList(queue) { queueItem ->
                                        state.activeQueueItemId == queueItem.queueId
                                    }

                                val mediaDescription =
                                    queue[nowPlayingQueuePosition].description

                                val audio = Audio(
                                    mediaDescription.title.toString(),
                                    mediaDescription.mediaId.toString(),
                                    mediaDescription.mediaUri!!,
                                    mediaDescription.subtitle.toString()
                                )

                                nowPlaying.value = audio
                            }
                        }

                        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
                            this@ActivityMainViewModel.queue = queue
                        }

                    })
                }
            }
        }
        mediaBrowser = MediaBrowserCompat(
            application.applicationContext,
            ComponentName(application.applicationContext, MediaPlaybackService::class.java),
            connectionCallBack,
            null
        )
        mediaBrowser.connect()
    }
}

class ActivityMainViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityMainViewModel::class.java)) {
            return ActivityMainViewModel(application) as T
        }
        throw  IllegalArgumentException("Unknown ViewModel class")
    }
}