# Google Play Store listing draft

## Store assets

- App name: **Rotation Control: Force Rotate** (30/30 characters)
- Short description (EN): **Set screen rotation modes, create per-app profiles, and restore your saved mode.**
- Short description (ID): **Atur rotasi layar, buat profil per aplikasi, dan pulihkan mode pilihan Anda.**
- App icon: [`play-store-assets/app-icon/rotation-control-icon-512.png`](play-store-assets/app-icon/rotation-control-icon-512.png), 512 × 512 PNG
- Feature graphic: [`play-store-assets/feature-graphic/rotation-control-feature-graphic.png`](play-store-assets/feature-graphic/rotation-control-feature-graphic.png), 1024 × 500 PNG
- Screenshots: [`play-store-assets/screenshots/`](play-store-assets/screenshots/)
- Privacy policy: `https://septiantonrezki.github.io/rotation-control-force-rotate/privacy-policy.html` (deployed and verified public)
- Support contact: `septianrezki17@gmail.com`

## Full description — English

Choose the screen orientation that works for you.

Rotation Control gives you quick access to nine rotation modes, optional per-app profiles, and a way to restore your saved choice when the service starts again.

• Choose Auto-Rotate, Portrait, Reverse Portrait, Landscape, Sensor Landscape, Reverse Landscape, Sensor Portrait, Lock Current, or Guard Mode.
• Create optional rotation profiles for individual apps. Enable Android Accessibility access if you want a profile applied when an app comes to the foreground.
• Use the ongoing notification as a shortcut back to the controls.
• Restore your last selected mode after device startup when the rotation service is enabled.
• Optionally let the service pause its override during a phone call.
• See banner and native ads within app pages. The app does not use interstitial or app-open ads.

**Permissions and compatibility**

Modify system settings is needed to change Android's rotation settings. Per-App profiles use installed-app visibility to show launchable apps in the picker. The optional Accessibility service observes the foreground app package to apply your chosen profile and handle package installer screens; it does not read screen text or window content. The optional call bypass uses phone-state access to detect call state only.

Android and individual apps may enforce their own orientation behavior. Results can vary by device, Android version, and the app or system screen in use.

On Android 16 or later on large-screen displays (600 dp or wider), the platform may ignore an individual app's orientation restrictions. Per-app rotation behavior can therefore differ on tablets, foldables, and desktop-sized windows.

## Deskripsi lengkap — Bahasa Indonesia

Pilih orientasi layar yang paling nyaman untuk Anda.

Rotation Control memberi akses cepat ke sembilan mode rotasi, profil opsional per aplikasi, dan pemulihan pilihan Anda saat layanan dijalankan kembali.

• Pilih Auto-Rotate, Portrait, Reverse Portrait, Landscape, Sensor Landscape, Reverse Landscape, Sensor Portrait, Lock Current, atau Guard Mode.
• Buat profil rotasi opsional untuk aplikasi tertentu. Aktifkan Aksesibilitas Android jika Anda ingin profil diterapkan saat aplikasi terbuka.
• Gunakan notifikasi tetap sebagai pintasan untuk kembali ke kontrol aplikasi.
• Pulihkan mode terakhir setelah perangkat dinyalakan jika layanan rotasi aktif.
• Secara opsional, jeda penguncian rotasi selama panggilan telepon.
• Iklan banner dan native ditampilkan di dalam halaman aplikasi. Aplikasi tidak menggunakan iklan interstitial atau app-open.

**Izin dan kompatibilitas**

Akses Ubah setelan sistem diperlukan untuk mengubah pengaturan rotasi Android. Profil per aplikasi menggunakan akses visibilitas aplikasi terpasang untuk menampilkan aplikasi yang dapat dibuka. Layanan Aksesibilitas opsional memantau nama paket aplikasi di latar depan untuk menerapkan profil pilihan dan menangani layar installer; layanan ini tidak membaca teks atau isi jendela. Fitur jeda panggilan opsional hanya menggunakan status panggilan.

Android dan masing-masing aplikasi dapat menerapkan kebijakan orientasinya sendiri. Hasil bisa berbeda menurut perangkat, versi Android, serta aplikasi atau layar sistem yang digunakan.

Pada Android 16 atau lebih baru di layar besar (lebar minimal 600 dp), sistem dapat mengabaikan batas orientasi yang ditetapkan aplikasi tertentu. Karena itu, perilaku profil per aplikasi bisa berbeda di tablet, perangkat lipat, dan jendela desktop.

## Suggested Play Console declarations

- App category: Tools
- Ads: Yes
- Privacy policy URL: the URL above, after Pages deployment is verified
- Accessibility API: disclose and describe only foreground package detection; the service is optional and is not an accessibility tool
- `QUERY_ALL_PACKAGES`: explain the Per-App profile picker as core functionality; Play approval is required
- Foreground service: declare `specialUse` with persistent user-requested rotation control
- Data safety: complete against the exact Google Mobile Ads/UMP SDK versions and Play's current SDK data disclosure; this listing draft is not a substitute for the Play Console Data Safety form
- Content rating, target audience, app access, and ads settings: complete in Play Console before production release
