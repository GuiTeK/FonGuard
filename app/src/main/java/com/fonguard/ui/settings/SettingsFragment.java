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
package com.fonguard.ui.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.fonguard.Preferences;
import com.fonguard.R;
import com.fonguard.guardservice.settings.Settings;
import com.fonguard.utils.java.io.File;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;

import static android.app.Activity.RESULT_OK;
import static com.fonguard.MainActivity.REQUEST_CODE_SETTINGS_EXPORT_FILE_DIALOG;
import static com.fonguard.MainActivity.REQUEST_CODE_SETTINGS_IMPORT_FILE_DIALOG;

public class SettingsFragment extends Fragment {
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container,
                false);
        Button importButton = root.findViewById(R.id.fragment_settings_import_button);
        Button exportButton = root.findViewById(R.id.fragment_settings_export_button);

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent()
                        .setType("application/json")
                        .setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        getString(R.string.fragment_settings_import_dialog_title)),
                        REQUEST_CODE_SETTINGS_IMPORT_FILE_DIALOG);
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent()
                        .setType("application/json")
                        .setAction(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent,
                        getString(R.string.fragment_settings_export_dialog_title)),
                        REQUEST_CODE_SETTINGS_EXPORT_FILE_DIALOG);
            }
        });

        return root;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SETTINGS_IMPORT_FILE_DIALOG) {
            if (resultCode == RESULT_OK) {
                importSettings(data.getData());
            }
        } else if (requestCode == REQUEST_CODE_SETTINGS_EXPORT_FILE_DIALOG) {
            exportSettings(data.getData());
        }
    }


    private void importSettings(Uri settingsUri) {
        try {
            String settingsJson = File.readAllText(settingsUri, getContext().getContentResolver());
            Gson gson = new GsonBuilder().create();
            Settings settings = gson.fromJson(settingsJson, Settings.class);

            // TODO: validate settings are correct (e.g. a rule's action is of shape TRIGGER:ID,
            //  IDs are unique, etc.) so there are no exceptions later on when using the settings.

            Preferences.getInstance(getContext()).setSettings(settings);

            Toast.makeText(getContext(), R.string.fragment_settings_import_success_toast_text,
                    Toast.LENGTH_LONG).show();
        }

        catch (MalformedJsonException | JsonSyntaxException ex) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.fragment_settings_import_error_title)
                    .setMessage(R.string.fragment_settings_import_error_message_json)
                    .setPositiveButton(R.string.fragment_settings_import_error_positive_button_text,
                            null)
                    .create()
                    .show();

            ex.printStackTrace();
        }

        catch (IOException ex) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.fragment_settings_import_error_title)
                    .setMessage(R.string.fragment_settings_import_error_message_unknown)
                    .setPositiveButton(R.string.fragment_settings_import_error_positive_button_text,
                            null)
                    .create()
                    .show();

            ex.printStackTrace();
        }
    }

    private void exportSettings(Uri settingsUri) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Settings settings = Preferences.getInstance(getContext()).getSettings();
        String settingsJson = gson.toJson(settings);

        try {
            File.writeAllText(settingsUri, getContext().getContentResolver(), settingsJson);
            Toast.makeText(getContext(), R.string.fragment_settings_export_success_toast_text,
                    Toast.LENGTH_LONG).show();
        } catch (IOException ex) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.fragment_settings_export_error_title)
                    .setMessage(R.string.fragment_settings_export_error_message_unknown)
                    .setPositiveButton(R.string.fragment_settings_export_error_positive_button_text,
                            null)
                    .create()
                    .show();

            ex.printStackTrace();
        }
    }
}
