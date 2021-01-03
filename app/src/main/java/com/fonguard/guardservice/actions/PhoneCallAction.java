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
import android.util.Log;

import com.fonguard.guardservice.rules.RulesManager;
import com.fonguard.guardservice.triggers.Trigger;
import com.fonguard.utils.android.telephony.CallManager;

public class PhoneCallAction implements IAction {
    private static final String LOG_TAG = PhoneCallAction.class.getName();

    private com.fonguard.guardservice.settings.actions.PhoneCallAction mSettings;


    public PhoneCallAction(com.fonguard.guardservice.settings.actions.PhoneCallAction settings) {
        mSettings = settings;
    }


    @Override
    public boolean perform(RulesManager rulesManager, Context context, Trigger source,
                           boolean includePayload, Object payload) {
        if (includePayload && payload != null) {
            Log.w(LOG_TAG, "Payload is non-null but it can't be sent over a call, skipping " +
                    "it");
        }

        Log.i(LOG_TAG, "Performing phone call action \"" + mSettings.Id + "\" (" + source +
                " trigger, include payload [IGNORED]: " + includePayload + ")...");

        CallManager.makeCall(context, mSettings.RecipientPhoneNumber);

        return true;
    }
}
