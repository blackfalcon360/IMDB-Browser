package com.blackfalcon.imdbbrowser;

import android.content.Context;

/** Language helper: every UI string is given in English, Urdu and Arabic. */
public class L {

    public static String t(Context c, String en, String ur, String ar) {
        String lang = Store.lang(c);
        if ("ur".equals(lang)) return ur;
        if ("ar".equals(lang)) return ar;
        return en;
    }

    public static boolean rtl(Context c) {
        String lang = Store.lang(c);
        return "ur".equals(lang) || "ar".equals(lang);
    }
}
