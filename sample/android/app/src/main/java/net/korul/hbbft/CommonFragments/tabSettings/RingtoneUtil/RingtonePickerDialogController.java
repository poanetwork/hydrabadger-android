package net.korul.hbbft.CommonFragments.tabSettings.RingtoneUtil;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.util.Log;

/**
 *
 */
public class RingtonePickerDialogController extends DialogFragmentController<RingtonePickerDialog> {
    private static final String TAG = "RingtonePickerCtrller";

    private final RingtonePickerDialog.OnRingtoneSelectedListener mListener;

    public RingtonePickerDialogController(FragmentManager fragmentManager, RingtonePickerDialog.OnRingtoneSelectedListener l) {
        super(fragmentManager);
        mListener = l;
    }

    public void show(@NonNull Uri initialUri, String tag) {
        RingtonePickerDialog dialog = RingtonePickerDialog.Companion.newInstance(mListener, initialUri);
        show(dialog, tag);
    }

    @Override
    public void tryRestoreCallback(String tag) {
        RingtonePickerDialog dialog = findDialog(tag);
        if (dialog != null) {
            Log.i(TAG, "Restoring on ringtone selected callback");
            dialog.setOnRingtoneSelectedListener(mListener);
        }
    }
}