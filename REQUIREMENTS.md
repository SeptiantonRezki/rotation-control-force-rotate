# Requirement Aplikasi - Rotation Control: Force Rotate

**Revisi:** 3.0, 16 November 2026
**Status dokumen:** diselaraskan dengan produk global-only
**Menggantikan:** bagian teknis yang bertentangan pada PDF SRS awal dan keputusan produk terdahulu

Dokumen ini menjadi acuan kebutuhan produk dan teknis terbaru. Aplikasi kini menerapkan satu mode rotasi secara global. Pemilih profil per aplikasi, akses daftar paket, dan AccessibilityService telah dihapus. PDF awal tetap menjadi sumber tujuan produk yang masih berlaku; uraian teknis ini menggantikan rancangan yang tidak sesuai dengan platform Android atau keputusan produk terbaru.

## 1. Tujuan dan Batasan Produk

Rotation Control menyediakan kontrol orientasi sistem Android, penyimpanan mode terakhir, pemulihan saat service dijalankan kembali, dan pintasan notifikasi. Aplikasi dibuat native dengan Kotlin, XML, dan ViewBinding.

Android tidak menyediakan cara aplikasi biasa menetapkan `screenOrientation` untuk overlay `WindowManager.LayoutParams`. Karena itu aplikasi tidak memakai overlay transparan, `TYPE_APPLICATION_OVERLAY`, atau izin `SYSTEM_ALERT_WINDOW` untuk memutar layar. Service memakai akses khusus **Modify system settings** (`WRITE_SETTINGS`) untuk mengubah `ACCELEROMETER_ROTATION` dan `USER_ROTATION`.

Perubahan pengaturan rotasi bersifat system-wide, tetapi Android, aplikasi tertentu, layar kunci, OEM, atau kebijakan perangkat dapat membatasi efeknya. Produk tidak menjamin setiap aplikasi dan permukaan sistem selalu dapat diputar.

## 2. Kebutuhan Fungsional

| ID | Kebutuhan yang berlaku | Status / catatan |
|---|---|---|
| FR-01 | Halaman Control menyediakan sembilan mode global: Guard Mode, Auto-Rotate, Landscape, Portrait, Reverse Portrait 180°, Sensor Landscape, Reverse Landscape, Sensor Portrait, dan Lock Current. | Diimplementasikan. Efek akhir dapat dibatasi Android/OEM. |
| FR-02 | Auto-Rotate dan Guard Mode mengikuti kebijakan sensor Android. Mode arah tetap menulis rotasi yang dipilih. Mode sensor terbatas memakai orientasi perangkat untuk memilih rotasi yang didukung mode tersebut. | Diimplementasikan melalui `Settings.System` dan `OrientationEventListener`, bukan overlay. |
| FR-03 | Pengguna harus memberikan akses Modify system settings sebelum aplikasi mulai mengubah orientasi. Bila akses belum ada, aplikasi mengarahkan pengguna ke pengaturan Android dan mencoba lagi setelah pengguna kembali. | Diimplementasikan. |
| FR-04 | Aplikasi menyimpan status service, mode global terakhir, timestamp, preferensi call bypass, dan hingga sepuluh pilihan rotasi terakhir di SharedPreferences pada device-protected storage. | Diimplementasikan. Profil aplikasi lama dihapus saat aplikasi diperbarui. |
| FR-05 | Jika service sebelumnya aktif dan izin Modify system settings tersedia, receiver menangani `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, dan `MY_PACKAGE_REPLACED` untuk mencoba memulihkan mode terakhir. | Diimplementasikan dengan batasan kebijakan Android/OEM dan status izin. |
| FR-06 | Foreground service menampilkan notifikasi persisten dengan mode aktif dan aksi untuk membuka `MainActivity` tanpa membuat tumpukan Activity ganda. | Diimplementasikan; service menggunakan tipe `specialUse`. |
| FR-07 | Call bypass bersifat opsional. Setelah disclosure dan izin `READ_PHONE_STATE`, service memakai Auto-Rotate saat panggilan `RINGING` atau `OFFHOOK`, lalu menerapkan kembali mode pilihan saat panggilan berakhir. Nomor telepon dan riwayat panggilan tidak digunakan. | Diimplementasikan; tidak aktif sebelum pengguna mengizinkan. |
| FR-08 | Iklan hanya berupa Adaptive Banner dan Native Advanced. Tidak ada interstitial, app-open, atau iklan layar penuh. Satu banner view dipindahkan ke halaman aktif dan tiap halaman memiliki slot native. | Diimplementasikan. Banner berada di alur konten, bukan sticky di atas navigation bar. |
| FR-09 | Saat aplikasi dibuka, UMP memperbarui status consent sebelum permintaan iklan. Tombol Privacy options tersedia bila diwajibkan UMP. | Diimplementasikan; ketersediaan dan isi iklan ditentukan jaringan iklan dan consent. |

## 3. Kebutuhan UI dan Perilaku

- Tema memakai palet gelap statis bergaya Ethereal Glassmorphism: latar gradasi indigo-ungu, kartu transparan berbingkai lembut, aksen lavender dan mint. Dynamic Color runtime tidak digunakan agar palet produk konsisten.
- Tidak ada orb/status circle animasi. Halaman tidak menampilkan judul dekoratif; label konteks seperti “Choose rotation” tetap digunakan di Control.
- Control menampilkan status service, switch service, grid mode rotasi tiga kolom, satu slot native ad, dan satu slot banner.
- Settings berisi status/izin rotasi sistem, auto-start, call bypass opsional, privacy options, privacy policy, slot iklan, dan info aplikasi.
- Bottom navigation memiliki dua item: Control dan Settings. Tinggi bar konten 60 dp, label selalu terlihat, indikator pill aktif tinggi dinonaktifkan, dan ripple dibuat lembut.
- Kedua halaman dapat digulir; banner tetap berada di konten halaman dan tidak menutupi bottom navigation.

## 4. Kebutuhan Lifecycle dan Data

- Saat Activity dihancurkan, listener UI dan listener inset dilepas, kemudian view iklan dihancurkan.
- Saat foreground service dihentikan, listener telepon dilepas dan `OrientationEventListener` dinonaktifkan.
- Data lama untuk profil aplikasi serta persetujuan visibilitas paket dihapus lokal saat aplikasi versi global-only dijalankan.
- Kriteria penerimaan: navigasi halaman, perubahan konfigurasi, pelepasan Activity, dan penghentian service tidak boleh meninggalkan callback/listener milik komponen yang sudah berakhir atau menghasilkan crash/ANR/`WindowLeaked` pada smoke check.

## 5. Platform, Build, dan Monetisasi

- Minimum SDK API 26; compile SDK dan target SDK API 36.
- Kotlin/JVM 17, Android Views/XML + ViewBinding, dan Material 3. Tidak menggunakan Jetpack Compose atau Flutter.
- Release mengaktifkan R8/minify dan resource shrinking. Release signing memakai `keystore.properties` atau environment variables; kredensial tidak disimpan di source control.
- Packaging native library menggunakan konfigurasi non-legacy. Target ukuran <3,5 MB adalah sasaran optimasi, bukan jaminan karena dependensi Google Mobile Ads/UMP. Ukuran artefak dan kompatibilitas page size 16 KB harus diperiksa setelah build release.
- AdMob App ID: `ca-app-pub-3359147804072195~4132266714`.
- Banner Ad Unit ID: `ca-app-pub-3359147804072195/5253776691`.
- Native Advanced Ad Unit ID: `ca-app-pub-3359147804072195/6922081582`.
- Aplikasi tidak meminta `QUERY_ALL_PACKAGES` atau mendeklarasikan AccessibilityService. Izin khusus Modify system settings, `READ_PHONE_STATE` opsional, notifikasi, dan foreground service `specialUse` tetap perlu dijelaskan dengan akurat di Play Console.

## 6. Kriteria Penerimaan Rilis

1. Build debug dan release dapat dikompilasi; release harus ditandatangani upload key pemilik aplikasi.
2. Pilihan mode global terlihat pada status/control dan tersimpan; mode dapat dipulihkan saat startup bila service aktif serta izin sistem tersedia.
3. Notification action membuka MainActivity tanpa membuat tumpukan Activity ganda.
4. Navigasi hanya menampilkan Control dan Settings; tidak ada pemilih aplikasi/profil atau pengaturan Aksesibilitas.
5. Tidak ada `QUERY_ALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`, atau AccessibilityService pada manifest aplikasi.
6. Tidak ada interstitial/app-open/full-screen ad; Banner/Native menunggu keputusan UMP.
7. Lifecycle audit memastikan callback, iklan, sensor, dan listener telepon dilepas sesuai lifecycle.
8. Sebelum publikasi, verifikasi signing, deklarasi `specialUse`, Data Safety/ads, ukuran artefak, dan kompatibilitas library 16 KB.

## 7. Verifikasi dan Batas Status

Build pengganti untuk kode versi 1 yang telah dipakai di Play Console menggunakan `versionCode` 2 dan `versionName` 1.0.1. Pada 16 November 2026, `:app:assembleDebug`, `:app:bundleRelease`, dan `:app:assembleRelease` berhasil. APK release berukuran 4,083,919 byte (3.89 MiB), lebih besar dari sasaran 3.5 MB; AAB berukuran 6,829,270 byte (6.51 MiB). APK lolos verifikasi apksigner dan pemeriksaan alignment 16 KB; AAB lolos verifikasi jarsigner dengan sertifikat upload key lokal. Analyzer mengonfirmasi code 2, version name 1.0.1, dan target API 36. Manifest hasil release tidak berisi `QUERY_ALL_PACKAGES` atau deklarasi AccessibilityService. AAB baru belum diunggah ke Play Console.

Smoke check pada emulator API 35 memakai aplikasi terbaru: Control terbuka, pilihan mode dapat diubah, dan navigasi Settings berfungsi tanpa force close. Ini memverifikasi tampilan dan perpindahan tab pada emulator, bukan seluruh perilaku di perangkat fisik.

Perilaku rotasi lintas aplikasi, jeda panggilan, boot pada perangkat fisik, persetujuan Play Console untuk foreground service, formulir Data Safety/Ads, dan perilaku OEM masih memerlukan verifikasi terpisah. Aplikasi tidak menjamin semua aplikasi, layar kunci, atau perangkat Android akan mengikuti pilihan orientasi global.
