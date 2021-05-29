package com.htetz.mytel;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PER = 1;
    public static final String NOT_FIRST_REQ = "not_first_req",RUNNING = "run",STATUS = "status",NOTI_TITLE="noti_title";
    private static final String[] PERMISSIONS_NEED = {Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_CALL_LOG,Manifest.permission.ANSWER_PHONE_CALLS};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private Preference status;
        private final List<String > permissions = new ArrayList<>();
        private SharedPreferences sharedPreferences;
        private SwitchCompat on_off;
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setHasOptionsMenu(true);
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            sharedPreferences = getPreferenceManager().getSharedPreferences();
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.hasKey()){
                if (preference.getKey().equals(STATUS)){
                    checkRunning();
                }
            }else if (preference.getTitle()!=null){
                if (preference.getTitle().equals(getString(R.string.dev))){
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    try {
                        intent.setData(Uri.parse("fb://profile/100030031876000"));
                        startActivity(intent);
                    } catch (Exception e) {
                        intent.setData(Uri.parse("https://facebook.com/100030031876000"));
                        startActivity(intent);
                    }
                }else if (preference.getTitle().equals(getString(R.string.source_code))){
                    openLink(getString(R.string.source_code_url));
                }
            }
            return super.onPreferenceTreeClick(preference);
        }

        private void openLink(String url){
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
        }

        private boolean checkRunning(){
            if (!checkPermissionZ()){
                requestPermissionZ();
                return false;
            }
            return true;
        }

        private boolean checkPermissionZ(){
            permissions.clear();
            for (String permission:PERMISSIONS_NEED){
                if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(requireActivity(), permission)){
                    permissions.add(permission);
                }
            }
            return permissions.isEmpty();
        }

        private void requestPermissionZ(){
            if (!permissions.isEmpty()){
                StringBuilder temp = new StringBuilder();
                temp.append(getString(R.string.req_permssion_msg));
                for (String s:permissions){
                    if (s.contains("READ_PHONE_STATE")){
                        temp.append("- READ_PHONE_STATE\n").append(getString(R.string.READ_PHONE_STATE));
                    }else if (s.contains("READ_CALL_LOG")){
                        temp.append("- READ_CALL_LOG\n").append(getString(R.string.READ_CALL_LOG));
                    }else if (s.contains("ANSWER_PHONE_CALLS")){
                        temp.append("- ANSWER_PHONE_CALLS\n").append(getString(R.string.ANSWER_PHONE_CALLS));
                    }
                }

                String msg = temp.toString();
                if (msg.contains("- ")){
                    msg = msg.replace("- ","\n\n- ");
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.noti_title)
                        .setMessage(msg)
                        .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (sharedPreferences.getBoolean(NOT_FIRST_REQ,false)) {
                                    for (String s : permissions) {
                                        if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), s)) {
                                            Intent i = new Intent();
                                            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            i.addCategory(Intent.CATEGORY_DEFAULT);
                                            i.setData(Uri.parse("package:" + requireContext().getPackageName()));
                                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                            startActivity(i);
                                            return;
                                        }
                                    }
                                }
                                requestPermissions(Arrays.copyOf(permissions.toArray(),permissions.size(),String[].class), REQ_PER);
                            }
                        })
                        .setNegativeButton(R.string.no,null)
                        .show();
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == REQ_PER) {
                sharedPreferences.edit().putBoolean(NOT_FIRST_REQ,true).apply();
                checkStatus();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            checkStatus();
        }

        private void checkStatus(){
            boolean check = checkPermissionZ();
            if (on_off!=null && check)
                on_off.setChecked(sharedPreferences.getBoolean(RUNNING,false));

            if (status!=null){
                if (check){
                    status.setTitle(getString(R.string.status_title) + " ✅️");
                    status.setSummary(R.string.status_running);
                }else {
                    status.setTitle(getString(R.string.status_title) + " ℹ️");
                    status.setSummary(R.string.status_need_permission);
                }
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            status = findPreference(STATUS);
        }

        @Override
        public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.main,menu);

            on_off = (SwitchCompat) menu.findItem(R.id.app_bar_switch).getActionView();
            on_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    sharedPreferences.edit().putBoolean(RUNNING,isChecked).apply();
                    if (!checkRunning()){
                        on_off.setChecked(false);
                    }
                }
            });

            checkStatus();
        }
    }
}