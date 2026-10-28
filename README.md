# IMDb Browser

A simple Android app (Java only, no AndroidX, no external libraries) that browses imdb.com inside the app,
with dedicated **Goofs** and **Quotes** tabs. All links open inside the app.
Package: `com.blackfalcon.imdbbrowser`. UI languages: English (default), Urdu, Arabic.
Unofficial: not affiliated with IMDb.

## What the app does
- **Browse tab**: the full imdb.com website in an in-app browser. Search box (type a title, an IMDb link or a title id like `tt0111161`),
  quick links (Home, Top 250, Popular, Top TV), back / forward / reload / home buttons, loading bar, error message, trailer full-screen support.
- **Goofs tab**: opens the goofs page of the current title.
- **Quotes tab**: opens the quotes page of the current title.
- "Current title" is detected from the page you are viewing (and remembered for the next launch).
  If no title is known yet, the tab asks you to search; once you tap the title's page, the Goofs or Quotes page opens automatically.
- **Links**: every web link stays inside the app (http links are upgraded to https). Links that are not web pages
  (for example mailto: or app-store links) cannot be opened inside the app, and the app says so.
- **Settings**: language (English default), dark/light theme (dark default), Save button, and "Clear browsing data".
- Exit confirmation on Back (Back first goes back in the page history); "By: Black Falcon" on the main screen; Splash screen first.

## How to build
1. Upload the contents of this folder to a GitHub repository (the folder with `build.gradle` and `.github` must be the repository root).
2. Open the **Actions** tab and run **Build APK** (it also runs on every push).
3. Download the `ImdbBrowser-APK` artifact and install the APK.

## Privacy
- Pages are loaded directly from imdb.com (and any site it links to), so those sites see your IP address and may set cookies.
- The app itself collects nothing, uses no location, and stores only the language, theme and the last title you opened.
- Settings > Clear browsing data deletes cookies, cache and history.

## Limitations (honest list)
- Internet is required; there is no offline mode.
- The app only shows imdb.com's own website. Its layout, ads, cookie banners, sign-in and the exact Goofs/Quotes page addresses
  are controlled by IMDb and can change. The page addresses used here (`/title/<id>/goofs/` and `/title/<id>/quotes/`)
  were not tested against the live site while building this app.
- IMDb may ask for a robot check or consent from some networks; that is IMDb's behavior, not the app's.
- Signing in with Google/Amazon accounts may not work inside an in-app WebView.
- Portrait only, so full-screen trailers also stay in portrait.
- No video downloads, no sharing of pages, and no bookmarks (kept simple).

## اردو (مختصر)
- یہ ایپ imdb.com کو ایپ کے اندر ہی براؤز کرتی ہے؛ تمام لنک ایپ کے اندر کھلتے ہیں۔
- "غلطیاں" ٹیب موجودہ فلم کی Goofs اور "مکالمے" ٹیب اس کی Quotes کھولتا ہے۔ اگر فلم معلوم نہ ہو تو تلاش کریں؛ فلم کا صفحہ کھولتے ہی متعلقہ سیکشن خود کھل جاتا ہے۔
- انٹرنیٹ ضروری ہے۔ ویب سائٹ کا ڈیزائن اور صفحات IMDb کے اختیار میں ہیں، اس لیے بدل سکتے ہیں۔
- ایپ خود کچھ جمع نہیں کرتی؛ کوکیز اور ہسٹری سیٹنگز سے صاف کی جا سکتی ہیں۔
