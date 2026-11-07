package com.personal.kicktv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    private static final String START_URL = "https://kick.com";
    private static final String USER_AGENT_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    private FrameLayout rootContainer;
    private WebView webView;
    private FrameLayout fullscreenContainer;
    private ProgressBar loadingProgress;
    private ImageView cursorView;
    private FrameLayout menuOverlay;

    // Menu Buttons
    private Button btnHome;
    private Button btnLogin;
    private Button btnTeleportTop;
    private Button btnCategories;
    private Button btnGoChannel;
    private Button btnFavorites;
    private Button btnAddFav;
    private Button btnToggleMouse;
    private Button btnToggleChat;
    private Button btnFullscreenVideo;
    private Button btnReload;
    private Button btnCloseMenu;
    private Button btnExitApp;

    private VirtualCursorController cursorController;
    private FavoritesManager favoritesManager;

    private View customVideoView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private long lastBackPressTime = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during streams
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();

        setContentView(R.layout.activity_main);

        rootContainer = findViewById(R.id.root_container);
        webView = findViewById(R.id.webview);
        fullscreenContainer = findViewById(R.id.fullscreen_custom_view_container);
        loadingProgress = findViewById(R.id.loading_progress);
        cursorView = findViewById(R.id.cursor_view);
        menuOverlay = findViewById(R.id.menu_overlay);

        favoritesManager = new FavoritesManager(this);
        cursorController = new VirtualCursorController(webView, cursorView);

        setupMenuButtons();
        setupWebView();

        rootContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                cursorController.updateScreenSize(right - left, bottom - top);
            }
        });

        webView.loadUrl(START_URL);
    }

    private void hideSystemUI() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void setupWebView() {
        // Prevent WebView from capturing DPAD focus / caret navigation
        webView.setFocusable(false);
        webView.setFocusableInTouchMode(false);
        webView.setLongClickable(false);
        webView.setHapticFeedbackEnabled(false);
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true; // Disable text selection context menu
            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // TV Performance Optimizations
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setTextZoom(100);
        settings.setGeolocationEnabled(false);

        settings.setUserAgentString(USER_AGENT_DESKTOP);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    loadingProgress.setVisibility(View.VISIBLE);
                    loadingProgress.setProgress(newProgress);
                } else {
                    loadingProgress.setVisibility(View.GONE);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customVideoView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customVideoView = view;
                customViewCallback = callback;

                webView.setVisibility(View.GONE);
                fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                fullscreenContainer.setVisibility(View.VISIBLE);
                hideSystemUI();
            }

            @Override
            public void onHideCustomView() {
                if (customVideoView == null) return;

                fullscreenContainer.removeView(customVideoView);
                fullscreenContainer.setVisibility(View.GONE);
                customVideoView = null;

                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
                webView.setVisibility(View.VISIBLE);
                hideSystemUI();
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                injectTvOptimizations();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectTvOptimizations();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                return true;
            }
        });
    }

    private void injectTvOptimizations() {
        String css =
                "*, *::before, *::after {" +
                "  -webkit-user-select: none !important;" +
                "  user-select: none !important;" +
                "  -webkit-touch-callout: none !important;" +
                "  -webkit-tap-highlight-color: transparent !important;" +
                "  outline: none !important;" +
                "  box-shadow: none !important;" +
                "  text-shadow: none !important;" +
                "  backdrop-filter: none !important;" +
                "  -webkit-backdrop-filter: none !important;" +
                "}" +
                "input, textarea {" +
                "  -webkit-user-select: text !important;" +
                "  user-select: text !important;" +
                "}" +
                "header, nav, [class*=\\\"header\\\"] {" +
                "  backdrop-filter: none !important;" +
                "  background-color: #0b0e0f !important;" +
                "}" +
                "div[class*=\\\"app-banner\\\"], div[class*=\\\"mobile-app\\\"], #onetrust-banner-sdk {" +
                "  display: none !important;" +
                "}" +
                "video {" +
                "  object-fit: contain !important;" +
                "  transform: translateZ(0) !important;" +
                "}";

        String js = "(function() {" +
                "  try {" +
                "    let style = document.getElementById('kick-tv-perf-style');" +
                "    if (!style) {" +
                "      style = document.createElement('style');" +
                "      style.id = 'kick-tv-perf-style';" +
                "      document.head.appendChild(style);" +
                "    }" +
                "    style.innerHTML = `" + css + "`;" +
                "    const cookieBtns = ['#onetrust-accept-btn-handler', 'button[id*=\\\"accept\\\"]', 'button[class*=\\\"cookie\\\"]'];" +
                "    for (const sel of cookieBtns) {" +
                "      const b = document.querySelector(sel);" +
                "      if (b) { b.click(); break; }" +
                "    }" +
                "  } catch(e) {}" +
                "})();";

        webView.evaluateJavascript(js, null);
    }

    private void setupMenuButtons() {
        btnHome = findViewById(R.id.btn_home);
        btnLogin = findViewById(R.id.btn_login);
        btnTeleportTop = findViewById(R.id.btn_teleport_top);
        btnCategories = findViewById(R.id.btn_categories);
        btnGoChannel = findViewById(R.id.btn_go_channel);
        btnFavorites = findViewById(R.id.btn_favorites);
        btnAddFav = findViewById(R.id.btn_add_fav);
        btnToggleMouse = findViewById(R.id.btn_toggle_mouse);
        btnToggleChat = findViewById(R.id.btn_toggle_chat);
        btnFullscreenVideo = findViewById(R.id.btn_fullscreen_video);
        btnReload = findViewById(R.id.btn_reload);
        btnCloseMenu = findViewById(R.id.btn_close_menu);
        btnExitApp = findViewById(R.id.btn_exit_app);

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(START_URL);
                hideMenu();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                triggerLoginModal();
            }
        });

        btnTeleportTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                // Move cursor to top-right corner where Log In and Sign Up buttons sit
                cursorController.teleportTo(1750, 55);
                Toast.makeText(MainActivity.this, "Kursor w prawym górnym rogu", Toast.LENGTH_SHORT).show();
            }
        });

        btnCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl("https://kick.com/categories");
                hideMenu();
            }
        });

        btnGoChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                showGoChannelDialog();
            }
        });

        btnFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                showFavoritesDialog();
            }
        });

        btnAddFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentUrl = webView.getUrl();
                if (currentUrl != null && !currentUrl.equals(START_URL)) {
                    String clean = FavoritesManager.cleanChannelName(currentUrl);
                    if (!clean.isEmpty() && !clean.equals("categories") && !clean.equals("following")) {
                        favoritesManager.addFavorite(clean);
                        Toast.makeText(MainActivity.this, "Dodano do ulubionych: " + clean, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Przejdź na profil streamera, aby go dodać", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Otwórz profil kanału przed dodaniem", Toast.LENGTH_SHORT).show();
                }
                hideMenu();
            }
        });

        btnToggleMouse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newState = !cursorController.isEnabled();
                cursorController.setEnabled(newState);
                btnToggleMouse.setText(newState ? "Wskaźnik myszy: WŁĄCZONY" : "Wskaźnik myszy: WYŁĄCZONY");
                Toast.makeText(MainActivity.this, newState ? "Wskaźnik włączony" : "Tryb D-Pad włączony", Toast.LENGTH_SHORT).show();
                hideMenu();
            }
        });

        btnToggleChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                String js = "(function() {" +
                        "  const btn = document.querySelector('[data-testid=\"toggle-chat\"]') || document.querySelector('button[aria-label*=\"chat\" i]');" +
                        "  if (btn) btn.click();" +
                        "})();";
                webView.evaluateJavascript(js, null);
            }
        });

        btnFullscreenVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
                String js = "(function() {" +
                        "  const btn = document.querySelector('button[aria-label*=\"theater\" i]') || document.querySelector('button[aria-label*=\"Theater\" i]');" +
                        "  if (btn) { btn.click(); return; }" +
                        "  const v = document.querySelector('video');" +
                        "  if (v && v.requestFullscreen) v.requestFullscreen();" +
                        "})();";
                webView.evaluateJavascript(js, null);
            }
        });

        btnReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
                hideMenu();
            }
        });

        btnCloseMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
            }
        });

        btnExitApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showGoChannelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Przejdź do kanału Kick");

        final EditText input = new EditText(this);
        input.setHint("Wpisz nick streamera (np. xqc)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setTextColor(0xFFFFFFFF);
        builder.setView(input);

        builder.setPositiveButton("Otwórz", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString();
                String clean = FavoritesManager.cleanChannelName(name);
                if (!clean.isEmpty()) {
                    webView.loadUrl("https://kick.com/" + clean);
                }
            }
        });
        builder.setNegativeButton("Anuluj", null);
        builder.show();
    }

    private void showFavoritesDialog() {
        final List<String> favs = favoritesManager.getFavorites();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("⭐ Ulubione kanały");

        if (favs.isEmpty()) {
            builder.setMessage("Brak zapisanych ulubionych.\nMożesz dodać kanał z poziomu menu (przycisk +).");
            builder.setPositiveButton("OK", null);
            builder.show();
            return;
        }

        CharSequence[] items = favs.toArray(new CharSequence[0]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String streamer = favs.get(which);
                webView.loadUrl("https://kick.com/" + streamer);
            }
        });

        builder.setNeutralButton("Usuń kanał...", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showDeleteFavoriteDialog(favs);
            }
        });

        builder.setNegativeButton("Zamknij", null);
        builder.show();
    }

    private void showDeleteFavoriteDialog(final List<String> favs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("Wybierz kanał do usunięcia");
        CharSequence[] items = favs.toArray(new CharSequence[0]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String toRemove = favs.get(which);
                favoritesManager.removeFavorite(toRemove);
                Toast.makeText(MainActivity.this, "Usunięto: " + toRemove, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Anuluj", null);
        builder.show();
    }

    private void toggleMenu() {
        if (menuOverlay.getVisibility() == View.VISIBLE) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    private void showMenu() {
        menuOverlay.setVisibility(View.VISIBLE);
        btnHome.requestFocus();
    }

    private void hideMenu() {
        menuOverlay.setVisibility(View.GONE);
        hideSystemUI();
    }

    private void triggerLoginModal() {
        String js = "(function() {" +
                "  try {" +
                "    const selectors = [" +
                "      'button[data-testid=\"login-button\"]'," +
                "      'button[aria-label*=\"log in\" i]'," +
                "      'button[aria-label*=\"zaloguj\" i]'," +
                "      'a[href*=\"/login\"]'" +
                "    ];" +
                "    for (const sel of selectors) {" +
                "      const el = document.querySelector(sel);" +
                "      if (el) { el.click(); return true; }" +
                "    }" +
                "    const buttons = Array.from(document.querySelectorAll('button, a'));" +
                "    const btn = buttons.find(b => {" +
                "      const t = (b.textContent || '').trim().toLowerCase();" +
                "      return t === 'log in' || t === 'zaloguj się' || t === 'sign in';" +
                "    });" +
                "    if (btn) { btn.click(); return true; }" +
                "    window.location.href = 'https://kick.com/auth/login';" +
                "  } catch(e) {}" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    private void handleBackPress() {
        if (customVideoView != null) {
            if (webView.getWebChromeClient() != null) {
                webView.getWebChromeClient().onHideCustomView();
            }
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackPressTime < 2500) {
            finish();
        } else {
            lastBackPressTime = now;
            Toast.makeText(this, "Naciśnij WSTECZ ponownie lub MENU aby otworzyć opcje", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // When menu overlay is open, allow standard focus traversal inside the menu card
        if (menuOverlay != null && menuOverlay.getVisibility() == View.VISIBLE) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                hideMenu();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }

        int keyCode = event.getKeyCode();
        int action = event.getAction();

        // Menu trigger
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BOOKMARK || keyCode == KeyEvent.KEYCODE_INFO) {
            if (action == KeyEvent.ACTION_DOWN) {
                toggleMenu();
            }
            return true;
        }

        // Multimedia controls
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (action == KeyEvent.ACTION_DOWN) {
                webView.evaluateJavascript("(function(){ const v = document.querySelector('video'); if (v) { if (v.paused) v.play(); else v.pause(); } })();", null);
            }
            return true;
        }

        // Back button
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (action == KeyEvent.ACTION_DOWN) {
                handleBackPress();
            }
            return true;
        }

        // Intercept all DPAD & Enter keys so WebView NEVER performs spatial caret / text selection
        if (cursorController != null && cursorController.isEnabled()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_PAGE_UP:
                case KeyEvent.KEYCODE_PAGE_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    if (action == KeyEvent.ACTION_DOWN) {
                        return cursorController.onKeyDown(keyCode, event);
                    } else if (action == KeyEvent.ACTION_UP) {
                        return cursorController.onKeyUp(keyCode, event);
                    }
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        hideSystemUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }
}
