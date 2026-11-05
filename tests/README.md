# Tests PRISME 5.1

Le 31 octobre : 623 assertions de préférences/outils, 43 817 de placement sur 378 configurations, 784 de gestes du centre, 20 162 de géométrie du volume sur 720 configurations et 109 de bord à bord. Tous passent.

Lancer run-tests.ps1, run-viewport-tests.ps1, run-gesture-tests.ps1, run-volume-tests.ps1 et run-edge-tests.ps1. Le JDK local et android-json.jar dans libs sont nécessaires. Ces tests utilisent les modèles de production avec des adaptateurs Android en mémoire ; ils ne modifient pas les données utilisateur.

api25_audit.py vérifie les références publiques du DEX final contre l’historique API du SDK local. Le chemin HISTORY peut être adapté si le SDK est déplacé. L’audit ne remplace pas les essais sur la montre et ne démontre pas tout le contrôle de flux.

Les rapports de la livraison et la galerie générée sur la montre complètent ces contrôles.

La version 5.1 ajoute dix assertions de compatibilité et dédoublonnage du bouton Rotation : 242 assertions du centre de contrôle sur sept scénarios.

## Audit du 5 novembre 2026

104 assertions supplémentaires couvrent les limites, types corrompus, palettes et portées des préférences Batterie/Musique. `run-all.ps1` les exécute également.

`node tests/website.cjs` vérifie le chargement de la galerie, les changements de langue concurrents, les réponses HTTP en erreur, les données invalides, la reprise et la navigation dans la fenêtre d’image. Ce test isolé ne remplace pas une vérification visuelle dans un navigateur.

## V6 bêta

`run-all.ps1` inclut V6CoreTest : validation de la grille, collisions, imports complets, annulation, récupération après interruption, calculs de compte à rebours, bornage des utilitaires et chiffrement avec mots de passe erronés/fichiers altérés. `node tests/home-model.cjs` vérifie les mêmes frontières de disposition côté éditeur PC. [Essais Android réels](device/README.md).

La validation du modèle n’est pas une preuve d’autonomie, de fluidité ou de compatibilité avec tous les lecteurs musicaux. Voir [le rapport V6](../docs/V6-VALIDATION.md).
