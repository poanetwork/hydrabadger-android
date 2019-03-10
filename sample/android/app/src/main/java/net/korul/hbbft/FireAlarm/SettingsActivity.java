package net.korul.hbbft.FireAlarm;

import android.media.AudioManager;

/**
 * Settings for the Alarm Clock.
 */
public class SettingsActivity {

    public static final String KEY_ALARM_IN_SILENT_MODE =
            "alarm_in_silent_mode";
    public static final String KEY_ALARM_SNOOZE =
            "snooze_duration";
    public static final String KEY_VOLUME_BEHAVIOR =
            "volume_button_setting";
    public static final String KEY_AUTO_SILENCE =
            "auto_silence";
    public static final String KEY_CLOCK_STYLE =
            "clock_style";
    public static final String KEY_HOME_TZ =
            "home_time_zone";
    public static final String KEY_AUTO_HOME_CLOCK =
            "automatic_home_clock";
    public static final String KEY_VOLUME_BUTTONS =
            "volume_button_setting";
    public static final String DEFAULT_VOLUME_BEHAVIOR = "0";
    private static final int ALARM_STREAM_TYPE_BIT =
            1 << AudioManager.STREAM_ALARM;
    private static CharSequence[][] mTimezones;
    private long mTime;


}
