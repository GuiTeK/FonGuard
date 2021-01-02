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
import android.util.Log;

import com.fonguard.guardservice.rules.RulesManager;
import com.fonguard.guardservice.triggers.Trigger;
import com.fonguard.utils.android.telephony.MmsManager;

public class PhoneMmsAction implements IAction {
    private static final String LOG_TAG = PhoneMmsAction.class.getName();

    private com.fonguard.guardservice.settings.actions.PhoneMmsAction mSettings;


    public PhoneMmsAction(com.fonguard.guardservice.settings.actions.PhoneMmsAction settings) {
        mSettings = settings;
    }


    @Override
    public boolean perform(RulesManager rulesManager, Context context, Trigger source,
                           boolean includePayload, Object payload) {
        Bitmap img = null;

        if (includePayload && payload != null) {
            if (payload instanceof Bitmap) {
                img = (Bitmap)payload;
            } else {
                Log.w(LOG_TAG, "Unsupported MMS payload type " +
                        payload.getClass().getName() + ", skipping it");
            }
        }

        Log.i(LOG_TAG, "Performing MMS action \"" +
                mSettings.Id + "\" (" + source + " trigger, include payload: " + includePayload +
                ")...");

        MmsManager.sendMmsAsync(context, mSettings.RecipientPhoneNumber,
                "A " + source.name() + " was detected", img);

        return true;
    }
}
