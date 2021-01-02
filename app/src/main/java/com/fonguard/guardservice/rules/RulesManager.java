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
package com.fonguard.guardservice.rules;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.fonguard.Preferences;
import com.fonguard.guardservice.actions.Action;
import com.fonguard.guardservice.actions.AwsS3Action;
import com.fonguard.guardservice.actions.HttpAction;
import com.fonguard.guardservice.actions.IAction;
import com.fonguard.guardservice.actions.PhoneMmsAction;
import com.fonguard.guardservice.settings.rules.Rule;
import com.fonguard.guardservice.triggers.Trigger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RulesManager {
    private static final String LOG_TAG = RulesManager.class.getName();

    private static RulesManager sInstance;

    private final ScheduledExecutorService mExecutorService;
    private final Preferences mPreferences;
    private Map<String, Long> mRulesCooldowns;


    public static RulesManager getInstance(Preferences preferences) {
        if (sInstance == null) {
            synchronized (RulesManager.class) {
                if (sInstance == null) {
                    sInstance = new RulesManager(preferences);
                }
            }
        }

        return sInstance;
    }


    private RulesManager(Preferences preferences) {
        mExecutorService =
                Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        mPreferences = preferences;
        mRulesCooldowns = new HashMap<>();
    }


    public void performActionsAsync(Trigger source, Object payload, Context context) {
        Map<Rule, IAction> actionsToPerform = getActionsToPerform(source);

        for (Map.Entry<Rule, IAction> actionToPerform : actionsToPerform.entrySet()) {
            final Rule rule = actionToPerform.getKey();
            final IAction action = actionToPerform.getValue();

            if (isCooldownOver(rule.Id)) {
                mRulesCooldowns.put(rule.Id, System.currentTimeMillis() + rule.CooldownMs);
                performActionAsync(source, payload, context, action, rule, 1, rule.Retries);
            } else {
                Log.v(LOG_TAG, "Cooldown for rule \"" + rule.Id + "\" is not over yet, " +
                        "skipping action");
            }
        }
    }

    private void performActionAsync(final Trigger source, final Object payload, Context context,
                                    final IAction action, final Rule rule, final int tries,
                                    final int maxRetries) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                boolean success = action.perform(RulesManager.this, context, source,
                        rule.IncludePayload, payload);

                if (!success && tries - 1 < maxRetries) {
                    mExecutorService.schedule(new Runnable() {
                        @Override
                        public void run() {
                            performActionAsync(source, payload, context, action, rule,
                                    tries + 1, maxRetries);
                        }
                    }, rule.RetryDelayMs, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    private Map<Rule, IAction> getActionsToPerform(Trigger trigger) {
        Map<Rule, IAction> actions = new HashMap<>();

        for (Rule rule : mPreferences.getRules()) {
            Trigger ruleTrigger = Trigger.valueOf(rule.Trigger.toUpperCase());
            String[] ruleActionParts = rule.Action.split(":");
            String ruleActionType = ruleActionParts[0].toUpperCase();
            String ruleActionId = ruleActionParts[1];
            Action ruleAction = Action.valueOf(ruleActionType);
            IAction action;

            if (ruleTrigger != trigger) {
                continue;
            }

            switch (ruleAction) {
                case HTTP:
                    action = createHttpAction(ruleActionId);
                    break;
                case AWS_S3:
                    action = createAwsS3Action(ruleActionId);
                    break;
                case PHONE_MMS:
                    action = createPhoneMmsAction(ruleActionId);
                    break;
                default:
                    Log.w(LOG_TAG, "Unsupported action type " + ruleActionType +
                            ", skipping rule");
                    continue;
            }

            if (action == null) {
                Log.w(LOG_TAG, "Action " + ruleActionType + ":" + ruleActionId +
                        " not found, skipping rule");
                continue;
            }

            actions.put(rule, action);
        }

        return actions;
    }


    private boolean isCooldownOver(String ruleId) {
        if (mRulesCooldowns.containsKey(ruleId)) {
            return mRulesCooldowns.get(ruleId) <= System.currentTimeMillis();
        }

        return true;
    }


    private HttpAction createHttpAction(String id) {
        List<com.fonguard.guardservice.settings.actions.HttpAction> actions =
                mPreferences.getHttpActions();

        for (com.fonguard.guardservice.settings.actions.HttpAction action : actions) {
            if (action.Id.equals(id)) {
                return new HttpAction(action);
            }
        }

        return null;
    }

    private AwsS3Action createAwsS3Action(String id) {
        List<com.fonguard.guardservice.settings.actions.AwsS3Action> actions =
                mPreferences.getAwsS3Actions();

        for (com.fonguard.guardservice.settings.actions.AwsS3Action action : actions) {
            if (action.Id.equals(id)) {
                return new AwsS3Action(action);
            }
        }

        return null;
    }

    private PhoneMmsAction createPhoneMmsAction(String id) {
        List<com.fonguard.guardservice.settings.actions.PhoneMmsAction> actions =
                mPreferences.getPhoneMmsActions();

        for (com.fonguard.guardservice.settings.actions.PhoneMmsAction action : actions) {
            if (action.Id.equals(id)) {
                return new PhoneMmsAction(action);
            }
        }

        return null;
    }


    public byte[] getBytesFromPayloadObject(Object payload) {
        byte[] buffer = null;

        if (payload instanceof Bitmap) {
            Bitmap bitmap = (Bitmap)payload;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            buffer = outputStream.toByteArray();

            try {
                outputStream.close();
            } catch (IOException ex) {
                Log.w(LOG_TAG, "Could not close Bitmap outputStream: " + ex.getMessage());
            }
        } else {
            Log.w(LOG_TAG, "RulesManager.getBytesFromPayloadObject(): unsupported payload " +
                    "object type " + payload.getClass().getName());
        }

        return buffer;
    }
}
