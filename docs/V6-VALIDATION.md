# Validation PRISME 6.0.0 — 5 novembre 2026

## Livrable et appareil

**6.0.0 · versionCode 24 · Android API 25 minimum**, préparé pour revue dans une pull request. L’APK signé utilise le certificat de l’installation existante et a été installé avec mise à jour, sans désinstallation. La préparation ne publie ni ne fusionne automatiquement une Release.

Appareil physique : DM100, Android 7.1.1/API 25, dalle 480×640, surface paysage 640×480 et portrait 480×640. Les autres modèles ne sont pas certifiés. Le bureau choisi parmi 22 styles reste l’accueil principal ; les cartes sont facultatives.

## Tests automatisés

| Suite | Assertions | Portée |
| --- | ---: | --- |
| Java | 67 663 | Préférences, profils, widgets, météo, géométrie, gestes, temps, utilitaires et chiffrement. |
| Modèle des cartes PC | 1 775 | Grille, taille, limites, collisions et copie indépendante. |
| Modèle des widgets PC | 123 | Types, bornes, filtres, options, identifiants, texte et import. |
| Profils complets PC | 314 | Exports V5/V6, champs requis, types et refus d’un profil amputé. |
| Galerie | Scénarios | Langues concurrentes, erreurs HTTP, données invalides, reprise et zoom. |
| Audit API 25 | 16 contrôles internes | Résolution de l’historique API, instructions DEX et gardes de versions. |

Les 229 cas météo/widgets Java couvrent notamment conversions °C/°F, horodatages, changement d’heure, cache, mesures manquantes, codes météo inconnus, géométrie réservée et interdiction d’importer ville/consentement/endpoint. Les 21 cas de mutation couvrent les identités après réordre/suppression, les changements atomiques et annulation/rétablissement. Les 1 807 cas du cœur V6 vérifient les imports, collisions, journal, minuteries, conversions et chiffrement.

Le nombre de cas de modèle ne représente pas autant de configurations essayées physiquement. Les 43 817 cas de viewport couvrent 378 configurations ; les 20 162 cas de volume couvrent 720 configurations.

## Compilation, signature et API

Compilation complète JDK 25, Java 8, SDK Android 37, DEX min-api 25 ; APK utilisateur signé et vérifié. L’audit du DEX final n’a trouvé aucune référence publique incompatible non protégée ni référence publique non résolue. Les 7 références API 26 sont confinées aux helpers privés vérifiés : chaque appel source possède une garde de version et les appelants DEX sont contrôlés. Cela reste une analyse statique ciblée, pas une preuve générale du comportement du firmware.

Les clés privées, mots de passe, exports utilisateur et listes d’applications réelles ne font pas partie des sources ou des captures publiques.

## Tests dans le processus Android réel

Un paquet d’instrumentation séparé, signé avec la même clé, a exécuté **16 assertions** sur la montre :

- Import d’un véritable export V5 de cette montre vers le schéma 7, avec notes et tâches conservées.
- Refus d’un module malformé, sans changement du carnet.
- Retour à l’état précédent après restauration interrompue simulée dans le journal.
- Chiffrement/déchiffrement AES-GCM et PBKDF2 sur Android 7.1.
- Programmation AlarmManager, pause, reprise sous la seconde et livraison de fin.
- Session MediaSession synthétique et réception d’une commande Lecture.

Les préférences ont été rétablies dans le bloc final, les notifications de test retirées et les alarmes existantes reprogrammées. L’autorisation de notifications temporairement employée a été remise à sa valeur initiale. Aucun message de notification n’a été lu ou journalisé.

Le test météo final a exécuté **14 assertions sur Android 25**, avec de vraies requêtes HTTPS de recherche et prévision pour **Paris, ville de démonstration**. Il vérifie résultats, température plausible, cache persistant, désactivation et conservation hors ligne. Deux actualisations lancées simultanément reçoivent chacune un seul callback, des données identiques, la même date de récupération et le cache du résultat partagé. Les trois magasins météo sont rétablis ensuite et les requêtes restantes invalidées.

Les sources reproductibles du harnais sont dans [tests/device](../tests/device/README.md). Le paquet QA n’est pas un fichier à joindre à la Release utilisateur.

## Inspection des 22 bureaux et parcours tactiles

Chaque style a été ouvert physiquement avec la zone widgets en paysage. Les 22 captures ont été inspectées : identité propre, palette, applications, lisibilité et position des commandes. La navigation Styles et l’accès permanent aux widgets sont présents, contenus dans l’écran et séparés géométriquement. Le catalogue réel installé est utilisé pour les essais privés ; ces captures ne sont pas publiées.

Les nouveaux parcours comprennent : ajout d’une horloge par le catalogue, appui long sur un widget, choix d’une apparence Néon, retrait de l’horloge ajoutée et retour au nombre initial. Le schéma, l’ordre, les filtres par bureau et les options ont en complément une validation de modèle. Le retrait ne supprime pas les données de l’outil.

Sur l’appareil, les deux centres ont été inspectés en paysage et portrait : commutateurs ronds, lecteur compact et niveaux verticaux iOS ; grande horloge, tuiles Material et niveaux horizontaux Android. Un essai supplémentaire iOS en portrait, largeur 65% et zoom 125%, vérifie le défilement et l’accès à Fermer. Les réglages de rotation initiaux ont été rétablis.

Le bureau Prisme Touch a aussi été inspecté en portrait avec sa bande horizontale de widgets et défilement indépendant. Cela ne signifie pas que les 22 styles ont tous une inspection physique exhaustive en portrait.

La revue a corrigé deux chevauchements : la batterie globale et la pastille de volume cèdent leur place à la zone widgets sur l’accueil, sans effacer leurs préférences. Le message vide ne s’affiche plus en double. Les widgets masqués ne reprennent plus leurs rafraîchissements au réveil. Les parcours cartes, retour Bureau, sélection des22 styles, réglages et outils existants restent disponibles.

Les essais précédents des cartes comprennent pages, palettes, annulation, déplacement tactile aimanté, ajout/suppression temporaires et mode de secours manuel. Les commandes externes non autorisées ont été refusées. Aucun plantage PRISME n’a été observé dans le tampon de crash consulté en fin de validation.

## Navigateur, site et paquet PC

Dans Edge réel : widgets 13 types, options, choix des bureaux, apparences, °C/°F, ordre/ajout/retrait, annulation, collisions des cartes, import/export, conservation des autres données et du mode bureau/cartes. Les aperçus météo PC sont explicitement fictifs et aucune donnée n’est envoyée à un service.

Le ZIP extrait a été testé directement en `file://`, sans serveur : les 22 images locales, les deux éditeurs, conversion d’un vrai format complet V5 vers V6, téléchargement du profil modifié et conservation des notes. Les exports incomplets ou trop gros sont refusés ; l’ancien bouton Exporter est désactivé après refus. Ce cas empêchait auparavant de garantir la conservation des données. 314 tests ajoutés en régression.

Le site a été vérifié à 390, 768 et 1 280 pixels, avec galerie multilingue, images locales, nouveaux centres de contrôle et zoom. Aucun débordement horizontal ni erreur JavaScript de page observé. Le ZIP n’a déclenché aucune requête HTTP. [Guide PC](PC-STUDIO.md).

## Captures publiques et limites

Les galeries historiques présentent des catalogues et textes fictifs. Les nouvelles captures météo, gestion widgets et centres de contrôle proviennent de la DM100. Paris est un choix de démonstration, pas une position GPS. Les données de session ont été retirées après les essais ; aucune liste d’applications ni contenu personnel n’est publié.

Pas d’essai prolongé d’autonomie, d’audit de sécurité indépendant, de certification Play Protect ni de comparaison V5/V6 reproductible de fluidité. Un redémarrage matériel n’a pas été provoqué dans cette session ; le calcul après redémarrage est testé en modèle. La livraison d’alarme peut être retardée par le firmware, notamment en veille profonde.

La session musicale synthétique ne certifie pas Spotify, Deezer, YouTube ou les lecteurs OEM. Métadonnées, commandes et lecture arrière-plan dépendent de leurs sessions et d’Android. Les sept langues historiques restent présentes ; tous les nouveaux textes détaillés et parcours RTL n’ont pas une validation matérielle exhaustive.

La météo dépend du réseau et d’Open-Meteo ; un cache ancien ou un réseau désactivé sont signalés. L’API gratuite est destinée aux usages non commerciaux. Aucun engagement d’exactitude ou de disponibilité 100% du fournisseur n’est revendiqué.

[Guide V6](V6-GUIDE.md) · [PC](PC-STUDIO.md) · [Architecture](V6-DEVELOPMENT.md) · [Confidentialité](PRIVACY.md)
