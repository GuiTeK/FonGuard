/*
 * FonGuard
 * Copyright (C) 2021  Guillaume TRUCHOT <guillaume.truchot@outlook.com>
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fonguard.guardservice.actions;

import android.content.Context;
import android.graphics.Bitmap;
import android.telephony.SmsManager;
import android.util.Log;

import com.fonguard.guardservice.rules.RulesManager;
import com.fonguard.guardservice.triggers.Trigger;
import com.fonguard.utils.android.telephony.MmsManager;

public class PhoneSmsAction implements IAction {
    private static final String LOG_TAG = PhoneSmsAction.class.getName();

    private com.fonguard.guardservice.settings.actions.PhoneSmsAction mSettings;


    public PhoneSmsAction(com.fonguard.guardservice.settings.actions.PhoneSmsAction settings) {
        mSettings = settings;
    }


    @Override
    public boolean perform(RulesManager rulesManager, Context context, Trigger source,
                           boolean includePayload, Object payload) {
        SmsManager smsManager = SmsManager.getDefault();

        if (includePayload && payload != null) {
            Log.w(LOG_TAG, "Payload is non-null but it can't be sent in an SMS, skipping it");
        }

        Log.i(LOG_TAG, "Performing SMS action \"" + mSettings.Id + "\" (" + source +
                " trigger, include payload [IGNORED]: " + includePayload + ")...");

        smsManager.sendTextMessage(mSettings.RecipientPhoneNumber, null,
                "A " + source.name() + " was detected", null, null);

        return true;
    }
}
