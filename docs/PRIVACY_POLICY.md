# Dot Widgets — Privacy Policy

_Last updated: 21 October 2026_

Dot Widgets ("the app") is a free home-screen and lock-screen widget app for Android, made by an
independent developer. This policy explains what data the app uses, where it goes, and the choices
you have.

**In short:** the app has no accounts, no ads, no analytics and no crash reporting. It has no
server of its own. Most of what it shows is read and drawn on your phone. A few widgets fetch public
data (such as weather) from the third-party services listed below.

## 1. Data that stays on your phone

The following is read only to draw your widgets. It is not sent anywhere by the app:

| Data | Used for | Permission |
|---|---|---|
| Calendar events | Agenda and timeline widgets | Calendar (optional) |
| Contacts (starred and chosen contacts) | Favourite-contact widgets | Contacts (optional) |
| Step count | Step counter and activity-ring widgets | Physical activity (optional) |
| App usage and mobile-data use | Screen-On Time, Top Apps Today and Mobile Data Today widgets | Usage access (optional) |
| Currently playing media (title, artist, playback state) | Now Playing and media-control widgets | Notification access (optional). The app reads media sessions only; it does not read the content of your notifications. |
| Battery, Bluetooth device battery, Wi-Fi, Do Not Disturb and similar device state | System and toggle widgets | Bluetooth, Do Not Disturb access (optional) |
| Notes, to-dos, habits, counters, photos you pick and other widget settings | The widgets you configure | None |

This data is stored in the app's private storage on your phone. Widget settings may be included in
your Android device backup (Google's backup service, if you have it turned on). Your Home Assistant
server address and token are excluded from backups.

## 2. Data sent to third-party services

Some widgets need data from the internet. The app connects directly to these services. No account
or identifier from the app is sent with these requests, but, as with any internet request, the
service can see your IP address.

| Service | What is sent | Used for |
|---|---|---|
| [Open-Meteo](https://open-meteo.com/) (forecast, air quality, geocoding) | Approximate coordinates of your chosen place, or the city name you search for | Weather, sunrise/sunset, air-quality widgets |
| [CoinGecko](https://www.coingecko.com/) | The coin symbols you follow | Crypto price widgets |
| [ExchangeRate-API](https://www.exchangerate-api.com/) (open.er-api.com) | Nothing beyond the request itself | Currency widgets |
| [Stooq](https://stooq.com/) | The stock symbols you follow | Stock widgets |
| Android's built-in geocoder (provided by Google on most phones) | Approximate coordinates, when you use "use my location" | Turning your location into a place name |

**Location.** If you choose "use my location" for weather, the app reads your **approximate**
(coarse) location and sends those coordinates to Open-Meteo to get your forecast, and to your phone's geocoder to
name the place. The app does not
use precise location and does not track your location in the background. You can instead search for
a city, or let the app guess from your time zone.

Each service has its own privacy policy, which applies to the requests it receives.

## 3. Servers you add yourself

- **Home Assistant.** If you connect a Home Assistant server, the app sends your access token only
  to the server address you enter. The token is stored encrypted on your phone and is never backed
  up. You can disconnect at any time in Settings → Connections.
- **Sports feed.** If you add a sports feed URL, the app downloads data from that address.

## 4. What the app does not do

- It does not sell or share your personal data.
- It does not show ads or use advertising identifiers.
- It does not include analytics or crash-reporting tools.
- It does not require you to create an account.

## 5. Permissions are optional

Every permission is asked for only when you turn on a feature that needs it, and the app works
without it. You can revoke any permission in Android Settings → Apps → Dot Widgets → Permissions.

## 6. Deleting your data

All app data is on your phone. To delete it, remove the widgets and either clear the app's storage
(Android Settings → Apps → Dot Widgets → Storage → Clear storage) or uninstall the app.

## 7. Children

The app is not directed at children under 13 and does not knowingly collect data from them.

## 8. Not affiliated with Nothing

Dot Widgets is an independent app. It is not affiliated with, endorsed by, or sponsored by Nothing
Technology Limited. "Nothing", "Nothing OS" and "Glyph" are trademarks of their owner.

## 9. Changes

If this policy changes, the updated version will be posted at this address with a new date.

## 10. Contact

Questions about this policy: SUPPORT_EMAIL_HERE
