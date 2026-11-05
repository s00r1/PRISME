# PRISME 6

**Un bureau personnalisable pour la DM100 et les grandes montres Android rectangulaires.** Composez votre accueil sur la montre ou sur PC, retrouvez vos outils quotidiens et gardez vos données en local.

[Site officiel](https://s00r1.github.io/PRISME/) · [Télécharger la V6](https://github.com/s00r1/PRISME/releases/tag/v6.0.1) · [Éditeur PC en ligne](https://s00r1.github.io/PRISME/home-studio.html) · [Guide détaillé](V6-GUIDE.md)

[Français](README.fr.md) · [English](README.en.md) · [Deutsch](README.de.md) · [中文](README.zh.md) · [Türkçe](README.tr.md) · [Русский](README.ru.md) · [العربية](README.ar.md)

> **Version stable publiée : 6.0.1 · versionCode 25 · Android complet 7.1 / API 25 minimum.** La DM100 sous Android 7.1.1, avec surface paysage 640 × 480, est la référence testée. Cette distribution cible les montres rectangulaires ; les montres rondes/carrées, Wear OS, watchOS et Tizen ne sont pas des cibles validées. Aucun root ni remplacement du firmware n’est nécessaire.

## Télécharger : l’application et son éditeur PC

| Fichier officiel | Utilisation |
| --- | --- |
| [**PRISME-6.0.1.apk**](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-6.0.1.apk) | Application Android signée ; mise à jour de PRISME sur la montre. |
| [**PRISME-PC-Studio-6.0.1.zip**](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-PC-Studio-6.0.1.zip) | Éditeurs PC complets, utilisables hors ligne en ouvrant un fichier HTML. |
| [**PRISME-6.0.1-sources.zip**](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-6.0.1-sources.zip) | Sources validées de l’application, des éditeurs, tests et documentation, dans l’archive jointe à la Release. |
| [**SHA256SUMS.txt**](https://github.com/s00r1/PRISME/releases/download/v6.0.1/SHA256SUMS.txt) | Sommes de contrôle des trois fichiers distribués. |

Ces fichiers sont disponibles dans la Release stable `v6.0.1`. Les [sources validées de la V6](https://github.com/s00r1/PRISME/releases/download/v6.0.1/PRISME-6.0.1-sources.zip) sont fournies dans l’archive jointe à cette Release. L’éditeur existe aussi [directement dans le navigateur](https://s00r1.github.io/PRISME/home-studio.html). Le ZIP PC est un outil local : aucune installation, inscription ou connexion permanente n’est nécessaire. Les artefacts non signés des workflows GitHub sont destinés au développement ; pour installer PRISME, utiliser l’APK signé.

## La V6 sur la montre

<table>
<tr><td><img src="../assets/device/v6/home-graphite.png" width="410" alt="Accueil PRISME 6 Graphite sur la DM100"><br><b>Graphite · accueil modulaire</b></td><td><img src="../assets/device/v6/home-paper.png" width="410" alt="Palette Papier sur la DM100"><br><b>Papier · même disposition, autre ambiance</b></td></tr>
<tr><td><img src="../assets/device/v6/page-media.png" width="410" alt="Page multimédia PRISME 6"><br><b>Page multimédia</b></td><td><img src="../assets/device/v6/editor.png" width="410" alt="Éditeur tactile de l’accueil"><br><b>Édition sur la montre</b></td></tr>
</table>

Captures réelles de la DM100 pendant la validation de l’interface V6. Elles illustrent les fonctions livrées ; les captures historiques des styles classiques restent des démonstrations avec catalogue fictif. Aucun carnet personnel ou catalogue réel d’applications n’est publié.

| Météo et prévisions | Widgets des 22 styles |
| --- | --- |
| ![Météo PRISME 6 sur la DM100](../assets/device/v6/weather.png) | ![Catalogue et réglages des widgets](../assets/device/v6/widgets.png) |

| Centre de contrôle iOS | Centre de contrôle Android |
| --- | --- |
| ![Modules iOS](../assets/device/v6/control-center-ios.png) | ![Réglages rapides Android](../assets/device/v6/control-center-android.png) |

## Les 22 styles restent votre bureau

Le style sélectionné s’affiche par défaut. **Réglages → 22 styles** ouvre un sélecteur compact avec recherche et accès aux aperçus. Choisir un style ferme les cartes et montre ce bureau immédiatement. Dans le panneau de cartes, **Bureau** ou Retour Android revient au style sélectionné.

## Ce qui change dans PRISME 6

### Treize widgets, dans les 22 styles

La zone de widgets accompagne chaque bureau, avec ses couleurs et ses commandes préservées. **Météo, Horloge, Musique, Batterie, Tâches, Carnet, Minuteurs, Calendrier, Horloge mondiale, Système, Concentration, Raccourcis et Chronomètre** : jusqu’à seize instances, à ajouter, retirer, désactiver ou réordonner depuis **Réglages → Widgets**. Météo, Musique et Tâches sont activés au départ ; les autres types restent disponibles. Un appui ouvre l’outil, un appui long personnalise le widget. Retirer un widget conserve les données de l’outil.

**Six apparences** (Signature du bureau, Verre, Carte pleine, Contour, Minimal, Néon), trois densités, quatre présentations, accents, opacité du fond, arrondis, taille du texte, titre et détails : chaque instance est indépendante. Choisir les styles dans lesquels elle apparaît, et une zone à gauche/droite de 144 à 208 px en paysage, ou une bande supérieure en portrait. La zone défile et peut être masquée. Les horloges proposent formats digital/analogique/grands chiffres/date-heure, secondes, 12/24 h et fuseaux.

### Une météo intégrée, avec votre accord

La météo propose conditions actuelles, tendance, cinq jours ou prochaines heures, avec °C/°F. Toucher le widget ouvre le détail : ressenti, humidité, vent, douze prochaines heures et cinq jours. L’actualisation manuelle et l’intervalle de 15 à 180 minutes en premier plan complètent le cache horodaté et les états d’erreur/ancienneté.

Sur la montre, **Réglages → Widgets → Ville et météo connectée** permet d’activer explicitement Open-Meteo et de choisir une ville. Aucun GPS. Le fournisseur reçoit l’IP et le texte de recherche ou les coordonnées de cette ville. La ville, les coordonnées, le consentement et le cache restent locaux et ne sont jamais importés. Les unités et la fréquence sont portables. L’API gratuite d’Open-Meteo est destinée aux usages non commerciaux ; l’attribution et les conditions sont détaillées dans la [confidentialité](PRIVACY.md).

### Deux centres de contrôle affirmés

**iOS** : modules séparés, commandes circulaires et curseurs verticaux de luminosité/volume. **Android** : grandes tuiles de réglages rapides, états textuels et curseurs horizontaux. Les raccourcis, l’ordre, les couleurs, dimensions, gestes, rotation et fonctions musicales restent personnalisables. Les restrictions du système peuvent imposer l’ouverture des réglages Android pour certaines commandes.

### Un accueil en pages, à votre façon

Le panneau de cartes s’ouvre explicitement dans **Outils → Bureau et cartes**. Ses boutons **Bureau** et **22 styles**, ainsi que Retour Android, permettent de retrouver le bureau choisi. Trois pages sont proposées au départ : **Principal, Travail et Multimédia**. Vous pouvez les renommer, en ajouter ou en supprimer, jusqu’à cinq pages. Les flèches du haut changent de page ; les commandes Applications, Outils, Modifier et Réglages restent accessibles en bas.

Sept types de blocs sont disponibles : **Horloge, Tâches, Minuteur, Musique, Carnet, Favoris et Montre**. Chaque page peut contenir jusqu’à huit blocs sur une grille de quatre colonnes et quatre lignes. Les blocs ont des positions et dimensions réglables ; les chevauchements et sorties d’écran sont refusés.

Un appui court ouvre la fonction. Un appui long permet de régler les coordonnées et la taille. Pour déplacer au doigt : **Modifier → Déplacer les blocs au doigt**, glisser puis **Terminer**. Une étape d’annulation et de rétablissement permet de revenir sur la dernière modification. Les pages partagent les mêmes notes et tâches : elles ne créent pas des copies de vos données.

### Six palettes, et les 22 bureaux classiques conservés

Le panneau de cartes, facultatif à côté de votre bureau, propose **Graphite, Verre, Papier, Instrument, Néon et Minimal**, ainsi qu’un mode grands textes. Le style du bureau classique reste indépendant : calendrier, terminal, arcade, constellation, papier, mosaïque, instruments, Nova, Silence et les autres compositions restent disponibles.

Les 22 bureaux restent accessibles dans **Réglages → 22 styles**. Les cartes s’ouvrent depuis **Outils → Bureau et cartes**, et leur bouton **Bureau** revient au style sélectionné. [Voir la galerie des styles](STYLES.md).

### Des outils réunis dans une navigation commune

| Fonction | Dans la V6 | À savoir |
| --- | --- | --- |
| Minuteurs nommés | Jusqu’à huit minuteurs, pause/reprise, redémarrage et suppression indépendants. | Durée initiale de 1 à 1 440 minutes ; le firmware peut retarder une alarme. |
| Musique | Commandes simples ou titre/artiste et choix de session en mode enrichi. | Le lecteur doit publier une session ; le mode enrichi nécessite un consentement distinct. |
| Calculatrice | Addition, soustraction, multiplication, division et décimales. | Nombres bornés, pas d’exécution d’expressions ou de scripts. |
| Convertisseur | Longueur, masse, température et durée, sans Internet. | Résultats arrondis pour l’affichage. |
| Carnet et tâches | Notes locales à sauvegarde automatique, checklist, recherche et accès depuis les blocs. | Les blocs d’accueil ne dévoilent pas le texte du carnet. |
| Temps et concentration | Chronomètre avec tours, minuteur de concentration, horloge de table. | L’écran toujours allumé reste un choix utilisateur. |
| Centre de contrôle | Présentations iOS/Android, raccourcis, dimensions, couleurs, gestes et rotation. | Certains boutons ouvrent les réglages Android au lieu de basculer directement une fonction. |
| Volume et batterie | Volume flottant ; batterie avec 24 présentations, 12 palettes, taille, position, opacité et diagnostic. | Le pourcentage vient du firmware, sans mesure électrique indépendante. |
| Studio, Sense et dock | Widgets classiques, règles locales de style selon heure/charge, dock flottant facultatif. | Activation et permissions locales ; aucune autorisation importée depuis un thème. |
| Doctor | Diagnostic local : API, écran, permissions, services, mémoire et batterie. | Pas de télémétrie ni de prétendue mesure de fréquence d’images. |

Les superpositions PRISME sont masquées sur l’accueil modulaire pour ne pas couvrir les blocs. Elles reviennent dans les autres applications selon vos réglages. **Outils → Suspendre tous les éléments flottants** permet une suspension générale jusqu’à réactivation.

## Installer ou mettre à jour depuis la V5

1. **Exporter votre configuration actuelle** depuis Réglages → Profils, puis conserver ce fichier.
2. Télécharger `PRISME-6.0.1.apk` depuis la Release officielle et l’ouvrir sur la montre, ou utiliser `adb install -r PRISME-6.0.1.apk` depuis un PC.
3. Installer **par-dessus** la version existante. La signature d’origine est conservée ; ne pas désinstaller PRISME pour une mise à jour normale.
4. Ouvrir PRISME. Le bureau sélectionné reste affiché ; les cartes sont facultatives et s’ouvrent depuis Outils → Bureau et cartes. Les notes, tâches et réglages restent présents.
5. Choisir PRISME comme accueil si le firmware le permet, ou l’utiliser comme une application normale. Les fonctions flottantes demandent séparément la superposition Android.

Les anciens profils JSON sont acceptés ; les exports V6 utilisent le **schéma 7**. Ce numéro désigne le format du fichier, pas une application « PRISME 7 ». Un export V6 peut être refusé par une ancienne version : garder l’export V5 pour un retour arrière. Android peut refuser l’installation d’un numéro de version inférieur ; aucun retour arrière ne désinstalle automatiquement vos données. [Guide de migration](V6-GUIDE.md).

## Composer sur PC, appliquer sur la montre

1. Ouvrir [PC Studio](https://s00r1.github.io/PRISME/home-studio.html), ou extraire le ZIP PC et ouvrir `index.html`.
2. Charger un **export JSON complet** provenant de PRISME. Sans export, l’éditeur offre seulement un aperçu et désactive l’export d’un profil incomplet.
3. Dans **Widgets des 22 styles**, ajouter et personnaliser les widgets ; dans **Cartes facultatives**, composer les pages, dimensions, palettes et grands textes.
4. Exporter `PRISME-6-Studio.json` puis le copier sur la montre.
5. Dans PRISME : **Réglages → Profils → Importer une configuration → Parcourir avec le sélecteur Android**. Vérifier et confirmer.

Le profil est traité localement dans la mémoire du navigateur ; son contenu n’est pas envoyé au site. Les autres sections du profil chargé sont conservées et le mode bureau/cartes actif ne change pas lors de l’import. Les 22 aperçus sont inclus hors ligne. L’archive conserve aussi `classic-studio.html` et ses quatre widgets historiques. Ville, cache et consentement météo se règlent exclusivement sur la montre. [Guide complet de l’éditeur et dépannage](PC-STUDIO.md).

## Permissions, sauvegardes et confidentialité

PRISME n’a pas de compte, publicité ou télémétrie. Notes, tâches, favoris et réglages restent locaux. **La permission Internet sert à la météo facultative**, désactivée jusqu’au consentement explicite dans PRISME et au choix manuel d’une ville ; le reste des données n’est pas envoyé au fournisseur. Les profils JSON ordinaires sont en clair et peuvent contenir des données personnelles : ne pas les publier dans une issue.

- **Météo** : HTTPS vers Open-Meteo sans GPS, après accord local ; désactiver conserve le cache et stoppe les nouvelles requêtes.
- **Superposition** : demandée pour le volume, le centre flottant, le dock ou l’orientation globale. Ces fonctions restent dépendantes des restrictions Android.
- **Musique enrichie** : désactivée par défaut. Elle demande un accès Android sensible aux notifications, mais PRISME utilise les sessions multimédias sans lire ni conserver les messages. Le mode simple ne demande pas cet accès.
- **Réglages système / Ne pas déranger** : accès distincts, utilisés seulement pour les commandes qui les nécessitent.
- **Démarrage** : relance des fonctions flottantes selon les choix locaux et reprogrammation des minuteurs.

Les points de récupération automatiques sont stockés dans l’espace privé de l’application. Un point est créé à l’entrée en V6 puis au démarrage de l’activité après 24 heures ; dix points automatiques sont conservés. Les imports sont entièrement validés avant écriture et protégés par un journal de récupération.

**Outils → Sauvegardes chiffrées** crée un coffre AES-256-GCM exportable, protégé par une phrase de passe d’au moins dix caractères. Il n’existe pas de récupération à distance si elle est perdue. Après trois plantages non traités consécutifs, le mode de secours permet une reprise ou un retour au bureau classique avec les éléments flottants désactivés. [Confidentialité détaillée](PRIVACY.md).

## Compatibilité et limites connues

La DM100 de référence est validée ; les autres appareils et variantes de firmware demandent leurs propres essais. L’installation acceptée par Android ne suffit pas à certifier un modèle. PRISME ne modifie ni les pilotes d’écran ni les services Google.

La livraison d’alarmes, la rotation, les fenêtres flottantes et la lecture en arrière-plan restent sous le contrôle d’Android et du firmware. Les commandes musicales ont été testées avec une session synthétique ; ce n’est pas une certification de Spotify, Deezer ou de tous les lecteurs. Aucun gain chiffré d’autonomie ni certification Play Protect n’est revendiqué. Les sept langues historiques sont conservées ; certains textes détaillés V6 restent en français.

## Vérifications et développement

Les suites automatisées couvrent modèles Java, cartes et widgets PC, galerie et parcours réels dans Edge. Les essais sur DM100 incluent migration, validation, récupération, chiffrement, alarmes, musique, navigation et affichage. Le rapport distingue les assertions de modèles, les essais tactiles et les inspections visuelles, avec les résultats et limites de cette livraison. [Rapport de validation](V6-VALIDATION.md).

```powershell
python scripts/fetch-test-dependency.py
./tests/run-all.ps1
node tests/home-model.cjs
node tests/dashboard-widget-model.cjs
node tests/website.cjs
./build.ps1 -Unsigned
python scripts/package-pc-editor.py
```

JDK 17+, SDK Android, PowerShell 7+, Python 3 et Node sont documentés dans [BUILD.md](BUILD.md). L’APK non signé doit être signé avant installation. Les clés privées et les données de montre ne font pas partie du dépôt. [Tests sur appareil](../tests/device/README.md).

## Documentation et contribution

[Guide V6](V6-GUIDE.md) · [Éditeur PC](PC-STUDIO.md) · [Compatibilité](COMPATIBILITY.md) · [Styles](STYLES.md) · [Captures](SCREENSHOTS.md) · [Notes de version](RELEASE-6.0.1.md) · [Fonctions futures](V6-DEVELOPMENT.md)

Pour signaler un problème, indiquer le modèle, l’API Android, l’orientation, la version PRISME et les étapes de reproduction. Éviter les comptes, identifiants, exports personnels et journaux bruts. [Ouvrir une issue](https://github.com/s00r1/PRISME/issues/new/choose).

Code sous [licence MIT](../LICENSE). Les noms de marques des styles sont des inspirations visuelles sans affiliation ni compatibilité matérielle implicite. Le compagnon Imou Paysage reste facultatif et possède sa propre signature ; il n’est pas nécessaire à PRISME.
