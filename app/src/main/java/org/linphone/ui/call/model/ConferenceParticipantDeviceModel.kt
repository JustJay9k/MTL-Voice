/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.ui.call.model

import android.view.TextureView
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.core.MediaDirection
import org.linphone.core.ParticipantDevice
import org.linphone.core.ParticipantDeviceListenerStub
import org.linphone.core.StreamType
import org.linphone.core.tools.Log
import org.linphone.utils.LinphoneUtils

class ConferenceParticipantDeviceModel @WorkerThread constructor(
    val device: ParticipantDevice,
    val isMe: Boolean = false
) {
    companion object {
        private const val TAG = "[Conference Participant Device Model]"
    }

    val avatarModel = coreContext.contactsManager.getContactAvatarModelForAddress(device.address)

    val name = avatarModel.contactName ?: device.name ?: LinphoneUtils.getDisplayName(
        device.address
    )

    val isMuted = MutableLiveData<Boolean>()

    val isSpeaking = MutableLiveData<Boolean>()

    val isActiveSpeaker = MutableLiveData<Boolean>()

    val isVideoAvailable = MutableLiveData<Boolean>()

    val isSendingVideo = MutableLiveData<Boolean>()

    val isJoining = MutableLiveData<Boolean>()

    val isInConference = MutableLiveData<Boolean>()

    private lateinit var textureView: TextureView

    private val deviceListener = object : ParticipantDeviceListenerStub() {
        @WorkerThread
        override fun onStateChanged(
            participantDevice: ParticipantDevice,
            state: ParticipantDevice.State?
        ) {
            Log.i(
                "$TAG Participant device [${participantDevice.address.asStringUriOnly()}] state changed [$state]"
            )
            when (state) {
                ParticipantDevice.State.Joining, ParticipantDevice.State.Alerting -> {
                    isJoining.postValue(true)
                }
                ParticipantDevice.State.OnHold -> {
                    isInConference.postValue(false)
                }
                ParticipantDevice.State.Present -> {
                    isJoining.postValue(false)
                    isInConference.postValue(true)
                }
                else -> {}
            }
        }

        @WorkerThread
        override fun onIsMuted(participantDevice: ParticipantDevice, muted: Boolean) {
            Log.d(
                "$TAG Participant device [${participantDevice.address.asStringUriOnly()}] is ${if (participantDevice.isMuted) "muted" else "no longer muted"}"
            )
            isMuted.postValue(participantDevice.isMuted)
        }

        @WorkerThread
        override fun onIsSpeakingChanged(
            participantDevice: ParticipantDevice,
            speaking: Boolean
        ) {
            Log.d(
                "$TAG Participant device [${participantDevice.address.asStringUriOnly()}] is ${if (participantDevice.isSpeaking) "speaking" else "no longer speaking"}"
            )
            isSpeaking.postValue(speaking)
        }

        @WorkerThread
        override fun onStreamAvailabilityChanged(
            participantDevice: ParticipantDevice,
            available: Boolean,
            streamType: StreamType?
        ) {
            Log.d(
                "$TAG Participant device [${participantDevice.address.asStringUriOnly()}] stream [$streamType] availability changed to ${if (available) "available" else "not available"}"
            )
            if (streamType == StreamType.Video) {
                isVideoAvailable.postValue(available)
                if (available) {
                    updateWindowId(textureView)
                }
            }
        }

        @WorkerThread
        override fun onStreamCapabilityChanged(
            participantDevice: ParticipantDevice,
            direction: MediaDirection?,
            streamType: StreamType?
        ) {
            Log.d(
                "$TAG Participant device [${participantDevice.address.asStringUriOnly()}] stream [$streamType] capability changed to [$direction]"
            )
            if (streamType == StreamType.Video) {
                isSendingVideo.postValue(
                    direction == MediaDirection.SendRecv || direction == MediaDirection.SendOnly
                )
            }
        }
    }

    init {
        device.addListener(deviceListener)

        val state = device.state
        val joining = state == ParticipantDevice.State.Joining || state == ParticipantDevice.State.Alerting
        isJoining.postValue(joining)
        val inConference = device.isInConference
        isInConference.postValue(inConference)
        if (joining) {
            Log.i(
                "$TAG Participant [${device.address.asStringUriOnly()}] is joining the conference (state [$state])"
            )
        } else {
            Log.i(
                "$TAG Participant [${device.address.asStringUriOnly()}] is ${if (inConference) "inside" else "outside"} the conference with state [${device.state}]"
            )
        }

        isMuted.postValue(device.isMuted)
        isSpeaking.postValue(device.isSpeaking)
        isActiveSpeaker.postValue(false)
        Log.i(
            "$TAG Participant [${device.address.asStringUriOnly()}] is in state [${device.state}]"
        )

        isVideoAvailable.postValue(device.getStreamAvailability(StreamType.Video))
        val videoCapability = device.getStreamCapability(StreamType.Video)
        isSendingVideo.postValue(
            videoCapability == MediaDirection.SendRecv || videoCapability == MediaDirection.SendOnly
        )
    }

    @WorkerThread
    fun destroy() {
        device.removeListener(deviceListener)
    }

    @UiThread
    fun setTextureView(view: TextureView) {
        Log.i(
            "$TAG TextureView for participant [${device.address.asStringUriOnly()}] available from UI [$view]"
        )
        textureView = view
        coreContext.postOnCoreThread {
            updateWindowId(textureView)
        }
    }

    @WorkerThread
    private fun updateWindowId(windowId: Any?) {
        Log.i(
            "$$TAG Setting participant [${device.address.asStringUriOnly()}] window ID [$windowId]"
        )
        // SDK does it but it's a bit better this way, prevents going to participants map in PlatformHelper for nothing
        if (isMe) {
            coreContext.core.nativePreviewWindowId = windowId
        } else {
            device.nativeVideoWindowId = windowId
        }
    }
}
