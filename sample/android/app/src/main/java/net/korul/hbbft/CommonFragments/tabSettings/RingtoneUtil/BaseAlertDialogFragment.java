package net.korul.hbbft.CommonFragments.tabSettings.RingtoneUtil;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Base class for creating AlertDialogs with 'cancel' and 'ok' actions.
 */
public abstract class BaseAlertDialogFragment extends AppCompatDialogFragment {

    protected abstract void onOk();

    @NotNull
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()))
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onOk();
                    }
                });
        return createFrom(builder);
    }

    /**
     * Subclasses can override this to make any modifications to the given Builder instance,
     * which already has its negative and positive buttons set.
     * <p></p>
     * The default implementation creates and returns the {@code AlertDialog} as is.
     */
    AlertDialog createFrom(@NonNull AlertDialog.Builder builder) {
        return builder.create();
    }
}