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
package com.fonguard.guardservice.settings.actions;

import com.google.gson.annotations.SerializedName;

public class AwsS3Action {
    @SerializedName("id")
    public String Id;

    @SerializedName("aws_region")
    public String AwsRegion;

    @SerializedName("aws_access_key_id")
    public String AwsAccessKeyId;

    @SerializedName("aws_secret_access_key")
    public String AwsSecretAccessKey;

    @SerializedName("bucket_name")
    public String BucketName;

    @SerializedName("key_prefix")
    public String KeyPrefix;
}
