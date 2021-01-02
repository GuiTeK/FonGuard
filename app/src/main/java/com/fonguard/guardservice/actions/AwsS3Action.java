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

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fonguard.guardservice.rules.RulesManager;
import com.fonguard.guardservice.triggers.Trigger;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AwsS3Action implements IAction {
    private static final String LOG_TAG = AwsS3Action.class.getName();

    private com.fonguard.guardservice.settings.actions.AwsS3Action mSettings;


    public AwsS3Action(com.fonguard.guardservice.settings.actions.AwsS3Action settings) {
        mSettings = settings;
    }


    @Override
    public boolean perform(RulesManager rulesManager, Context context, Trigger source,
                           boolean includePayload, Object payload) {
        AWSCredentials credentials = new BasicAWSCredentials(mSettings.AwsAccessKeyId,
                mSettings.AwsSecretAccessKey);
        Region region = Region.getRegion(mSettings.AwsRegion);
        AmazonS3Client s3Client = new AmazonS3Client(credentials, region);
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                Locale.US);
        String dateTime = dateTimeFormat.format(new Date());
        String s3Key = mSettings.KeyPrefix + dateTime + ".jpg";
        byte[] payloadBytes = rulesManager.getBytesFromPayloadObject(payload);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(payloadBytes);

        Log.i(LOG_TAG, "Performing AWS S3 action \"" + mSettings.Id + "\" (" + source +
                " trigger, include payload [IGNORED]: " + includePayload + ")...");

        try {
            s3Client.putObject(mSettings.BucketName, s3Key, inputStream, new ObjectMetadata());
            Log.i(LOG_TAG, "Performed AWS S3 action \"" + mSettings.Id + "\" successfully");
            return true;
        } catch (AmazonClientException ex) {
            Log.e(LOG_TAG, "Failed to perform AWS S3 action \"" + mSettings.Id + "\": " +
                    ex.getMessage());
            return false;
        }
    }
}
