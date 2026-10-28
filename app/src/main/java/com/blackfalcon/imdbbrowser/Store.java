package com.blackfalcon.imdbbrowser;

import android.content.Context;
import android.content.SharedPreferences;

public class Store {

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("imdbbrowser", Context.MODE_PRIVATE);
    }

    private static String gs(Context c, String k, String d) { return p(c).getString(k, d); }
    private static void ps(Context c, String k, String v) { p(c).edit().putString(k, v).apply(); }

    public static String lang(Context c) { return gs(c, "lang", "en"); }
    public static void setLang(Context c, String v) { ps(c, "lang", v); }

    public static String theme(Context c) { return gs(c, "theme", "dark"); }
    public static void setTheme(Context c, String v) { ps(c, "theme", v); }

    /** Last title id (for example tt0111161) seen while browsing. */
    public static String tt(Context c) { return gs(c, "tt", ""); }
    public static void setTt(Context c, String v) { ps(c, "tt", v); }

    public static String ttName(Context c) { return gs(c, "ttName", ""); }
    public static void setTtName(Context c, String v) { ps(c, "ttName", v); }
}
