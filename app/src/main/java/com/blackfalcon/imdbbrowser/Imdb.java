package com.blackfalcon.imdbbrowser;

import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure URL helpers (no Android classes) so they can be tested separately. */
public class Imdb {

    public static final String HOME = "https://www.imdb.com/";

    private static final Pattern TITLE = Pattern.compile(
            "^https?://(?:[a-z0-9-]+\\.)?imdb\\.com/title/(tt\\d+)(/[^?#]*)?(?:\\?[^#]*)?(?:#.*)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IMDB_HOST = Pattern.compile(
            "^(?:https?://)?(?:[a-z0-9-]+\\.)*imdb\\.com(?:[/?#].*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TT_ONLY = Pattern.compile("^tt\\d{5,}$", Pattern.CASE_INSENSITIVE);

    /** Title id found in an imdb.com/title/ URL, or "". */
    public static String ttId(String url) {
        if (url == null) return "";
        Matcher m = TITLE.matcher(url);
        return m.matches() ? m.group(1).toLowerCase(Locale.US) : "";
    }

    /** True for the main page of a title (no sub-page such as /goofs or /fullcredits). */
    public static boolean isTitleRoot(String url) {
        if (url == null) return false;
        Matcher m = TITLE.matcher(url);
        if (!m.matches()) return false;
        String p = m.group(2);
        return p == null || p.length() == 0 || "/".equals(p);
    }

    /** True if the URL is the given sub-page (for example goofs) of the given title. */
    public static boolean isSection(String url, String tt, String sec) {
        if (url == null) return false;
        Matcher m = TITLE.matcher(url);
        if (!m.matches()) return false;
        if (!m.group(1).equalsIgnoreCase(tt)) return false;
        String p = m.group(2);
        if (p == null) return false;
        String lp = p.toLowerCase(Locale.US);
        return lp.equals("/" + sec) || lp.startsWith("/" + sec + "/");
    }

    public static String section(String tt, String sec) {
        return "https://www.imdb.com/title/" + tt + "/" + sec + "/";
    }

    public static String searchUrl(String q) {
        String enc;
        try {
            enc = URLEncoder.encode(q.trim(), "UTF-8");
        } catch (Exception e) {
            enc = q.trim();
        }
        return "https://www.imdb.com/find/?q=" + enc;
    }

    public static String https(String u) {
        if (u != null && u.regionMatches(true, 0, "http://", 0, 7)) return "https://" + u.substring(7);
        return u;
    }

    /** Turns what the user typed into a URL: a link, a title id, or an IMDb search. */
    public static String normalizeInput(String in) {
        String s = in == null ? "" : in.trim();
        if (s.length() == 0) return "";
        if (TT_ONLY.matcher(s).matches()) {
            return "https://www.imdb.com/title/" + s.toLowerCase(Locale.US) + "/";
        }
        if (s.indexOf(' ') < 0) {
            if (IMDB_HOST.matcher(s).matches()) {
                if (s.regionMatches(true, 0, "http", 0, 4)) return https(s);
                return "https://" + s;
            }
            if (s.regionMatches(true, 0, "https://", 0, 8) || s.regionMatches(true, 0, "http://", 0, 7)) {
                return https(s);
            }
        }
        return searchUrl(s);
    }

    /** "The Matrix (1999) - IMDb" becomes "The Matrix (1999)". */
    public static String cleanTitle(String t) {
        if (t == null) return "";
        String s = t.trim();
        String suffix = " - imdb";
        if (s.toLowerCase(Locale.US).endsWith(suffix)) {
            s = s.substring(0, s.length() - suffix.length());
        }
        return s.trim();
    }
}
