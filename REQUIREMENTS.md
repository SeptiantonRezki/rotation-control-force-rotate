# Requirement Aplikasi - Rotation Control: Force Rotate

**Revisi:** 2.0, 16 November 2026
**Status dokumen:** diselaraskan dengan implementasi proyek saat ini
**Menggantikan:** bagian teknis yang bertentangan pada PDF SRS awal

Dokumen ini menjadi acuan kebutuhan produk dan teknis yang terbaru. PDF awal tetap menjadi sumber tujuan produk dan strategi ASO, tetapi uraian teknis di bawah menggantikan pendekatan yang tidak sesuai dengan implementasi Android saat ini. Pernyataan seperti “memutar 100% semua layar” diperlakukan sebagai tujuan, bukan jaminan lintas perangkat.

## 1. Tujuan dan Batasan Produk

Rotation Control menyediakan kontrol orientasi sistem Android, profil rotasi per aplikasi, penyimpanan mode terakhir, dan jalan pintas notifikasi. Aplikasi dibuat native dengan Kotlin, XML, dan ViewBinding.

Android tidak menyediakan cara aplikasi biasa menetapkan `screenOrientation` untuk jendela overlay di `WindowManager.LayoutParams`. Karena itu aplikasi **tidak** memakai overlay transparan `0x0`, `TYPE_APPLICATION_OVERLAY`, atau izin `SYSTEM_ALERT_WINDOW` untuk memutar layar. Service memakai izin khusus **Modify system settings** (`WRITE_SETTINGS`) untuk mengubah `ACCELEROMETER_ROTATION` dan `USER_ROTATION`.

Perubahan pengaturan rotasi bersifat system-wide, tetapi Android, aplikasi tertentu, layar kunci, OEM, atau kebijakan perangkat dapat membatasi efeknya. Produk tidak menjamin setiap aplikasi dan permukaan sistem selalu dapat diputar.

## 2. Kebutuhan Fungsional

| ID | Kebutuhan yang berlaku | Status / catatan |
|---|---|---|
| FR-01 | Halaman Control menyediakan sembilan mode global: Guard Mode, Auto-Rotate, Landscape, Portrait, Reverse Portrait, Sensor Landscape, Reverse Landscape, Sensor Portrait, dan Lock Current. Memilih mode memperbarui status aktif, menyimpan pilihan, lalu menerapkan pengaturan rotasi sistem. | Diimplementasikan. Efek akhir dapat dibatasi Android/OEM. |
| FR-02 | Auto-Rotate dan Guard Mode mengikuti kebijakan sensor Android. Mode arah tetap menulis rotasi yang dipilih. Mode sensor terbatas memakai orientasi perangkat untuk memilih rotasi yang didukung mode tersebut. | Diimplementasikan melalui `Settings.System` dan `OrientationEventListener`, bukan overlay. |
| FR-03 | Pengguna harus memberikan akses Modify system settings sebelum aplikasi mulai mengubah orientasi. Bila akses belum ada, aplikasi mengarahkan pengguna ke pengaturan Android dan mencoba lagi setelah pengguna kembali. | Diimplementasikan. |
| FR-04 | Aplikasi menyimpan status service, mode global terakhir, profil per aplikasi, timestamp, dan hingga sepuluh pilihan rotasi terakhir di SharedPreferences dalam device-protected storage. | Diimplementasikan. Data riwayat dipangkas menjadi sepuluh entri. |
| FR-05 | Jika service sebelumnya aktif dan izin Modify system settings tersedia, receiver menangani `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, dan `MY_PACKAGE_REPLACED` untuk mencoba memulihkan mode terakhir. Kegagalan start oleh kebijakan sistem/OEM tidak menghapus preferensi tersimpan; aplikasi dapat mencoba lagi saat dibuka. | Diimplementasikan dengan batasan kebijakan Android dan status izin. |
| FR-06 | Foreground service menampilkan notifikasi persisten dengan mode aktif dan aksi untuk membuka `MainActivity`. Aksi membuka instance yang ada bila memungkinkan, menggunakan `SINGLE_TOP` dan `CLEAR_TOP`. | Diimplementasikan; service menggunakan tipe `specialUse`. |
| FR-07 | Sebelum membuka daftar Per-App pertama kali, aplikasi menampilkan disclosure untuk visibilitas daftar paket. Setelah pengguna melanjutkan, daftar memuat aplikasi yang memiliki launcher activity, menyediakan pencarian, dan menyimpan pilihan mode tiap paket secara lokal. | Diimplementasikan. Daftar dipindai di background; system package tanpa launcher tidak ditampilkan. |
| FR-08 | Penerapan profil Per-App bersifat opt-in. Setelah membaca disclosure aksesibilitas, pengguna sendiri mengaktifkan `RotationAccessibilityService` pada pengaturan Android. Service menggunakan event perubahan jendela untuk membaca nama paket foreground saja; tidak membaca teks/window content dan tidak mengirim daftar aplikasi ke analitik atau permintaan iklan aplikasi. | Diimplementasikan; pengaktifan aksesibilitas tetap tindakan pengguna. |
| FR-09 | Saat aplikasi dengan profil berpindah ke foreground, mode profil diterapkan. Aplikasi tanpa profil dan aplikasi Rotation Control menggunakan mode global terakhir. Package `com.android.systemui` diabaikan. | Diimplementasikan melalui AccessibilityService. Efeknya bergantung pada layanan yang diaktifkan pengguna. |
| FR-10 | Untuk package installer yang dikenali, layanan aksesibilitas sementara memilih Auto-Rotate. Pada event package foreground berikutnya, service menerapkan kembali profil paket atau mode global. | Diimplementasikan dengan daftar package dan heuristic nama installer; tidak menjamin deteksi semua installer OEM. Tidak ada overlay yang menutupi tombol Install. |
| FR-11 | Call bypass bersifat opsional. Setelah disclosure dan izin `READ_PHONE_STATE`, service memakai mode Auto-Rotate saat status panggilan `RINGING` atau `OFFHOOK`, lalu menerapkan kembali mode pilihan saat status kembali `IDLE`. Nomor telepon dan call history tidak digunakan. | Diimplementasikan; tidak aktif sebelum pengguna mengizinkan. |
| FR-12 | Iklan hanya berupa Adaptive Banner dan Native Advanced. Tidak ada interstitial, app-open, atau iklan layar penuh. Satu banner view dipindahkan ke halaman yang sedang dibuka; tiap halaman menyediakan slot native ad. | Diimplementasikan. Banner mengikuti konten halaman dan bukan elemen sticky tetap di atas navigation bar. |
| FR-13 | Saat aplikasi dibuka, UMP memperbarui status consent dan menampilkan form yang diwajibkan. Permintaan iklan baru dilakukan bila UMP mengizinkan; tombol Privacy options tersedia jika UMP mewajibkannya. | Diimplementasikan; ketersediaan/isi iklan ditentukan jaringan iklan dan consent. |

## 3. Kebutuhan UI dan Perilaku

- Tema memakai palet gelap statis bergaya Ethereal Glassmorphism: latar gradasi indigo-ungu, kartu transparan berbingkai lembut, aksen lavender dan mint. Warna Material You Dynamic Color runtime tidak digunakan agar palet produk konsisten.
- Tidak ada orb/status circle animasi. Halaman tidak menampilkan judul halaman dekoratif; label konteks seperti “Choose rotation” tetap digunakan di Control.
- Control menampilkan grid rotasi 3 kolom, status service, serta satu slot native ad dan satu slot banner.
- Per-App menampilkan search field, daftar kartu aplikasi yang dapat digulir, pilihan rotasi berbentuk exposed dropdown dengan nama mode, deskripsi, dan penanda pilihan aktif. Daftar/ikon dimuat bertahap agar pembukaan dan scroll tetap responsif.
- Area daftar Per-App menggunakan viewport RecyclerView 360 dp di dalam NestedScrollView; konten panjang dan landscape tetap dapat digulir. Tinggi viewport dapat disesuaikan bila pengujian perangkat nyata menemukan ruang layar yang tidak memadai.
- Bottom navigation memakai bar mengambang dengan tinggi konten 60 dp, label Control/Per-App/Settings tetap terlihat, indikator pill aktif tinggi dinonaktifkan, dan ripple dibuat lembut.
- Slot native ad dan banner tetap berada pada alur konten setiap halaman; banner tidak mengambang di atas navigation bar.

## 4. Kebutuhan Lifecycle dan Ketahanan Memori

- Saat Activity dihancurkan, TextWatcher dan listener UI dilepas, listener inset dihapus, task pemindaian aplikasi dibatalkan, executor dihentikan, adapter/callback/cache dibersihkan, popup dropdown ditutup saat view dilepas, dan view iklan dihancurkan.
- Saat adapter dilepas dari RecyclerView, worker pemuatan ikon dihentikan dan callback ke Activity dihapus. Cache ikon dibatasi hingga 32 item.
- Saat foreground service dihentikan, listener telepon dilepas dan `OrientationEventListener` dinonaktifkan.
- Saat AccessibilityService dihentikan atau dilepas, referensi store dan state paket foreground dibersihkan.
- Kriteria penerimaan: perpindahan halaman, perubahan konfigurasi, pelepasan Activity, dan penghentian service tidak boleh meninggalkan callback/listener milik komponen yang sudah berakhir atau menghasilkan crash/ANR/`WindowLeaked` pada smoke check.

## 5. Platform, Build, dan Monetisasi

- Minimum SDK API 26; compile SDK dan target SDK 35.
- Kotlin/JVM 17, Android View/XML + ViewBinding, Material 3, dan RecyclerView. Tidak menggunakan Jetpack Compose atau Flutter.
- Release mengaktifkan R8/minify dan resource shrinking; entry point yang dipanggil framework dipertahankan melalui aturan R8/manifest. Release signing memakai `keystore.properties` atau environment variables, bukan kredensial yang disimpan di source control.
- Packaging native library menggunakan konfigurasi non-legacy. Ukuran unduhan di bawah 3,5 MB tetap merupakan target optimasi, bukan kriteria yang dijamin karena dependensi Google Mobile Ads/UMP dan library native. Ukuran serta kompatibilitas page size 16 KB harus diperiksa pada artefak release final.
- AdMob App ID: `ca-app-pub-3359147804072195~4132266714`.
- Banner Ad Unit ID: `ca-app-pub-3359147804072195/5253776691`.
- Native Advanced Ad Unit ID: `ca-app-pub-3359147804072195/6922081582`.
- `QUERY_ALL_PACKAGES`, AccessibilityService, `READ_PHONE_STATE`, dan `specialUse` foreground service harus dicantumkan dan dijelaskan sesuai deklarasi Play Console. Disclosure di aplikasi tidak menggantikan pengajuan deklarasi atau persetujuan Google Play.

## 6. Kriteria Penerimaan Rilis

1. Build debug dan release dapat dikompilasi; release hanya dianggap siap diunggah setelah ditandatangani upload key milik pemilik aplikasi.
2. Perubahan mode global terlihat pada status/control dan tersimpan; mode dapat dipulihkan saat startup jika status service aktif serta izin sistem tersedia.
3. Notification action membuka MainActivity tanpa membuat tumpukan Activity ganda.
4. Disclosure muncul sebelum akses daftar paket dan sebelum pengguna diarahkan untuk mengaktifkan AccessibilityService.
5. Pencarian, pilihan dropdown, state aktif, dan navigasi Control/Per-App/Settings dapat digunakan tanpa force close.
6. Tidak ada interstitial/app-open/full-screen ad; pemuatan Banner/Native menunggu keputusan UMP.
7. Lifecycle audit memastikan task, callback, popup, iklan, sensor, dan listener telepon dilepas pada lifecycle yang sesuai.
8. Sebelum publikasi, verifikasi ukuran final, penandatanganan, kebijakan izin terbatas, deklarasi Accessibility/FGS, dan kompatibilitas library 16 KB pada artefak release.

## 7. Verifikasi dan Batas Status

Perubahan UI terbaru sudah dibangun sebagai debug APK, dipasang pada emulator API 35, dan diperiksa pada halaman Per-App termasuk pemilihan dropdown serta perpindahan ke Settings. Smoke check tersebut tidak menampilkan fatal exception, ANR, atau `WindowLeaked` di logcat. Pemeriksaan itu bukan pengganti heap profiling pada banyak perangkat/OEM.

Alur panggilan telepon, deteksi installer OEM, boot di perangkat fisik, persetujuan Play Console untuk izin terbatas, ukuran artefak release, dan penandatanganan production tetap memerlukan verifikasi terpisah sebelum rilis. Perilaku rotasi lintas aplikasi tidak dapat dijamin seragam pada setiap perangkat Android.
