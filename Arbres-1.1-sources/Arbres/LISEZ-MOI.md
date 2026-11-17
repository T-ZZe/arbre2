# Arbres 1.1 : sources

## Compiler
Dans ce dossier : `mvn package` -> le jar est dans `target/Arbres-1.1.jar` (Java 25, Paper 26.3).
Remplace l'ancien jar dans `plugins/`, supprime l'ancien `config.yml` (ou ajoute les nouvelles sections), puis redémarre.

## Bugs corrigés
1. Restes sous les arbres : nouvelle classe `Cleaner` (feuilles sans tronc, lianes / mousse pendante / cacao / coeur de creaking / neige en l'air, troncs flottants).
   Elle tourne à chaque génération de chunk, et `/arbres convert [rayon]` nettoie aussi les chunks déjà générés.
2. Branches à nu : toutes les branches (hors conifères) sont habillées de feuilles (`TreeGen.dress`), plus le haut du tronc des bonsaïs.
3. Feuilles manquantes : touffes plus denses (moins de trous dans `blob`).

## Nouveautés (toutes réglables dans config.yml)
- Champignons géants de l'île champignon (dôme rouge, chapeau plat brun, version 2 étages), aussi à la poudre d'os
- Fungus du Nether (ombrelles de verrues, shroomlight, lianes pleureuses)
- Grands pics de glace (biome ice_spikes)
- Pics de pierre moussus au bord de l'eau (stony_shore / stony_peaks)
- Arbres (pale oak) sur les îles extérieures de l'End
- Tours d'obsidienne de l'île centrale : bandes, colonnes de purpur, pied évasé, balcons, end rods
- Palmiers sur les plages

Les arbres déjà générés gardent leur ancienne forme : le nouveau style s'applique aux nouveaux chunks.
