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
package com.fonguard;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.fonguard.guardservice.GuardService;
import com.fonguard.utils.java.Collections;
import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_APP_NEEDED_DANGEROUS_PERMISSIONS = 1;
    public static final int REQUEST_CODE_SETTINGS_IMPORT_FILE_DIALOG = 2;
    public static final int REQUEST_CODE_SETTINGS_EXPORT_FILE_DIALOG = 3;

    public static final String INTENT_ACTION_MOTION_TRIGGER_SET_PREVIEW_SURFACE =
            "com.fonguard.intent.guardservice.triggers.motiontrigger.SET_PREVIEW_SURFACE";
    public static final String INTENT_ACTION_MOTION_TRIGGER_RESTART =
            "com.fonguard.intent.guardservice.triggers.motiontrigger.RESTART";
    public static final String INTENT_ACTION_MOTION_TRIGGER_SET_PIXEL_VALUE_DIFF_THRESHOLD =
            "com.fonguard.intent.guardservice.triggers.motiontrigger.SET_PIXEL_VALUE_DIFF_THRESHOLD";
    public static final String INTENT_ACTION_MOTION_TRIGGER_SET_PIXEL_NUMBER_DIFF_THRESHOLD =
            "com.fonguard.intent.guardservice.triggers.motiontrigger.SET_PIXEL_PERCENTAGE_DIFF_THRESHOLD";
    public static final String INTENT_ACTION_TRIGGERS_MOTION_UPDATE_DIFF_PIXELS_NB =
            "com.fonguard.intent.ui.triggers.motion.triggers.triggersmotionfragment.SET_STATUS";

    private static final String[] APP_NEEDED_DANGEROUS_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SEND_SMS
    };

    private AppBarConfiguration mAppBarConfiguration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each menu should be considered as top level
        // destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.fragment_home)
                .setDrawerLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this,
                R.id.fragment_navigation_host);
        NavigationUI.setupActionBarWithNavController(this, navController,
                mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        requestAppNeededDangerousPermissions();

        GuardService.actionService(GuardService.ServiceAction.START, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this,
                R.id.fragment_navigation_host);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) ||
                super.onSupportNavigateUp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_APP_NEEDED_DANGEROUS_PERMISSIONS:
                if (Collections.intArrayContains(grantResults, PackageManager.PERMISSION_DENIED)) {
                    showAppNeededDangerousPermissionsDeniedDialog();
                }
                break;
            default:
                break;
        }
    }


    private void requestAppNeededDangerousPermissions() {
        ActivityCompat.requestPermissions(this, APP_NEEDED_DANGEROUS_PERMISSIONS,
                REQUEST_CODE_APP_NEEDED_DANGEROUS_PERMISSIONS);
    }

    private void showAppNeededDangerousPermissionsDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.main_activity_permissions_denied_dialog_title))
                .setMessage(getString(R.string.main_activity_permissions_denied_dialog_message))
                .setCancelable(false)
                .setNegativeButton(getString(
                        R.string.main_activity_permissions_denied_dialog_button_negative_text),
                        null)
                .setPositiveButton(getString(
                        R.string.main_activity_permissions_denied_dialog_button_positive_text),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.requestAppNeededDangerousPermissions();
                    }
                })
                .create()
                .show();
    }


    private void showFatalAlertDialog(String errorMessage) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.main_activity_fatal_error_dialog_title)
                .setMessage(getString(R.string.main_activity_fatal_error_dialog_message,
                        errorMessage))
                .setPositiveButton(R.string.main_activity_fatal_error_dialog_button_text,
                        (dialog, which) -> finishAndRemoveTask())
                .create()
                .show();
    }
}
