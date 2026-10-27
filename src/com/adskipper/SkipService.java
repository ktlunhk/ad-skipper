package com.adskipper;

import android.accessibilityservice.AccessibilityService;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Locale;

public class SkipService extends AccessibilityService {

    private static final String YT = "com.google.android.youtube";

    // View ids used by the YouTube app. If YouTube changes them, update here.
    private static final String[] SKIP_IDS = new String[] {
        YT + ":id/skip_ad_button",
        YT + ":id/skip_ad_button_container"
    };
    private static final String[] AD_IDS = new String[] {
        YT + ":id/ad_progress_text"
    };

    private static final long MIN_INTERVAL_MS = 120;
    private static final long WATCHDOG_MS = 1000;

    private final Handler handler = new Handler();
    private long lastRun = 0;
    private boolean mutedByUs = false;

    private final Runnable watchdog = new Runnable() {
        @Override
        public void run() {
            if (!mutedByUs) {
                return;
            }
            boolean adStillShown = false;
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                try {
                    if (YT.equals(String.valueOf(root.getPackageName()))) {
                        adStillShown = hasAnyId(root, AD_IDS) || hasAnyId(root, SKIP_IDS);
                    }
                } finally {
                    root.recycle();
                }
            }
            if (adStillShown) {
                handler.postDelayed(this, WATCHDOG_MS);
            } else {
                unmuteMusic();
            }
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        long now = SystemClock.uptimeMillis();
        if (now - lastRun < MIN_INTERVAL_MS) {
            return;
        }
        lastRun = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }
        try {
            process(root);
        } finally {
            root.recycle();
        }
    }

    private void process(AccessibilityNodeInfo root) {
        if (!YT.equals(String.valueOf(root.getPackageName()))) {
            unmuteMusic();
            return;
        }

        AccessibilityNodeInfo skip = findSkipButton(root);
        boolean adShown = skip != null || hasAnyId(root, AD_IDS);

        if (adShown) {
            if (Prefs.isMute(this)) {
                muteMusic();
            }
        } else {
            unmuteMusic();
        }

        if (skip != null) {
            if (Prefs.isAutoSkip(this)) {
                if (clickNode(skip)) {
                    unmuteMusic();
                }
            }
            skip.recycle();
        }
    }

    // ---------- finding nodes ----------

    private AccessibilityNodeInfo findSkipButton(AccessibilityNodeInfo root) {
        for (int i = 0; i < SKIP_IDS.length; i++) {
            List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId(SKIP_IDS[i]);
            if (list != null && !list.isEmpty()) {
                AccessibilityNodeInfo first = list.get(0);
                for (int j = 1; j < list.size(); j++) {
                    list.get(j).recycle();
                }
                return first;
            }
        }
        return findSkipByText(root);
    }

    private AccessibilityNodeInfo findSkipByText(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByText("skip");
        if (list == null) {
            return null;
        }
        AccessibilityNodeInfo found = null;
        for (int i = 0; i < list.size(); i++) {
            AccessibilityNodeInfo n = list.get(i);
            boolean match = isSkipLabel(n.getText()) || isSkipLabel(n.getContentDescription());
            if (found == null && match) {
                found = n;
            } else {
                n.recycle();
            }
        }
        return found;
    }

    private boolean isSkipLabel(CharSequence cs) {
        if (cs == null) {
            return false;
        }
        String s = cs.toString().trim().toLowerCase(Locale.ENGLISH);
        return s.equals("skip") || s.startsWith("skip ad");
    }

    private boolean hasAnyId(AccessibilityNodeInfo root, String[] ids) {
        for (int i = 0; i < ids.length; i++) {
            List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId(ids[i]);
            if (list != null) {
                boolean found = !list.isEmpty();
                for (int j = 0; j < list.size(); j++) {
                    list.get(j).recycle();
                }
                if (found) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------- clicking ----------

    // Clicks the node, or the nearest clickable parent (up to 5 levels).
    private boolean clickNode(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo cur = AccessibilityNodeInfo.obtain(node);
        for (int i = 0; i < 5 && cur != null; i++) {
            if (cur.isClickable()) {
                boolean ok = cur.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                cur.recycle();
                return ok;
            }
            AccessibilityNodeInfo parent = cur.getParent();
            cur.recycle();
            cur = parent;
        }
        if (cur != null) {
            cur.recycle();
        }
        return false;
    }

    // ---------- muting ----------

    @SuppressWarnings("deprecation")
    private void muteMusic() {
        if (mutedByUs) {
            return;
        }
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if (am.isStreamMute(AudioManager.STREAM_MUSIC)) {
                return; // user already muted it, leave it alone
            }
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
        } else {
            am.setStreamMute(AudioManager.STREAM_MUSIC, true);
        }
        mutedByUs = true;
        handler.removeCallbacks(watchdog);
        handler.postDelayed(watchdog, WATCHDOG_MS);
    }

    @SuppressWarnings("deprecation")
    private void unmuteMusic() {
        if (!mutedByUs) {
            return;
        }
        mutedByUs = false;
        handler.removeCallbacks(watchdog);
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
        } else {
            am.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
    }

    // ---------- lifecycle ----------

    @Override
    public void onInterrupt() {
        unmuteMusic();
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        unmuteMusic();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        unmuteMusic();
        super.onDestroy();
    }
}
