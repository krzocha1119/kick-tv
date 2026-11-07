package com.personal.kicktv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageView;

public class VirtualCursorController {

    private final WebView webView;
    private final ImageView cursorView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private float cursorX;
    private float cursorY;
    private int screenWidth;
    private int screenHeight;

    private boolean isEnabled = true;
    private boolean isVisible = false;

    private final Runnable hideCursorRunnable = new Runnable() {
        @Override
        public void run() {
            if (cursorView != null && isVisible) {
                cursorView.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                cursorView.setVisibility(View.GONE);
                                isVisible = false;
                            }
                        })
                        .start();
            }
        }
    };

    public VirtualCursorController(WebView webView, ImageView cursorView) {
        this.webView = webView;
        this.cursorView = cursorView;

        WindowManager wm = (WindowManager) webView.getContext().getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            this.screenWidth = dm.widthPixels;
            this.screenHeight = dm.heightPixels;
        } else {
            this.screenWidth = 1920;
            this.screenHeight = 1080;
        }

        // Initial position at center
        cursorX = screenWidth / 2f;
        cursorY = screenHeight / 2f;
        updateViewPosition();
    }

    public void updateScreenSize(int width, int height) {
        if (width > 0 && height > 0) {
            this.screenWidth = width;
            this.screenHeight = height;
            cursorX = Math.min(cursorX, screenWidth - 1);
            cursorY = Math.min(cursorY, screenHeight - 1);
            updateViewPosition();
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (!enabled && isVisible) {
            cursorView.setVisibility(View.GONE);
            isVisible = false;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isEnabled) {
            return false;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                handleDpadMove(keyCode, event.getRepeatCount());
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (!isVisible) {
                    showCursor();
                    return true;
                }
                simulateClick();
                return true;

            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_CHANNEL_UP:
                webView.scrollBy(0, -400);
                return true;

            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                webView.scrollBy(0, 400);
                return true;

            default:
                return false;
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isEnabled) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            return true;
        }
        return false;
    }

    private void handleDpadMove(int keyCode, int repeatCount) {
        showCursor();

        // Progressive acceleration: precision tapping (14px) vs holding down (up to 42px)
        float step = 14f + Math.min(repeatCount * 4f, 28f);

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                cursorY -= step;
                if (cursorY <= 15) {
                    webView.scrollBy(0, -(int) (step * 2.5f));
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                cursorY += step;
                if (cursorY >= screenHeight - 35) {
                    webView.scrollBy(0, (int) (step * 2.5f));
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                cursorX -= step;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                cursorX += step;
                break;
        }

        // Clamp to screen bounds
        if (cursorX < 0) cursorX = 0;
        if (cursorX >= screenWidth) cursorX = screenWidth - 1;
        if (cursorY < 0) cursorY = 0;
        if (cursorY >= screenHeight) cursorY = screenHeight - 1;

        updateViewPosition();
    }

    private void showCursor() {
        handler.removeCallbacks(hideCursorRunnable);
        if (!isVisible) {
            cursorView.setAlpha(1f);
            cursorView.setVisibility(View.VISIBLE);
            isVisible = true;
        }
        // Auto-hide after 4 seconds of inactivity
        handler.postDelayed(hideCursorRunnable, 4000);
    }

    private void updateViewPosition() {
        cursorView.setX(cursorX);
        cursorView.setY(cursorY);
    }

    private void simulateClick() {
        showCursor();

        // Little bounce animation to show feedback on click
        cursorView.animate()
                .scaleX(0.75f)
                .scaleY(0.75f)
                .setDuration(70)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        cursorView.animate().scaleX(1f).scaleY(1f).setDuration(70).start();
                    }
                })
                .start();

        long now = SystemClock.uptimeMillis();

        MotionEvent down = MotionEvent.obtain(
                now, now,
                MotionEvent.ACTION_DOWN,
                cursorX, cursorY, 0
        );
        webView.dispatchTouchEvent(down);
        down.recycle();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long upTime = SystemClock.uptimeMillis();
                MotionEvent up = MotionEvent.obtain(
                        upTime, upTime,
                        MotionEvent.ACTION_UP,
                        cursorX, cursorY, 0
                );
                webView.dispatchTouchEvent(up);
                up.recycle();
            }
        }, 50);
    }
}
