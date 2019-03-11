package net.korul.hbbft.CommonFragments.tabSettings.RingtoneUtil;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * A MediaPlayer configured to play a ringtone in a loop.
 */
final class RingtoneLoop {

    @NonNull
    private final Context mContext;
    @NonNull
    private final AudioManager mAudioManager;
    private final Uri mUri;

    @Nullable
    private MediaPlayer mMediaPlayer;

    RingtoneLoop(Context context, Uri uri) {
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mUri = uri;
    }

    void play() {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(mContext, mUri);
            if (mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                // "Must call this method before prepare() or prepareAsync() in order
                // for the target stream type to become effective thereafter."
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.setLooping(true);
                // There is prepare() and prepareAsync().
                // "For files, it is OK to call prepare(), which blocks until
                // MediaPlayer is ready for playback."
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (@NonNull SecurityException | IOException e) {
            destroyLocalPlayer();
        }
    }

    void stop() {
        if (mMediaPlayer != null) {
            destroyLocalPlayer();
        }
    }

    private void destroyLocalPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

}