# LowLevelDetector privacy policy

## Data collections

For me:
- No user personal privacy data will be collected on user's phone

For 3rd-parties:
> - https://developers.google.com/assistant/console/firebase-services
> - https://play.google.com/intl/en_us/about/privacy-security-deception
> - https://firebase.google.com/terms
> - https://firebase.google.com/support/privacy

- Google Play will collect some data for statistics if your device is running Google Play Service,  
  and Google Firebase will also collect some data for statistics if this app crashes or ANR.
  - These data are not requested by me forwardly;
  - I can see the backend report on their Consoles;
  - I will not share these data to any other people.

## Permissions

- android.permission.INTERNET
  - This permission is used to fetch data from remote server;
  - This permission is only used when user turns on the switch in settings manually;
  - Please notice that some 3rd-parties dependencies, such as Firebase,  
    still use this permission silently in the background.

- android.permission.QUERY_ALL_PACKAGES
  - This permission is used to fetch target api, package name, etc. of apps;
  - This permission is only used to retrieve prebuilt ROM system apps,  
    and no user installed apps will be retrieved.

- Used by `Play services`:
  - `Play services measurement`:
    - android.permission.ACCESS_NETWORK_STATE
    - android.permission.INTERNET
    - android.permission.WAKE_LOCK
    - com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE
  - `Play services measurement api`:
    - android.permission.ACCESS_ADSERVICES_AD_ID
    - android.permission.ACCESS_ADSERVICES_ATTRIBUTION
    - android.permission.ACCESS_NETWORK_STAT
    - android.permission.INTERNET
    - android.permission.WAKE_LOCK
    - com.google.android.gms.permission.AD_ID
  - `Play services ads identifier`:
    - com.google.android.gms.permission.AD_ID

- Used by `androidx.core`:
  - net.imknown.android.forefrontinfo.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION

## Security

- You can review all the code here, so I promise there are no things below inside:
  - trojan
  - backdoor
  - virus
  - any other bad things
