# PRISME 6

DM100 ve tam Android çalıştıran büyük dikdörtgen saatler için kişiselleştirilebilir masaüstü. Seçtiğiniz 22 klasik stilden biri ana masaüstünüz olarak kalır; yeni kartlar isteğe bağlıdır.

[Français](README.fr.md) · [English](README.en.md) · [Deutsch](README.de.md) · [中文](README.zh.md) · [Türkçe](README.tr.md) · [Русский](README.ru.md) · [العربية](README.ar.md)

> Güncel sürüm: **6.0.0 · versionCode 24 · en az tam Android 7.1 / API 25**. Referans: Android 7.1.1 kullanan DM100, yatay 640 × 480. Yuvarlak/kare saatler, Wear OS, watchOS ve Tizen doğrulanmış hedefler değildir. Root veya ürün yazılımını değiştirmek gerekmez.

## İndir

| İndir | PRISME 6 |
| --- | --- |
| [PRISME-6.0.0.apk](https://github.com/s00r1/PRISME/releases/download/v6.0.0/PRISME-6.0.0.apk) | Saat için imzalı Android uygulaması; mevcut PRISME üzerine kurulur. |
| [PRISME-PC-Studio-6.0.0.zip](https://github.com/s00r1/PRISME/releases/download/v6.0.0/PRISME-PC-Studio-6.0.0.zip) | Eksiksiz çevrimdışı PC düzenleyicileri; ZIP dosyasını çıkarıp index.html açın. |
| [PRISME-6.0.0-sources.zip](https://github.com/s00r1/PRISME/releases/download/v6.0.0/PRISME-6.0.0-sources.zip) | Uygulama, düzenleyiciler, testler ve belgelerin kaynakları. |
| [SHA256SUMS.txt](https://github.com/s00r1/PRISME/releases/download/v6.0.0/SHA256SUMS.txt) | Dağıtılan dosyaların sağlama toplamları. |

[PRISME · Web](https://s00r1.github.io/PRISME/) · [PC Studio · Web](https://s00r1.github.io/PRISME/home-studio.html) · [GitHub · v6.0.0](https://github.com/s00r1/PRISME/releases/tag/v6.0.0)

## Gerçek V6 ekranları

| PRISME 6 | PRISME 6 |
| --- | --- |
| ![DM100](../assets/device/v6/home-graphite.png) | ![DM100](../assets/device/v6/home-paper.png) |
| ![DM100](../assets/device/v6/page-media.png) | ![DM100](../assets/device/v6/editor.png) |

DM100 üzerindeki V6 arayüzünün gerçek görüntüleri; arayüz Fransızcadır. Klasik galeri kişisel uygulama listesi yerine örnek veriler kullanır.

## Masaüstü, kartlar ve görünüm

**22 klasik masaüstü korunur ve seçilen stil varsayılan olarak görünür.** Ayarlardaki “22 styles” düğmesi tüm stillere doğrudan erişim sağlar. İsteğe bağlı kartları Araçlar içindeki masaüstü/kart seçimiyle açabilirsiniz. Kartlardaki “Bureau” düğmesi ve Android Geri tuşu seçili masaüstüne döndürür. Bir stil seçmek kartları kapatıp o stili hemen gösterir.

Kartlar için üç hazır sayfa vardır; en fazla **beş sayfa, sayfa başına sekiz kart ve 4 × 4 ızgara** kullanılabilir. Saat, görevler, zamanlayıcı, müzik, notlar, favoriler ve saat durumu kartları taşınıp boyutlandırılabilir. Çakışan veya ekran dışındaki konumlar reddedilir. Uzun basma sayısal ayarları açar; düzenleme menüsünde sürükleme modu ve tek adımlı geri alma/yineleme bulunur.

Grafit, Cam, Kâğıt, Gösterge, Neon ve Minimal paletleri ile büyük yazı seçeneği sunulur. Kartların paleti klasik stilinizden bağımsızdır. Sayfalar aynı notları ve görevleri paylaşır; ana kartlar not metnini göstermez.

## 22 masaüstünde widget’lar

İsteğe bağlı ızgara sayfalarına ek olarak **22 klasik stilin** ortak bir widget alanı vardır. Katalogda **13 tür** bulunur: hava durumu, saat, müzik, pil, görevler, not defteri, zamanlayıcılar, takvim, dünya saati, sistem, odaklanma, kısayollar ve kronometre. Varsayılan olarak hava durumu, müzik ve görevler görünür; ekleme, kaldırma, gizleme ve sıralama desteklenir. En fazla 16 öğe saklanır. Widget kaldırmak veya gizlemek seçili masaüstü stilini değiştirmez.

**Ayarlar → Widgets** üzerinden veya widget’a uzun basarak görünüm, yoğunluk, vurgu rengi, saydamlık, köşeler, yazı boyutu, özel başlık ve hangi stillerde gösterileceği ayrı ayrı ayarlanır. Altı görünüm vardır: masaüstü imzası, cam, dolu kart, çerçeve, minimal ve neon. Saatin analog görünümü, dünya saatinin seçilebilir saat dilimi vardır. Alan yatay ekranda sağda veya solda, dikeyde üst şerittedir; masaüstü kendi alanını korur. Bütün alan gizlenebilir, son değişiklik geri alınabilir ve yinelenebilir.

### İsteğe bağlı Open-Meteo hava durumu

Bağlantılı hava durumu **varsayılan olarak kapalıdır**. Hava durumu ayarlarında açıp şehri elle arayarak seçin; GPS ve konum izni gerekmez. Open-Meteo IP adresini, şehir aramasını veya seçilen şehrin koordinatlarını alır. Ayrıntı penceresinde sıcaklık, hissedilen sıcaklık, nem, rüzgâr, gelecek saatler ve beş günlük tahmin bulunur. Celsius/Fahrenheit ve ön planda yenileme aralığı ayarlanabilir. Yerel önbelleğin tarihi ve eskime durumu görünür; bağlantı hatası uydurma ölçüm üretmez. Bağlantıyı kapatmak istekleri durdurur, önbelleği korur. [Veri sağlayıcı: Open-Meteo](https://open-meteo.com/).

### Gerçekten farklı iki kontrol merkezi

**iOS:** ilk dört seçili komut dairesel düğmeli bir modüldedir; kompakt müzik kartı ve dikey parlaklık/ses çubukları bulunur. **Android:** büyük saat, iki sütunda geniş Material düğmeleri, yatay çubuklar ve tam genişlikte müzik kartı sunar. Dar alanda düzen uyarlanır. Seçili komutlar, sıralama ve uzun basma korunur; müzik ve çubuklar ayrı gizlenebilir. Durumlar Android’den gelir; korumalı işlevler ilgili sistem ayarını açar. Kapat, Geri ve seçilen hareketler kullanılmaya devam eder.

## Günlük araçlar

| PRISME | Günlük araçlar |
| --- | --- |
| Adlandırılmış zamanlayıcılar | Sekiz bağımsız sayaç; duraklatma, sürdürme, yeniden başlatma ve silme. Başlangıç süresi 1–1440 dakika. |
| Müzik | Basit medya tuşları veya isteğe bağlı başlık/sanatçı ve oynatıcı oturumu seçimi. |
| Hesap ve dönüştürme | Dört ondalık işlem; uzunluk, kütle, sıcaklık ve süre için çevrimdışı dönüşüm. |
| Notlar ve görevler | Yerel otomatik kayıt, kontrol listesi, arama ve kartlardan erişim. |
| Saat araçları | Turlu kronometre, odaklanma sayacı ve masa saati. |
| Kontrol merkezi | iOS/Android düzenleri, sıra, renkler, boyutlar, hareketler ve döndürme. Bazı düğmeler Android ayarlarını açar. |
| Ses ve pil | Yüzen ses; 24 pil görünümü, 12 palet ve ürün yazılımı ölçüm tanısı. |
| Studio / Sense / Dock / Doctor | Klasik bileşenler, yerel saat/şarj kuralları, isteğe bağlı yüzen panel ve yerel tanılama. |

## Kurulum ve V5 güncellemesi

1. Mevcut yapılandırmayı Ayarlar → Profiller bölümünden dışa aktarıp saklayın.
2. İmzalı PRISME-6.0.0.apk dosyasını indirip mevcut PRISME üzerine kurun. Normal güncellemede kaldırma gerekmez.
3. Asıl imza sertifikası korunur; notlar, görevler ve ayarlar yerinde kalır.
4. Ürün yazılımı izin veriyorsa PRISME varsayılan ana ekran olabilir; normal uygulama olarak da açılır.
5. Yüzen özellikler için ayrı izin verin. Klasik masaüstü ve kartlar bildirim erişimi gerektirmez.

```text
adb install -r PRISME-6.0.0.apk
```

Eski JSON profilleri desteklenir. V6 dışa aktarımları **şema 7** kullanır; bu dosya biçimidir, PRISME 7 değildir. Geri dönüş için V5 dosyanızı saklayın. Android düşük versionCode kurulumunu reddedebilir; uygulama sürüm düşürmek için verilerinizi otomatik silmez.

## PC düzenleyicisi: hazırlama ve aktarma

1. Çevrimiçi düzenleyiciyi açın veya PC ZIP dosyasını tamamen çıkarıp index.html dosyasını başlatın. Windows için LANCER-EDITEUR.cmd de vardır.
2. PRISME’den alınmış tam JSON dışa aktarımını yükleyin. Gerçek profil olmadan yalnızca önizleme sunulur.
3. 22 stilin widget’ları ile isteğe bağlı ızgara sayfaları arasında seçim yapın. Widget sırası ve görünümünü veya kart konumu, boyutu, paleti ve büyük yazıyı ayarlayın. Izgara çakışmaları reddeder.
4. PRISME-6-Studio.json dosyasını dışa aktarın ve USB ile saate kopyalayın.
5. Ayarlarda Profiller → Yapılandırma içe aktar → Android dosya seçicisi yolundan dosyayı seçip onaylayın.

Profil tarayıcı belleğinde işlenir ve sunucuya gönderilmez. Diğer profil bölümleri korunur; etkin masaüstü modu değiştirilmez. Kartları Araçlar üzerinden ayrıca açın. PC hava durumu önizlemesi açıkça örnek veri kullanır; şehir ve ağ onayı saatte seçilir. Birim ve yenileme aralığı aktarılabilir. classic-studio.html klasik 22 stili ve önceki dört bileşeni düzenlemeye devam eder. PC arayüzü ve ayrıntılı kılavuz Fransızcadır. Sunucu, hesap veya geliştirme aracı gerekmez.

## İzinler, gizlilik ve kurtarma

Reklam, hesap ve telemetri yoktur. PRISME 6 isteğe bağlı hava durumu için **INTERNET** izni bildirir; hava durumu istekleri varsayılan olarak kapalıdır. Notlar, görevler, favoriler ve diğer profil verileri sağlayıcıya gönderilmez. Ağ onayı, şehir ve koordinatlar saatte yerel kalır ve temadan içe aktarılmaz. Normal JSON dosyaları düz metindir; kişisel not, görev veya favori içerebilir. Bunları herkese açık paylaşmayın.

Gelişmiş müzik başlangıçta kapalıdır. Android hassas bildirim erişimi ister; PRISME bildirim mesajlarını okumadan veya kaydetmeden medya oturumlarını kullanır. Yüzen pencere, sistem ayarları ve Rahatsız Etmeyin erişimleri ayrı amaçlara sahiptir.

Otomatik kurtarma noktaları uygulamanın özel alanındadır ve on otomatik nokta tutulur. Araçlardan en az on karakterli parola ile AES-256-GCM yedeği oluşturulup dışa aktarılabilir. Parola olmadan uzaktan kurtarma yoktur. İçe aktarımlar yazılmadan önce doğrulanır ve bir geri alma günlüğüyle korunur. Etkin zamanlayıcılar, Android izinleri ve masaüstü/kart modu tema dosyasından açılmaz. Üç ardışık işlenmemiş çökmeden sonra kurtarma modu yeniden deneme veya yüzen öğeler kapalı klasik masaüstü sunar.

## Testler ve bilinen sınırlar

Önceki V6 doğrulamasında **67.407 Java doğrulaması**, **1.775 PC modeli doğrulaması**, galeri testleri, gerçek Edge akışları ve **DM100 üzerinde 16 doğrulama**; geçiş, hatalı dosyanın reddi, kurtarma, şifreleme, alarm ve yapay müzik oturumunu kapsar. Dokunmatik testlerde sayfalar, sürükleme, paletler ve manuel kurtarma incelenir. Yeni widget, hava durumu ve kontrol merkezi kontrollerinin güncel durumu [doğrulama raporundadır](V6-VALIDATION.md); önceki sayılar tek başına yeni özelliklerin tamamının test edildiğini göstermez.

Model sayısı fiziksel cihaz sayısı değildir. Diğer saatler, uzun süreli pil ömrü, tüm üçüncü taraf oynatıcılar veya Play Protect sertifikası iddia edilmez. Arka plan, alarmlar ve döndürme Android/ürün yazılımına bağlıdır. Yedi tarihsel dil korunur; bazı ayrıntılı V6 iletileri Fransızcadır.

## Belgeler ve destek

Hata bildirirken model, Android API, PRISME sürümü, yön ve yeniden üretme adımlarını ekleyin. Hesap, seri numarası, kişisel profil veya ham günlük paylaşmayın. MIT lisansı; stil marka adları ortaklık veya donanım onayı anlamına gelmez.

[V6 · FR](V6-GUIDE.md) · [PC Studio · FR](PC-STUDIO.md) · [English guide](README.en.md) · [Compatibility](COMPATIBILITY.md) · [22 styles](STYLES.md) · [Privacy](PRIVACY.md) · [Validation](V6-VALIDATION.md) · [Build](BUILD.md) · [MIT](../LICENSE) · [Issues](https://github.com/s00r1/PRISME/issues/new/choose)
