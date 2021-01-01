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
package com.fonguard.guardservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RestartReceiver extends BroadcastReceiver {
    public static final String INTENT_ACTION_RESTART_GUARD_SERVICE =
            "com.fonguard.intent.action.RESTART_GUARD_SERVICE";
    private static final String TAG = RestartReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "broadcast received");

        if (intent.getAction() == null) {
            throw new IllegalArgumentException("RestartReceiver received a null intent action");
        }

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i(TAG, "system boot completed, starting GuardService...");
            GuardService.actionService(GuardService.ServiceAction.START, context);
        } else if (intent.getAction().equals(INTENT_ACTION_RESTART_GUARD_SERVICE)) {
            Log.i(TAG, "GuardService was killed by system, restarting it...");
            GuardService.actionService(GuardService.ServiceAction.START, context);
        } else {
            throw new IllegalArgumentException("RestartReceiver received an unknown intent action");
        }
    }
}
