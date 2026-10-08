# 🎓 UniFlow - Üniversite Kulüp ve Etkinlik Yönetim Platformu

UniFlow, üniversite öğrencilerinin kampüs kulüplerini keşfetmelerine, etkinliklere katılmalarına ve kulüp yöneticilerinin etkinlik/başvuru süreçlerini kolayca yönetmelerine olanak tanıyan modern bir **Android** uygulamasıdır.

Öğrenci, kayıt olup giriş yaptıktan sonra üniversite ve profil bilgilerini doldurur. Ardından kendi üniversitesine bağlı tüm kulüpleri ve etkinlikleri görüntüleyebilir, etkinliklere katılabilir ve kulüplere üye olabilir.

Öğrenci, kulüp başvurusu yaparak kendi üniversitesine bağlı yeni bir kulüp açma talebinde bulunabilir. Kulüp başvurusu sistem yöneticisine iletilir. Başvuru onaylanırsa öğrenci, ilgili kulübün yöneticisi olur ve kulüp ile etkinlikler üzerinde CRUD işlemleri gerçekleştirebilir.

---

##  Proje Amacı ve Öne Çıkan Özellikler

UniFlow, kampüs içi öğrenci etkileşimini ve kulüp organizasyonlarını dijitalleştirerek tek bir platformda toplamayı amaçlar.

### 🌟 Öne Çıkan Özellikler
- **Kullanıcı ve Rol Yönetimi:** Öğrenci, Kulüp Yöneticisi ve Sistem Yöneticisi (Admin) rolleri ile yetkilendirilmiş erişim.
- **Üniversite Seçimi:** Kullanıcıların kendi üniversitelerine özel kulüpleri ve etkinlikleri dinamik olarak görüntülemesi.
- **Kulüp ve Etkinlik Keşfi:** Aktif kulüpleri inceleme, kulüplere üyelik başvurusu yapma, yaklaşan ve geçmiş etkinlikleri takip etme.
- **Etkinlik ve Kulüp Yönetimi:** Kulüp yöneticileri için yeni etkinlik oluşturma, düzenleme ve başvuru onay süreçleri.
- **Admin Paneli:** Sistem genelinde üniversite ve kulüp onaylama/düzenleme işlemleri.
- **Profil Yönetimi:** Profil bilgilerini güncelleme, e-posta/şifre değiştirme ve katılım sağlanan başvuruları listeleme.

---

## 🖼️ Uygulama İçi Görseller
<img width="225" height="462" alt="Ekran görüntüsü 2026-10-07 181956" src="https://github.com/user-attachments/assets/789f815a-b8cc-40ba-baad-e1870ba3e8e9" />
<img width="225" height="462" alt="Ekran görüntüsü 2026-10-07 175621" src="https://github.com/user-attachments/assets/170446fd-4af6-400f-9997-df95a1a331a3" />
<img width="225" height="462" alt="Ekran görüntüsü 2026-10-07 181157" src="https://github.com/user-attachments/assets/36366fd7-7a53-482b-8c83-a10d81bfce19" />
<img width="225" height="462" alt="Ekran görüntüsü 2026-10-07 174936" src="https://github.com/user-attachments/assets/394f47ad-6174-4bfe-a869-4d35de751750" />
<img width="225" height="462" alt="Ekran görüntüsü 2026-10-07 180358" src="https://github.com/user-attachments/assets/4ca2ab74-efea-45bc-82a0-866c505c6f95" />
<img width="225" height="462" alt="Ekran görüntüsü 2026-10-07 183618" src="https://github.com/user-attachments/assets/5499ee11-d64a-437d-bd02-fd1c19571aab" />
<img width="225" height="462" alt="Ekran görüntüsü 2026-10-07 180845" src="https://github.com/user-attachments/assets/01985554-a323-43c1-8da8-a78b34b18d95" />
<img width="225" height="462" alt="Ekran görüntüsü 2026-10-07 180804" src="https://github.com/user-attachments/assets/05505904-7afd-4dfd-81dc-98e0c694aca1" />




---

## 🛠️ Kullanılan Teknolojiler ve Mimari

Proje, güncel Android geliştirme standartlarına ve **Clean Architecture + MVVM** mimarisine uygun olarak geliştirilmiştir.

- **Dil:** [Kotlin](https://kotlinlang.org/)
- **Kullanıcı Arayüzü (UI):** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Mimari Pattern:** Clean Architecture (Domain, Data, Presentation katmanları) + MVVM
- **Bağımlılık Enjeksiyonu (DI):** [Dagger Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Backend & Veritabanı:**
  - **Firebase Authentication** (Kullanıcı doğrulaması)
  - **Cloud Firestore** (NoSQL veritabanı)
- **Görsel Yükleme:** [Coil Compose](https://coil-kt.github.io/coil/compose/)
- **Asenkron ve Akış Yönetimi:** Kotlin Coroutines & StateFlow
- **Navigasyon:** Jetpack Navigation Compose

---

## 📂 Proje Dosya Yapısı

```text
uni-flow/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/uniflow/app/
│   │   │   │   ├── core/              # Ortak yardımcı sınıflar, util ve Sabitler (Utils)
│   │   │   │   ├── data/              # DTO, Mapper, Repository Implementasyonları & Firebase Data Sources
│   │   │   │   ├── di/                # Hilt Dependency Injection Modülleri
│   │   │   │   ├── domain/            # Domain Modelleri, Repository Arayüzleri & Use Case'ler
│   │   │   │   ├── presentation/      # Jetpack Compose Ekranları (UI) & ViewModel'ler
│   │   │   │   │   ├── admin_*/       # Admin yönetim ekranları
│   │   │   │   │   ├── club_*/        # Kulüp detay, başvuru ve yönetim ekranları
│   │   │   │   │   ├── event_*/       # Etkinlik listesi, detay ve oluşturma ekranları
│   │   │   │   │   ├── login/         # Giriş ekranı
│   │   │   │   │   ├── register/      # Kayıt ekranı
│   │   │   │   │   └── profile/       # Profil ve ayarlar ekranları
│   │   │   │   └── ui/theme/          # Renk, Tipografi ve Tema ayarları
│   │   │   ├── res/                   # İkonlar, XML Kaynakları ve Görseller
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts               # Uygulama düzeyinde Gradle bağımlılıkları
├── build.gradle.kts                   # Kök proje Gradle yapılandırması
├── settings.gradle.kts
└── README.md
```
## 💻 Projeyi Yerel Bilgisayarda Test Etme (Kurulum)

Projeyi kendi bilgisayarınızda çalıştırıp test etmek için aşağıdaki adımları izleyebilirsiniz:

### 📋 Ön Gereksinimler
- **Android Studio** (Koala / Ladybug veya daha yeni bir sürüm tavsiye edilir)
- **JDK 11** veya üzeri
- **Android SDK** (Minimum API 24 - Android 7.0)

### 🚀 Çalıştırma Adımları

1. **Projeyi Klonsalayın:**
   ```bash
   git clone https://github.com/furkanacikalin/uni-flow.git
   cd uni-flow
   ```
2. **Firebase Yapılandırması (google-services.json):**
   ```bash
   Proje Firebase servislerini kullandığı için kendi Firebase projenizden aldığınız google-services.json dosyasını app/ dizinine ekleyin (uni-flow/app/google-services.json).
   ```
3. **Projedeki Bağımlılıkları Senkronize Edin:**
   ```bash
   Android Studio'yu açın ve projeyi yükleyin.
   File -> Sync Project with Gradle Files butonuna tıklayarak bağımlılıkların indirilmesini bekleyin.
   ```
4. **Uygulamayı Çalıştırın:**
   ```bash
   Bir Android Emülatörü (AVD) başlatın veya USB Hata Ayıklama modu açık bir fiziksel Android cihaz bağlayın.
   Üst menüdeki Run ('app') veya Shift + F10 kısayoluna basarak uygulamayı derleyip çalıştırın.
   ```
