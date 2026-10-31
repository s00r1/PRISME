# PRISME 5.1

[Français](README.fr.md) · [English](README.en.md) · [Deutsch](README.de.md) · [中文](README.zh.md) · [Türkçe](README.tr.md) · [Русский](README.ru.md) · [العربية](README.ar.md)

**Un bureau personnalisable pour les montres sous Android complet**, avec 22 compositions, un centre de contrôle et des outils quotidiens. Hors ligne, sans compte, publicité ni télémétrie.

## Télécharger

**[Télécharger PRISME.apk — prêt à installer](https://github.com/s00r1/PRISME/releases/latest/download/PRISME.apk)** · [Versions et sommes de contrôle](https://github.com/s00r1/PRISME/releases)

1. La montre doit utiliser **Android complet 7.1 ou plus, API 25 minimum**, et accepter les APK Android ordinaires.
2. Téléchargez `PRISME.apk` dans son navigateur puis ouvrez-le avec l’installateur Android. Autorisez cette source d’installation si nécessaire.
3. Lancez PRISME ; choisissez-le comme accueil si le firmware le permet, ou ouvrez-le comme une application normale.
4. La superposition est nécessaire pour la pastille de volume et l’orientation globale. Certains boutons peuvent demander une autorisation ou ouvrir un réglage Android.
5. Choisissez la langue dans **Réglages → Style → Langue / Language**. Installez les mises à jour officielles par-dessus la version existante.

Depuis un ordinateur : `adb install -r PRISME.apk`. Installer PRISME ne nécessite ni root, ni changement de firmware, ni modification des services Google.

## Ce que contient PRISME

- **22 styles distincts** : calendrier, bureau, galerie, console, arcade, constellation, répertoire typographique, mosaïque, bulles, instruments, carnet et interface minimaliste.
- Navigation permanente : cadran d’origine, Styles, Aujourd’hui, Réglages. Un appui long sur Aujourd’hui ouvre cinq raccourcis personnalisables.
- **Centre de contrôle iOS ou Android** : boutons, ordre, couleurs, transparence, dimensions, position et gestes réglables.
- **Portrait / Paysage** : appui court pour tourner, appui long pour les orientations inversées et le retour Système. La notification propose aussi des commandes depuis une autre application. Fonction testée sur DM100 ; les autres firmwares peuvent la limiter.
- **Pastille de volume intégrée** : styles, formes, taille, opacité, position, déploiement et atténuation au repos.
- Recherche des applications et réglages, favoris, récents, espaces et noms personnalisés.
- Aujourd’hui, carnet à sauvegarde automatique, tâches, minuteur avec alarme Android, chronomètre avec tours et horloge de table.
- Zoom global, marges, textes, icônes, couleurs, profils et sauvegardes de sécurité locales.
- **Sept langues** : français, anglais, allemand, chinois simplifié, turc, russe, arabe, avec présentation de droite à gauche.

## Montres

| Montre ou famille | Statut |
|---|---|
| Domiwear / Domino DM100, Android 7.1.1 | **Testée sur appareil**, référence 640 × 480 en paysage |
| Autres variantes DM100 | À vérifier : un nom similaire ne garantit pas le même firmware |
| LEMFO LEM T / LEM16 | **Non testées**, candidates à un essai communautaire si les conditions Android sont remplies |
| KOSPET PRIME / PRIME SE / PRIME Pro / Optimus 2 | **Non testées**, mêmes conditions ; écran rond et rotation globale non garantis |
| Autres montres sous Android complet, API 25+ | Installation et fonctions à évaluer selon le firmware |

Les modèles non testés sont des **candidats, pas une liste certifiée**. Vérifiez le véritable système, son niveau API et l’installation d’APK sur la montre. Ce projet n’est pas un cadran Wear OS : Pixel Watch et Galaxy Watch Wear OS ne sont pas validées. watchOS, Tizen et les systèmes propriétaires sans APK Android sont hors cible. [Critères et sources](COMPATIBILITY.md).

## Voir PRISME

[Menus et options](SCREENSHOTS.md) · [Les 22 styles](STYLES.md)

Les images utilisent les vraies interfaces d’une version de démonstration séparée, avec données et icônes fictives. Elles ne montrent aucun compte ni catalogue personnel d’applications.

## Confidentialité et limites

L’application ne déclare pas la permission Internet. Notes, tâches, favoris et historique des lancements depuis PRISME restent dans son stockage local ; les exports choisis par l’utilisateur peuvent les contenir. Le dépôt n’inclut aucune clé privée, sauvegarde utilisateur, capture de compte, identifiant de montre ou journal personnel.

La gestion de batterie constructeur peut arrêter le minuteur et les services flottants. Les commandes musicales nécessitent un lecteur actif. Les traductions secondaires peuvent être affinées. Les bandes physiques latérales signalées sur une DM100 restent **sans réparation confirmée** ; PRISME ne prétend pas modifier la dalle ou réparer son pilote. L’affichage sur d’autres écrans n’est pas garanti.

## Sources et contribution

Java, ressources, catalogues de langues, tests et compagnon Imou optionnel sont fournis. [Compiler et tester](BUILD.md) · [Permissions et données](PRIVACY.md) · [Signaler une compatibilité](https://github.com/s00r1/PRISME/issues/new/choose).

Code original sous licence MIT : utilisation, modification et redistribution autorisées avec conservation de la notice. Les marques citées dans les styles décrivent une inspiration visuelle, sans affiliation.

![PRISME — fr](../assets/screenshots/language-fr.png)
