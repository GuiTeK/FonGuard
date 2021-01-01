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
package com.fonguard.guardservice.settings;

import com.fonguard.guardservice.settings.rules.Rule;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Settings {
    @SerializedName("triggers")
    public com.fonguard.guardservice.settings.triggers.Triggers Triggers;

    @SerializedName("actions")
    public com.fonguard.guardservice.settings.actions.Actions Actions;

    @SerializedName("rules")
    public List<Rule> Rules;
}
