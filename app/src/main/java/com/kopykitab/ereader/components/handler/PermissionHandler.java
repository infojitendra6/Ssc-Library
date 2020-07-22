package com.kopykitab.ereader.components.handler;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHandler {
    private Activity mActivity;
    private PermissionListener mPermissionListener;
    private int mRequestCode;

    public void requestPermission(Activity activity, @NonNull String[] permissions, int requestCode,
                                  PermissionListener listener) {
        mActivity = activity;
        mRequestCode = requestCode;
        mPermissionListener = listener;

        if (!needRequestRuntimePermissions()) {
            mPermissionListener.onPermissionGranted();
            return;
        }
        requestUnGrantedPermissions(permissions, requestCode);
    }

    private boolean needRequestRuntimePermissions() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private void requestUnGrantedPermissions(String[] permissions, int requestCode) {
        String[] unGrantedPermissions = findUnGrantedPermissions(permissions);
        if (unGrantedPermissions.length == 0) {
            mPermissionListener.onPermissionGranted();
            return;
        }
        ActivityCompat.requestPermissions(mActivity, unGrantedPermissions, requestCode);
    }

    private boolean isPermissionGranted(String permission) {
        return ActivityCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private String[] findUnGrantedPermissions(String[] permissions) {
        List<String> unGrantedPermissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (!isPermissionGranted(permission)) {
                unGrantedPermissionList.add(permission);
            }
        }
        return unGrantedPermissionList.toArray(new String[0]);
    }

    public void onPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == mRequestCode) {
            if (grantResults.length > 0) {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    String permission = permissions[i];
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission);
                        if (!showRationale) {
                            mPermissionListener.onPermissionPermanentlyDenied();
                            return;
                        }
                        mPermissionListener.onPermissionDenied();
                        return;
                    }
                }
                mPermissionListener.onPermissionGranted();
            } else {
                mPermissionListener.onPermissionDenied();
            }
        }
    }

    public interface PermissionListener {
        void onPermissionGranted();

        void onPermissionDenied();

        void onPermissionPermanentlyDenied();
    }
}
