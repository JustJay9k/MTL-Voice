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
package org.linphone.ui.assistant.model

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import org.linphone.utils.AppUtils
import org.linphone.utils.Event
import org.linphone.R

class ConfirmPhoneNumberDialogModel
    @UiThread
    constructor(phoneNumber: String) {
    val message = MutableLiveData<String>()

    val dismissEvent = MutableLiveData<Event<Boolean>>()

    val confirmPhoneNumberEvent = MutableLiveData<Event<Boolean>>()

    init {
        message.value = AppUtils.getFormattedString(R.string.assistant_dialog_confirm_phone_number_message, phoneNumber)
    }

    @UiThread
    fun dismiss() {
        dismissEvent.value = Event(true)
    }

    @UiThread
    fun confirmPhoneNumber() {
        confirmPhoneNumberEvent.value = Event(true)
    }
}
