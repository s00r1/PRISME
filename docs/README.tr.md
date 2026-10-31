# PRISME 5.1

[Français](README.fr.md) · [English](README.en.md) · [Deutsch](README.de.md) · [中文](README.zh.md) · [Türkçe](README.tr.md) · [Русский](README.ru.md) · [العربية](README.ar.md)

**Tam Android çalıştıran saatler için özelleştirilebilir ana ekran.** Çevrimdışı çalışır; hesap, reklam ve telemetri içermez.

## Kurulum

**[Kuruluma hazır PRISME.apk dosyasını indir](https://github.com/s00r1/PRISME/releases/latest/download/PRISME.apk)** · [Sürümler ve sağlama toplamları](https://github.com/s00r1/PRISME/releases)

Saatte **tam Android 7.1 veya üzeri, en az API 25** ve normal Android APK kurma desteği gerekir. APK'yi saat tarayıcısından indirin, Android yükleyicisiyle açın ve istenirse bu kaynağa izin verin. PRISME'yi başlatın; aygıt yazılımı izin veriyorsa varsayılan ana ekran seçin veya normal uygulama olarak açın. Yüzen ses ve genel yönlendirme için diğer uygulamaların üzerinde gösterme izni verin. Dil: **Ayarlar → Stil → Dil**. Resmî güncellemeleri mevcut sürümün üzerine kurun.

Bilgisayardan: `adb install -r PRISME.apk`. Kurulum için root, aygıt yazılımı veya Google hizmetlerini değiştirmek gerekmez.

## Özellikler

- **22 farklı düzen**: takvim, masaüstü, galeri, terminal, oyun salonu, takımyıldız, metin dizini, mozaik, baloncuklar, göstergeler, not defteri ve sade ekran.
- Yerel kadran, Stiller, Bugün ve ayarlara sürekli erişim. Bugün'e uzun basınca beş özelleştirilebilir kısayol açılır.
- **iOS veya Android tarzı kontrol merkezi**: düğmeler, sıra, renkler, saydamlık, boyut, konum ve hareketler ayarlanabilir. Bazı komutlar Android ayarlarını açar.
- **Dikey / Yatay**: dokunarak döndürme; uzun basışla ters yönler ve Sistem modu. Bildirimden diğer uygulamaların içindeyken de döndürülebilir. DM100 üzerinde test edildi; diğer sistemler kısıtlayabilir.
- **Yüzen ses düğmesi**: farklı stiller/şekiller, boyut, opaklık, konum, açılır tuşlar ve isteğe bağlı boşta solma.
- Uygulama/ayar araması, favoriler, son uygulamalar, çalışma alanları ve özel adlar.
- Bugün, otomatik not kaydı, görevler, Android alarmlı süreölçer, tur kayıtlı kronometre ve masa saati.
- Genel ölçek, kenar boşlukları, metin/simge boyutu, renkler, profiller ve yerel yedekler.
- **Fransızca, İngilizce, Almanca, Basitleştirilmiş Çince, Türkçe, Rusça ve Arapça**; sağdan sola görünüm desteği.

## Saatler

| Model veya aile | Durum |
|---|---|
| Domiwear / Domino DM100, Android 7.1.1 | **Cihazda test edildi**, yatay referans 640 × 480 |
| Diğer DM100 varyantları | Ayrı ayrı doğrulanmalı; benzer ad aynı yazılım anlamına gelmez |
| LEMFO LEM T / LEM16 | **Test edilmedi**, Android koşulları sağlanırsa topluluk testi adayı |
| KOSPET PRIME / PRIME SE / PRIME Pro / Optimus 2 | **Test edilmedi**; yuvarlak ekran ve genel döndürme garantisi yok |
| Diğer tam Android, API 25+ saatler | Kurulum ve özellikler değerlendirilmelidir |

Bu adlar **aday listesidir, sertifikalı destek listesi değildir**. Gerçek saatin işletim sistemi, API düzeyi ve APK kurulumunu kontrol edin. Proje Wear OS kadranı değildir. Pixel Watch ve Wear OS Galaxy Watch doğrulanmamıştır. watchOS, Tizen ve Android APK yükleyemeyen özel sistemler hedef dışıdır. [Koşullar ve kaynaklar](COMPATIBILITY.md).

## Görseller, gizlilik ve sınırlar

[Menüler ve seçenekler](SCREENSHOTS.md) · [22 stilin tümü](STYLES.md)

Görseller ayrı bir demo derlemesinin gerçek arayüzlerinden, kurmaca veriler ve genel simgelerle alınmıştır. Kullanıcı hesabı veya kişisel uygulama listesi göstermez. Internet izni yoktur. Notlar, görevler, favoriler ve PRISME'den başlatmalar yerel kalır; kullanıcı dışa aktarımları bunları içerebilir. Özel anahtarlar, kullanıcı yedekleri, cihaz kimlikleri ve kişisel günlükler yayımlanmaz.

Üretici pil yönetimi süreölçeri ve yüzen hizmetleri durdurabilir. Müzik denetimi etkin oynatıcı gerektirir. Çeviriler geliştirilebilir. DM100'de bildirilen yan siyah alanlar için **doğrulanmış onarım yoktur**. PRISME paneli veya sürücüyü değiştirmez. Diğer ekranların görünümü garanti edilmez.

Java kaynakları, kaynak dosyaları, dil katalogları, testler ve isteğe bağlı Imou yardımcısı dahildir. [Derleme ve test](BUILD.md) · [İzinler](PRIVACY.md) · [Uyumluluk bildir](https://github.com/s00r1/PRISME/issues/new/choose). Kod inceleme için yayımlanmıştır; henüz açık kaynak lisansı seçilmemiştir. APK ücretsiz indirilebilir, kurulabilir ve kullanılabilir. Marka adları yalnızca görsel esinlenmeyi anlatır; markalarla bağlantı yoktur.

![PRISME — tr](../assets/screenshots/language-tr.png)
