# Validation PRISME 6.0.0-beta1 — 5 novembre 2026

## Statut

Première bêta, versionCode 22, Android API 25 minimum. Elle intègre la branche V6 préexistante avec les corrections V5 et les nouveaux modules. Elle ne remplace pas la version stable v5.4. Les essais concernent une DM100 réelle sous Android 7.1.1, surface paysage 640 × 480 ; les autres appareils ne sont pas certifiés.

## Contrôles automatisés locaux

- **67 407 assertions Java** : préférences historiques, placement sur 378 configurations de modèle, géométrie du volume, gestes, bord à bord, préférences Batterie/Musique et 1 804 assertions du cœur V6.
- Le cœur V6 vérifie notamment les limites de grille, collisions, types invalides, conservation des données lors d’un import refusé, import/export des pages et modules, annulation/rétablissement, journal de récupération, temps monotone/changement d’heure/redémarrage simulé, pause sous la seconde, opérations décimales et conversions.
- Chiffrement : aller-retour, mot de passe incorrect, altération de ciphertext, mot de passe trop court et salt/nonce renouvelés.
- **1 775 assertions JavaScript** sur le modèle de disposition PC, plus les scénarios de galerie (chargement, langues concurrentes, erreurs, reprise et fenêtre de zoom).
- Compilation complète JDK 25, bytecode Java 8, SDK Android 37 et DEX min-api 25. La compilation n’est pas une preuve d’absence de référence à une API plus récente : les fonctions sensibles ont aussi été exercées sur Android 7.1.

## Tests dans le processus Android réel

Un paquet d’instrumentation séparé, signé avec la même clé, a exécuté **16 assertions** sur la montre :

- Import d’un véritable export V5 de cette montre vers le schéma 7 ; notes et tâches conservées.
- Refus d’un module malformé, sans changement du carnet.
- Retour à l’état précédent après une restauration interrompue simulée dans le journal.
- Chiffrement/déchiffrement AES-GCM et PBKDF2 disponibles sur Android 7.1.
- Programmation AlarmManager, pause, reprise avec moins d’une seconde et réception de fin.
- Session MediaSession synthétique avec titre de démonstration ; commande Lecture reçue par son callback.

Les préférences ont été rétablies dans le bloc final du test, les notifications de test retirées et les alarmes existantes reprogrammées. L’autorisation système de notifications temporairement utilisée pour l’essai a été remise exactement à sa valeur initiale. Aucun message de notification n’a été lu ou journalisé.

Les premières mesures ont montré qu’une alarme très courte peut être livrée quelques secondes après l’échéance ; le test attend désormais une livraison bornée et l’interface vérifie aussi les expirations. Cela ne garantit pas la ponctualité en veille profonde ou avec un firmware restrictif.

## Parcours tactiles sur la montre

Trois pages d’accueil, changement de palette puis retour à Graphite, ouverture et fermeture des nouvelles fenêtres par la commande Accueil, création d’une page et d’un bloc temporaires, déplacement au doigt avec aimantation, suppression de la page de test, accès aux minuteurs/musique/calculatrice et mode de secours manuel suivi d’une reprise. Aucun enregistrement PRISME dans le tampon de crash consulté à la fin du parcours. Les deux entrées publiques utilityAction et controlCenterAction ont aussi été essayées avec une commande de secours non autorisée : toutes deux la refusent. Les commandes sensibles du centre de contrôle sont réservées aux appels internes du processus.

Le test a révélé la limitation de hauteur des dialogues natifs du firmware ; les nouveaux modules utilisent maintenant le cadre plein écran de PRISME. Les services vérifient également la politique de masquage pour empêcher une relance asynchrone de recouvrir l’accueil.

## Éditeur PC et site

Essai dans un vrai navigateur Edge sans interface visible : palettes, pages, annulation, refus de collision, import local d’un profil de démonstration, téléchargement du profil modifié avec conservation de la note de test, largeur de 1 280 puis 390 pixels sans débordement, changement de langue de galerie et navigation dans le zoom. Aucune erreur JavaScript de page observée.

Les captures V6 sont de vraies images de la DM100. Le carnet n’est pas prévisualisé sur l’accueil, les favoris n’exposent pas leur catalogue et aucune capture de compte n’est publiée.

## Limites et suite

Pas de certification Play Protect, pas d’audit de sécurité indépendant, pas d’essai d’autonomie prolongé, de comparaison V5/V6 reproductible de fluidité, ni de redémarrage matériel dans cette session. Le calcul après redémarrage est testé en modèle et le replanificateur est implémenté, mais sa validation matérielle reste à faire.

La session musicale synthétique ne valide pas Spotify, Deezer, YouTube ou les lecteurs OEM. Les commandes simples/enrichies dépendent de leurs sessions ; leur lecture en arrière-plan reste sous le contrôle d’Android et du firmware. Les sept langues historiques sont conservées, avec principaux libellés V6 traduits ; certains textes détaillés et tous les parcours RTL n’ont pas une validation matérielle exhaustive.

Les tests utilisent de nombreux cas de modèle : leur nombre ne signifie pas qu’autant de configurations ont été essayées physiquement. Le mode de secours a été ouvert manuellement ; trois plantages réels consécutifs n’ont pas été provoqués sur les données utilisateur.

[Guide](../V6-GUIDE.md) · [Fonctions reportées](../V6-DEVELOPMENT.md) · [Confidentialité](../PRIVACY.md)
