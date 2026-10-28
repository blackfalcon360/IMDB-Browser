package com.blackfalcon.imdbbrowser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int ACC = 0xFFF5C518;
    private static final int ON_ACC = 0xFF000000;

    private interface Pick {
        void on(int i);
    }

    private FrameLayout outer;
    private FrameLayout fsHolder;
    private FrameLayout overlay;
    private LinearLayout root;
    private LinearLayout chipsRow;
    private EditText searchEt;
    private TextView titleTv;
    private TextView errorTv;
    private TextView backBtn;
    private TextView fwdBtn;
    private ProgressBar progress;
    private WebView webView;
    private View customView;
    private WebChromeClient.CustomViewCallback customCb;
    private final TextView[] tabViews = new TextView[4];
    private int cur = 0;
    private String pendingSection = null;
    private int bg, card, chipBg, txt, sub;
    private String pLang, pTheme;

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        build();
        webView.loadUrl(Imdb.HOME);
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null && webView.getVisibility() == View.VISIBLE) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            ViewGroup p = (ViewGroup) webView.getParent();
            if (p != null) p.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            hideCustom();
            return;
        }
        if (cur == 3) {
            showTab(0);
            return;
        }
        if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        if (cur != 0) {
            showTab(0);
            return;
        }
        confirmExit();
    }

    // ------------------------------------------------------------------ small helpers

    private String tr(String en, String ur, String ar) {
        return L.t(this, en, ur, ar);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private TextView tv(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout hbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private TextView button(String label, int bgColor, int fg, View.OnClickListener l) {
        TextView t = tv(label, 14, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(11), dp(12), dp(11));
        t.setBackground(rounded(bgColor, 10));
        t.setOnClickListener(l);
        return t;
    }

    /** Adds an equal-width button to a horizontal row. The row itself must be added with MATCH_PARENT width. */
    private void addBtn(LinearLayout row, String label, int bgColor, int fg, View.OnClickListener l) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, WRAP, 1f);
        p.setMargins(dp(3), 0, dp(3), 0);
        row.addView(button(label, bgColor, fg, l), p);
    }

    private TextView navBtn(String label, View.OnClickListener l) {
        TextView t = tv(label, 20, ACC, true);
        t.setPadding(dp(11), dp(6), dp(11), dp(6));
        t.setOnClickListener(l);
        return t;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private int indexOf(String[] a, String v) {
        for (int i = 0; i < a.length; i++) if (a[i].equals(v)) return i;
        return -1;
    }

    private void applyTheme() {
        boolean dark = !"light".equals(Store.theme(this));
        if (dark) {
            bg = 0xFF000000;
            card = 0xFF161616;
            chipBg = 0xFF2A2A2A;
            txt = 0xFFFFFFFF;
            sub = 0xFF9E9E9E;
        } else {
            bg = 0xFFF2F2F2;
            card = 0xFFFFFFFF;
            chipBg = 0xFFDEDEDE;
            txt = 0xFF111111;
            sub = 0xFF5F5F5F;
        }
    }

    private void styleChip(TextView t, boolean sel) {
        t.setBackground(rounded(sel ? ACC : chipBg, 10));
        t.setTextColor(sel ? ON_ACC : txt);
    }

    private LinearLayout chips(String[] labels, int sel, final boolean sticky, final Pick cb) {
        final LinearLayout row = hbox();
        final int n = labels.length;
        for (int i = 0; i < n; i++) {
            final int idx = i;
            TextView t = new TextView(this);
            t.setText(labels[i]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(12);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setPadding(dp(4), dp(9), dp(4), dp(9));
            styleChip(t, i == sel);
            t.setOnClickListener(v -> {
                if (sticky) {
                    for (int k = 0; k < n; k++) styleChip((TextView) row.getChildAt(k), k == idx);
                }
                cb.on(idx);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            row.addView(t, lp);
        }
        return row;
    }

    private LinearLayout section(LinearLayout box, String title) {
        LinearLayout c = vbox();
        c.setBackground(rounded(card, 12));
        c.setPadding(dp(12), dp(10), dp(12), dp(10));
        c.addView(tv(title, 14, ACC, true));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.setMargins(0, 0, 0, dp(10));
        box.addView(c, p);
        return c;
    }

    private void note(LinearLayout parent, String text) {
        TextView t = tv(text, 12, sub, false);
        t.setPadding(0, dp(6), 0, 0);
        parent.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && searchEt != null) imm.hideSoftInputFromWindow(searchEt.getWindowToken(), 0);
        } catch (Exception ignore) {
            // keyboard already hidden
        }
    }

    // ------------------------------------------------------------------ WebView

    @SuppressLint("SetJavaScriptEnabled")
    private void createWebView() {
        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportMultipleWindows(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                hideError();
                noteUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                noteUrl(url);
                if (Imdb.isTitleRoot(url)) {
                    String n = Imdb.cleanTitle(view.getTitle());
                    if (n.length() > 0 && !n.equals(Store.ttName(MainActivity.this))) {
                        Store.setTtName(MainActivity.this, n);
                        updateTitleLabel();
                    }
                }
                updateNav();
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                noteUrl(url);
                updateNav();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showError();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progress != null) {
                    progress.setProgress(newProgress);
                    progress.setVisibility(newProgress >= 100 ? View.INVISIBLE : View.VISIBLE);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                showCustom(view, callback);
            }

            @Override
            public void onHideCustomView() {
                hideCustom();
            }
        });
    }

    /** Decides what to do with a link tapped inside the page. All web links stay inside the app. */
    private boolean handleUrl(String url) {
        if (url.startsWith("http://")) {
            navigate(Imdb.https(url));
            return true;
        }
        if (url.startsWith("https://")) {
            if (pendingSection != null && Imdb.isTitleRoot(url)) {
                navigate(url);
                return true;
            }
            return false;
        }
        if (url.startsWith("about:") || url.startsWith("blob:") || url.startsWith("data:")
                || url.startsWith("javascript:")) {
            return false;
        }
        toast(tr("This kind of link cannot be opened inside the app.",
                "اس قسم کا لنک ایپ کے اندر نہیں کھل سکتا۔",
                "لا يمكن فتح هذا النوع من الروابط داخل التطبيق."));
        return true;
    }

    /** Loads a URL; if a Goofs/Quotes search is waiting and this is a title page, opens that section instead. */
    private void navigate(String url) {
        if (pendingSection != null && Imdb.isTitleRoot(url)) {
            String tt = Imdb.ttId(url);
            String sec = pendingSection;
            pendingSection = null;
            Store.setTt(this, tt);
            updateTitleLabel();
            webView.loadUrl(Imdb.section(tt, sec));
        } else {
            webView.loadUrl(url);
        }
    }

    private void noteUrl(String url) {
        String tt = Imdb.ttId(url);
        if (tt.length() > 0 && !tt.equals(Store.tt(this))) {
            Store.setTt(this, tt);
            Store.setTtName(this, "");
            updateTitleLabel();
        }
    }

    private void updateTitleLabel() {
        if (titleTv == null) return;
        String tt = Store.tt(this);
        if (tt.length() == 0) {
            titleTv.setVisibility(View.GONE);
            return;
        }
        String name = Store.ttName(this);
        String shown = name.length() > 0 ? name + " (" + tt + ")" : tt;
        titleTv.setText(String.format(Locale.US,
                tr("Current title: %s", "موجودہ ٹائٹل: %s", "العنوان الحالي: %s"), shown));
        titleTv.setVisibility(View.VISIBLE);
    }

    private void updateNav() {
        if (webView == null || backBtn == null || fwdBtn == null) return;
        backBtn.setAlpha(webView.canGoBack() ? 1f : 0.35f);
        fwdBtn.setAlpha(webView.canGoForward() ? 1f : 0.35f);
    }

    private void showError() {
        if (errorTv != null) {
            errorTv.setText(tr("Could not load the page. Check your internet connection and tap \u21BB.",
                    "صفحہ لوڈ نہیں ہو سکا۔ انٹرنیٹ چیک کریں اور \u21BB دبائیں۔",
                    "تعذّر تحميل الصفحة. تحقق من الإنترنت واضغط \u21BB."));
            errorTv.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (errorTv != null) errorTv.setVisibility(View.GONE);
    }

    private void showCustom(View v, WebChromeClient.CustomViewCallback cb) {
        if (customView != null) {
            cb.onCustomViewHidden();
            return;
        }
        customView = v;
        customCb = cb;
        fsHolder.addView(v, new FrameLayout.LayoutParams(MATCH, MATCH));
        fsHolder.setVisibility(View.VISIBLE);
    }

    private void hideCustom() {
        if (customView == null) return;
        fsHolder.removeView(customView);
        fsHolder.setVisibility(View.GONE);
        customView = null;
        if (customCb != null) customCb.onCustomViewHidden();
        customCb = null;
    }

    // ------------------------------------------------------------------ layout

    private void build() {
        applyTheme();
        String keepText = searchEt != null ? searchEt.getText().toString() : "";
        if (webView == null) {
            createWebView();
        } else {
            ViewGroup old = (ViewGroup) webView.getParent();
            if (old != null) old.removeView(webView);
        }
        boolean rtl = L.rtl(this);

        root = vbox();
        root.setBackgroundColor(bg);
        root.setLayoutDirection(rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);

        // header with navigation buttons
        LinearLayout head = hbox();
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(12), dp(6), dp(4), dp(2));
        TextView title = tv("\uD83C\uDFAC " + tr("IMDb Browser", "آئی ایم ڈی بی براؤزر", "متصفح IMDb"), 18, txt, true);
        head.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));
        backBtn = navBtn(rtl ? "\u25B6" : "\u25C0", v -> navBack());
        fwdBtn = navBtn(rtl ? "\u25C0" : "\u25B6", v -> navForward());
        head.addView(backBtn, new LinearLayout.LayoutParams(WRAP, WRAP));
        head.addView(fwdBtn, new LinearLayout.LayoutParams(WRAP, WRAP));
        head.addView(navBtn("\u21BB", v -> navReload()), new LinearLayout.LayoutParams(WRAP, WRAP));
        head.addView(navBtn("\uD83C\uDFE0", v -> openUrl(Imdb.HOME)), new LinearLayout.LayoutParams(WRAP, WRAP));
        root.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        // search row (typed text is black on white in both themes)
        LinearLayout sr = hbox();
        sr.setGravity(Gravity.CENTER_VERTICAL);
        sr.setPadding(dp(10), dp(2), dp(10), dp(4));
        searchEt = new EditText(this);
        searchEt.setHint(tr("Search movies, shows\u2026 or paste a link",
                "فلم یا شو تلاش کریں\u2026 یا لنک پیسٹ کریں",
                "ابحث عن فيلم أو مسلسل\u2026 أو الصق رابطًا"));
        searchEt.setSingleLine(true);
        searchEt.setTextSize(15);
        searchEt.setTextColor(Color.BLACK);
        searchEt.setHintTextColor(0xFF777777);
        searchEt.setBackground(rounded(Color.WHITE, 10));
        searchEt.setPadding(dp(12), dp(9), dp(12), dp(9));
        searchEt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchEt.setText(keepText);
        searchEt.setOnEditorActionListener((tvw, actionId, ev) -> {
            doSearch();
            return true;
        });
        sr.addView(searchEt, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView go = button(tr("Search", "تلاش", "بحث"), ACC, ON_ACC, v -> doSearch());
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(WRAP, WRAP);
        gp.setMarginStart(dp(8));
        sr.addView(go, gp);
        root.addView(sr, new LinearLayout.LayoutParams(MATCH, WRAP));

        // quick links (Browse tab only)
        final String[] quickUrls = {
                Imdb.HOME,
                "https://www.imdb.com/chart/top/",
                "https://www.imdb.com/chart/moviemeter/",
                "https://www.imdb.com/chart/toptv/"};
        String[] quickLabels = {
                tr("Home", "ہوم", "الرئيسية"),
                tr("Top 250", "ٹاپ 250", "أفضل 250"),
                tr("Popular", "مقبول", "الأشهر"),
                tr("Top TV", "ٹاپ ٹی وی", "أفضل المسلسلات")};
        chipsRow = chips(quickLabels, -1, false, i -> openUrl(quickUrls[i]));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(MATCH, WRAP);
        cp.setMargins(dp(7), 0, dp(7), 0);
        root.addView(chipsRow, cp);

        titleTv = tv("", 12, sub, false);
        titleTv.setPadding(dp(14), dp(2), dp(14), dp(2));
        root.addView(titleTv, new LinearLayout.LayoutParams(MATCH, WRAP));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(ColorStateList.valueOf(ACC));
        progress.setVisibility(View.INVISIBLE);
        root.addView(progress, new LinearLayout.LayoutParams(MATCH, dp(3)));

        // stage: web page, error message and overlay panels share the same space
        FrameLayout stage = new FrameLayout(this);
        stage.addView(webView, new FrameLayout.LayoutParams(MATCH, MATCH));
        errorTv = tv("", 15, txt, false);
        errorTv.setBackgroundColor(bg);
        errorTv.setGravity(Gravity.CENTER);
        errorTv.setPadding(dp(24), dp(24), dp(24), dp(24));
        errorTv.setVisibility(View.GONE);
        stage.addView(errorTv, new FrameLayout.LayoutParams(MATCH, MATCH));
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(bg);
        overlay.setVisibility(View.GONE);
        stage.addView(overlay, new FrameLayout.LayoutParams(MATCH, MATCH));
        root.addView(stage, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        // tab bar
        LinearLayout bar = hbox();
        bar.setBackgroundColor(card);
        String[] names = {
                tr("Browse", "براؤز", "تصفح"),
                tr("Goofs", "غلطیاں", "أخطاء"),
                tr("Quotes", "مکالمے", "اقتباسات"),
                tr("Settings", "سیٹنگز", "الإعدادات")};
        String[] icons = {"\uD83C\uDF10", "\uD83C\uDFAD", "\uD83D\uDCAC", "\u2699\uFE0F"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView t = tv(icons[i] + "\n" + names[i], 12, sub, false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(7), 0, dp(7));
            t.setOnClickListener(v -> showTab(idx));
            tabViews[i] = t;
            bar.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
        }
        root.addView(bar, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView by = tv("By: Black Falcon \uD83E\uDD85", 12, sub, false);
        by.setGravity(Gravity.CENTER);
        by.setBackgroundColor(card);
        by.setPadding(0, dp(4), 0, dp(8));
        root.addView(by, new LinearLayout.LayoutParams(MATCH, WRAP));

        outer = new FrameLayout(this);
        outer.addView(root, new FrameLayout.LayoutParams(MATCH, MATCH));
        fsHolder = new FrameLayout(this);
        fsHolder.setBackgroundColor(Color.BLACK);
        fsHolder.setVisibility(View.GONE);
        outer.addView(fsHolder, new FrameLayout.LayoutParams(MATCH, MATCH));
        setContentView(outer);

        showTab(cur);
        updateNav();
        updateTitleLabel();
    }

    private void showTab(int i) {
        cur = i;
        for (int k = 0; k < 4; k++) {
            tabViews[k].setTextColor(k == i ? ACC : sub);
            tabViews[k].setTypeface(null, k == i ? Typeface.BOLD : Typeface.NORMAL);
        }
        chipsRow.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        if (i != 1 && i != 2) pendingSection = null;
        if (i == 0) {
            showWeb();
        } else if (i == 1 || i == 2) {
            String sec = (i == 1) ? "goofs" : "quotes";
            String tt = Store.tt(this);
            if (tt.length() > 0) {
                pendingSection = null;
                showWeb();
                if (!Imdb.isSection(webView.getUrl(), tt, sec)) webView.loadUrl(Imdb.section(tt, sec));
            } else {
                pendingSection = sec;
                showOverlay(buildPickPanel(sec));
            }
        } else {
            showOverlay(buildSettings());
        }
    }

    private void showWeb() {
        overlay.setVisibility(View.GONE);
        overlay.removeAllViews();
        webView.setVisibility(View.VISIBLE);
        webView.onResume();
    }

    private void showOverlay(View v) {
        overlay.removeAllViews();
        overlay.addView(v, new FrameLayout.LayoutParams(MATCH, MATCH));
        overlay.setVisibility(View.VISIBLE);
        webView.onPause();
    }

    private boolean overlayShowing() {
        return overlay.getVisibility() == View.VISIBLE;
    }

    /** Makes sure the web page is visible before a navigation action. */
    private void ensureWeb() {
        if (cur == 3) showTab(0);
        else if (overlayShowing()) showWeb();
    }

    private void openUrl(String url) {
        ensureWeb();
        hideKeyboard();
        navigate(url);
    }

    private void navBack() {
        ensureWeb();
        if (webView.canGoBack()) webView.goBack();
    }

    private void navForward() {
        ensureWeb();
        if (webView.canGoForward()) webView.goForward();
    }

    private void navReload() {
        ensureWeb();
        hideError();
        webView.reload();
    }

    private void doSearch() {
        String u = Imdb.normalizeInput(searchEt.getText().toString());
        if (u.length() == 0) return;
        hideKeyboard();
        if (cur == 3) showTab(0);
        else if (overlayShowing()) showWeb();
        pendingSection = (cur == 1) ? "goofs" : (cur == 2 ? "quotes" : null);
        navigate(u);
    }

    // ------------------------------------------------------------------ panels

    private View buildPickPanel(final String sec) {
        boolean goofs = "goofs".equals(sec);
        LinearLayout b = vbox();
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(28), dp(24), dp(28), dp(24));
        TextView icon = tv(goofs ? "\uD83C\uDFAD" : "\uD83D\uDCAC", 48, txt, false);
        icon.setGravity(Gravity.CENTER);
        b.addView(icon, new LinearLayout.LayoutParams(MATCH, WRAP));
        String msg;
        if (goofs) {
            msg = tr("Goofs are mistakes left in movies (continuity errors, things that do not belong in the period, and so on). Search for a movie or show above. When you tap its page, its goofs open automatically.",
                    "غلطیاں (Goofs) فلموں میں رہ جانے والی غلطیاں ہیں (تسلسل کی غلطیاں، اس دور سے مطابقت نہ رکھنے والی چیزیں وغیرہ)۔ اوپر کسی فلم یا شو کو تلاش کریں۔ اس کا صفحہ کھولتے ہی غلطیاں خود کھل جائیں گی۔",
                    "الأخطاء (Goofs) هي الهفوات التي تبقى في الأفلام (أخطاء الاستمرارية، أشياء لا تناسب الحقبة، وغيرها). ابحث عن فيلم أو مسلسل بالأعلى. عند فتح صفحته تُفتح أخطاؤه تلقائيًا.");
        } else {
            msg = tr("Quotes are the memorable lines of a movie or show. Search for a movie or show above. When you tap its page, its quotes open automatically.",
                    "مکالمے (Quotes) فلم یا شو کے یادگار جملے ہیں۔ اوپر کسی فلم یا شو کو تلاش کریں۔ اس کا صفحہ کھولتے ہی مکالمے خود کھل جائیں گے۔",
                    "الاقتباسات (Quotes) هي العبارات الشهيرة من الفيلم أو المسلسل. ابحث عن فيلم أو مسلسل بالأعلى. عند فتح صفحته تُفتح اقتباساته تلقائيًا.");
        }
        TextView t = tv(msg, 15, txt, false);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(0, 1.15f);
        t.setPadding(0, dp(12), 0, dp(18));
        b.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));
        TextView pop = button(tr("Pick from popular movies", "مقبول فلموں میں سے چنیں", "اختر من الأفلام الشائعة"),
                ACC, ON_ACC, v -> {
                    pendingSection = sec;
                    overlay.setVisibility(View.GONE);
                    overlay.removeAllViews();
                    webView.setVisibility(View.VISIBLE);
                    webView.onResume();
                    webView.loadUrl("https://www.imdb.com/chart/moviemeter/");
                });
        b.addView(pop, new LinearLayout.LayoutParams(MATCH, WRAP));
        return b;
    }

    private View buildSettings() {
        pLang = Store.lang(this);
        pTheme = Store.theme(this);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(bg);
        LinearLayout box = vbox();
        box.setPadding(dp(12), dp(10), dp(12), dp(16));
        sv.addView(box);

        final String[] langV = {"en", "ur", "ar"};
        LinearLayout s1 = section(box, tr("Language", "زبان", "اللغة"));
        s1.addView(chips(new String[]{"English", "اردو", "العربية"}, indexOf(langV, pLang), true,
                i -> pLang = langV[i]), new LinearLayout.LayoutParams(MATCH, WRAP));

        final String[] themeV = {"dark", "light"};
        LinearLayout s2 = section(box, tr("Theme", "تھیم", "المظهر"));
        s2.addView(chips(new String[]{tr("Dark", "ڈارک", "داكن"), tr("Light", "لائٹ", "فاتح")},
                indexOf(themeV, pTheme), true, i -> pTheme = themeV[i]), new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView save = button(tr("Save", "محفوظ کریں", "حفظ"), ACC, ON_ACC, v -> saveSettings());
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(MATCH, WRAP);
        sp.setMargins(0, dp(2), 0, dp(12));
        box.addView(save, sp);

        LinearLayout s3 = section(box, tr("Browsing data", "براؤزنگ ڈیٹا", "بيانات التصفح"));
        note(s3, tr("Cookies, cache and history are kept inside this app's browser. You can delete them here.",
                "کوکیز، کیش اور ہسٹری اسی ایپ کے براؤزر میں رہتی ہیں۔ یہاں سے مٹائی جا سکتی ہیں۔",
                "تبقى ملفات الكوكيز والتخزين المؤقت والسجل داخل متصفح التطبيق. يمكنك حذفها من هنا."));
        TextView clear = button(tr("Clear browsing data", "براؤزنگ ڈیٹا صاف کریں", "مسح بيانات التصفح"),
                chipBg, txt, v -> clearData());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(MATCH, WRAP);
        clp.setMargins(0, dp(8), 0, 0);
        s3.addView(clear, clp);

        LinearLayout s4 = section(box, tr("Privacy and about", "رازداری اور تعارف", "الخصوصية ونبذة"));
        note(s4, tr("This app is an unofficial browser for imdb.com and is not affiliated with IMDb. Every page is loaded directly from imdb.com (and any site it links to) inside the app, so those sites see your IP address and can set cookies. The app itself collects nothing, uses no location, and only remembers the last title you opened. Use \"Clear browsing data\" to delete cookies, cache and history.",
                "یہ ایپ imdb.com کا غیر سرکاری براؤزر ہے اور IMDb سے منسلک نہیں۔ ہر صفحہ براہِ راست imdb.com (اور جن سائٹس کے لنک ہوں) سے ایپ کے اندر لوڈ ہوتا ہے، اس لیے وہ سائٹس آپ کا IP ایڈریس دیکھ سکتی ہیں اور کوکیز رکھ سکتی ہیں۔ ایپ خود کچھ جمع نہیں کرتی، مقام استعمال نہیں کرتی، اور صرف آخری کھولا گیا ٹائٹل یاد رکھتی ہے۔ کوکیز، کیش اور ہسٹری مٹانے کے لیے \"براؤزنگ ڈیٹا صاف کریں\" دبائیں۔",
                "هذا التطبيق متصفح غير رسمي لموقع imdb.com وليس تابعًا لـ IMDb. تُحمَّل كل صفحة مباشرة من imdb.com (ومن أي موقع يرتبط به) داخل التطبيق، لذا ترى هذه المواقع عنوان IP الخاص بك وقد تحفظ ملفات تعريف الارتباط. التطبيق نفسه لا يجمع شيئًا ولا يستخدم الموقع ويتذكر فقط آخر عنوان فتحته. استخدم «مسح بيانات التصفح» لحذف الكوكيز والتخزين المؤقت والسجل."));
        return sv;
    }

    private void saveSettings() {
        Store.setLang(this, pLang);
        Store.setTheme(this, pTheme);
        cur = 3;
        build();
        toast(tr("Saved \u2713", "محفوظ ہو گیا \u2713", "تم الحفظ \u2713"));
    }

    private void clearData() {
        try {
            webView.clearCache(true);
            webView.clearHistory();
            WebStorage.getInstance().deleteAllData();
            CookieManager cm = CookieManager.getInstance();
            cm.removeAllCookies(null);
            cm.flush();
        } catch (Exception ignore) {
            // nothing more to clear
        }
        Store.setTt(this, "");
        Store.setTtName(this, "");
        updateTitleLabel();
        updateNav();
        toast(tr("Browsing data cleared.", "براؤزنگ ڈیٹا صاف ہو گیا۔", "تم مسح بيانات التصفح."));
    }

    // ------------------------------------------------------------------ dialogs

    private Dialog makeDialog(View contentView) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        contentView.setLayoutDirection(L.rtl(this) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        d.setContentView(contentView);
        Window w = d.getWindow();
        if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return d;
    }

    private void confirmExit() {
        LinearLayout box = vbox();
        box.setBackground(rounded(card, 16));
        box.setPadding(dp(20), dp(18), dp(20), dp(16));
        box.addView(tv(tr("Exit?", "باہر نکلیں؟", "الخروج؟"), 18, txt, true));
        TextView m = tv(tr("Close IMDb Browser?", "آئی ایم ڈی بی براؤزر بند کریں؟", "إغلاق متصفح IMDb؟"), 14, sub, false);
        m.setPadding(0, dp(8), 0, dp(14));
        box.addView(m);
        LinearLayout row = hbox();
        final Dialog d = makeDialog(box);
        addBtn(row, tr("No", "نہیں", "لا"), chipBg, txt, v -> d.dismiss());
        addBtn(row, tr("Yes", "ہاں", "نعم"), ACC, ON_ACC, v -> {
            d.dismiss();
            finish();
        });
        box.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
        d.show();
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9f), WRAP);
        }
    }
}
