package fr.arbres;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Arbres sur les îles extérieures de l'End et tours d'obsidienne embellies sur l'île centrale. */
final class EndGen {
    private EndGen() {}

    private static final int NONE = Integer.MIN_VALUE;

    private static double hash(long seed, int x, int y, int z) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 31) * 0xBF58476D1CE4E5B9L;
        h ^= y * 0x94D049BB133111EBL;
        h = Long.rotateLeft(h, 27) * 0x9E3779B97F4A7C15L;
        h ^= z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) * (1.0 / (1L << 53));
    }

    private static Material mat(String name, Material fallback) {
        Material m = Material.getMaterial(name);
        return m == null ? fallback : m;
    }

    // ================================================================== arbres des îles extérieures

    private static int groundY(Canvas cv, int x, int z) {
        for (int y = Math.min(cv.maxY() - 1, 130); y > 20; y--) {
            Material m = cv.type(x, y, z);
            if (m == null) return NONE;
            if (m.isAir() || Blocks.replaceable(m)) continue;
            return y;
        }
        return NONE;
    }

    static int trees(Canvas cv, int chunkX, int chunkZ, long seed, double chance, double scale) {
        long wx = chunkX * 16L + 8, wz = chunkZ * 16L + 8;
        if (wx * wx + wz * wz < 700L * 700L) return 0; // pas sur l'île centrale
        Random r = new Random(TreeGen.mix(seed * 29L + chunkX * 341873128712L + chunkZ * 132897987541L + 777L));
        if (r.nextDouble() >= chance) return 0;
        TreeGen.Species sp = TreeGen.Species.PALE_OAK;
        if (!sp.ok()) return 0;
        int wanted = 1 + r.nextInt(3);
        int made = 0;
        List<int[]> placed = new ArrayList<>();
        for (int attempt = 0; attempt < 16 && made < wanted; attempt++) {
            int x = chunkX * 16 + 3 + r.nextInt(10), z = chunkZ * 16 + 3 + r.nextInt(10);
            int y = groundY(cv, x, z);
            if (y == NONE || cv.type(x, y, z) != Material.END_STONE) continue;
            if (cv.biome(x, y, z).equals("the_end")) continue;
            boolean flat = true;
            int[][] around = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}};
            for (int[] a : around) {
                int gy = groundY(cv, x + a[0], z + a[1]);
                if (gy == NONE || Math.abs(gy - y) > 2 || cv.type(x + a[0], gy, z + a[1]) != Material.END_STONE) flat = false;
            }
            if (!flat) continue;
            for (int k = 1; k <= 22 && flat; k++) {
                Material a = cv.type(x, y + k, z);
                if (a == null || !(a.isAir() || Blocks.replaceable(a))) flat = false;
            }
            if (!flat) continue;
            boolean far = true;
            for (int[] p : placed) {
                if (Math.abs(p[0] - x) + Math.abs(p[1] - z) < 8) far = false;
            }
            if (!far) continue;
            int th = 1;
            if (r.nextInt(3) == 0 && cv.type(x + 1, y, z) == Material.END_STONE && cv.type(x, y, z + 1) == Material.END_STONE
                    && cv.type(x + 1, y, z + 1) == Material.END_STONE) th = 2;
            Random tr = new Random(TreeGen.mix(seed ^ (x * 341873128712L) ^ (z * 132897987541L) ^ y));
            TreeGen.Shape shape = TreeGen.build(sp, tr, x, y + 1, z, th, scale, tr.nextLong());
            TreeGen.apply(shape, sp, cv, false);
            placed.add(new int[] {x, z});
            made++;
        }
        return made;
    }

    // ================================================================== tours d'obsidienne

    static int towers(Canvas cv, int chunkX, int chunkZ, long seed) {
        if (Math.abs(chunkX) > 4 || Math.abs(chunkZ) > 4) return 0;
        int made = 0;
        for (int i = 0; i < 10; i++) { // mêmes positions que les pics vanilla
            double a = 2.0 * (-Math.PI + (Math.PI / 10.0) * i);
            int cx = (int) Math.floor(42.0 * Math.cos(a));
            int cz = (int) Math.floor(42.0 * Math.sin(a));
            if ((cx >> 4) != chunkX || (cz >> 4) != chunkZ) continue;
            if (tower(cv, cx, cz, seed)) made++;
        }
        return made;
    }

    private static boolean tower(Canvas cv, int cx, int cz, long seed) {
        int h = NONE;
        for (int y = Math.min(cv.maxY() - 1, 120); y >= 50; y--) {
            Material m = cv.type(cx, y, cz);
            if (m == null) return false;
            if (m == Material.BEDROCK) { // la base du cristal
                Material b = cv.type(cx, y - 1, cz);
                if (b == Material.OBSIDIAN || b == Material.CRYING_OBSIDIAN) h = y;
                break;
            }
        }
        if (h == NONE) return false;
        if (cv.type(cx, h - 1, cz) == Material.CRYING_OBSIDIAN) return false; // déjà embellie
        int rad = 0;
        while (rad < 8 && cv.type(cx + rad + 1, h - 3, cz) == Material.OBSIDIAN) rad++;
        if (rad < 2) return false;

        // hauteur du sol : fond de l'île autour de la tour
        int gy = NONE;
        int[][] probes = {{rad + 3, 0}, {-(rad + 3), 0}, {0, rad + 3}, {0, -(rad + 3)}};
        for (int[] p : probes) {
            for (int y = h; y > 30; y--) {
                Material m = cv.type(cx + p[0], y, cz + p[1]);
                if (m == null) break;
                if (m == Material.END_STONE) {
                    gy = Math.max(gy, y);
                    break;
                }
            }
        }
        if (gy == NONE || h - gy < 10) return false;

        Material obs = Material.OBSIDIAN;
        Material cry = Material.CRYING_OBSIDIAN;
        Material bs = mat("BLACKSTONE", obs);
        Material pbb = mat("POLISHED_BLACKSTONE_BRICKS", obs);
        Material pur = mat("PURPUR_PILLAR", obs);
        BlockData dObs = obs.createBlockData();
        BlockData dCry = cry.createBlockData();
        BlockData dBs = bs.createBlockData();
        BlockData dPbb = pbb.createBlockData();
        BlockData dPur = pur.createBlockData();
        BlockData rod = mat("END_ROD", Material.AIR).createBlockData();

        // 1) peau du fût : bandes, colonnes de purpur aux 4 points cardinaux, un peu d'obsidienne pleureuse
        for (int y = gy + 1; y <= h - 1; y++) {
            int k = y - gy;
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > rad * rad || d2 < (rad - 1) * (rad - 1)) continue;
                    if (cv.type(cx + dx, y, cz + dz) != Material.OBSIDIAN) continue;
                    double n = hash(seed, cx + dx, y, cz + dz);
                    BlockData d;
                    if ((dx == 0 || dz == 0)) d = dPur;
                    else if (k % 9 == 0 || (k % 9 == 1 && n < 0.5)) d = dPbb;
                    else if (n < 0.10) d = dCry;
                    else if (n < 0.24) d = dBs;
                    else d = dObs;
                    cv.set(cx + dx, y, cz + dz, d);
                }
            }
        }

        // 2) pied évasé
        for (int k = 1; k <= 9; k++) {
            double ext = 3.2 - k * 0.36;
            if (ext <= 0.3) break;
            double R = rad + ext;
            int ir = (int) Math.ceil(R);
            int y = gy + k;
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > R * R || d2 <= rad * rad) continue;
                    Material t = cv.type(cx + dx, y, cz + dz);
                    if (t == null || !(t.isAir() || Blocks.replaceable(t))) continue;
                    double n = hash(seed + 1, cx + dx, y, cz + dz);
                    cv.set(cx + dx, y, cz + dz, n < 0.35 ? dObs : n < 0.6 ? dBs : dPbb);
                    if (k == 1) { // combler le vide sous le pied
                        for (int yy = y - 1; yy > y - 7; yy--) {
                            Material u = cv.type(cx + dx, yy, cz + dz);
                            if (u == null || !u.isAir()) break;
                            cv.set(cx + dx, yy, cz + dz, dBs);
                        }
                    }
                }
            }
        }

        // 3) balcons avec lumières
        int[] ledges = {gy + (h - gy) / 2, h - 9};
        for (int ly : ledges) {
            if (ly <= gy + 6 || ly >= h - 2) continue;
            ring(cv, cx, cz, ly, rad + 0.5, rad + 2.4, dPbb);
            cardinalRods(cv, cx, cz, ly + 1, rad + 2, rod);
        }

        // 4) couronne sous le cristal (rien n'est touché à partir de la bedrock)
        ring(cv, cx, cz, h - 1, rad + 0.5, rad + 1.6, dPbb);
        cardinalRods(cv, cx, cz, h, rad + 1, rod);

        // 5) marqueur (caché dans le fût) pour ne jamais refaire la tour
        cv.set(cx, h - 1, cz, dCry);
        return true;
    }

    /** Anneau de blocs dans l'air entre deux rayons. */
    private static void ring(Canvas cv, int cx, int cz, int y, double r0, double r1, BlockData d) {
        int ir = (int) Math.ceil(r1);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 <= r0 * r0 || d2 > r1 * r1) continue;
                Material t = cv.type(cx + dx, y, cz + dz);
                if (t != null && t.isAir()) cv.set(cx + dx, y, cz + dz, d);
            }
        }
    }

    private static void cardinalRods(Canvas cv, int cx, int cz, int y, int dist, BlockData rod) {
        if (rod.getMaterial() == Material.AIR) return;
        int[][] c = {{dist, 0}, {-dist, 0}, {0, dist}, {0, -dist}};
        for (int[] p : c) {
            Material t = cv.type(cx + p[0], y, cz + p[1]);
            Material below = cv.type(cx + p[0], y - 1, cz + p[1]);
            if (t != null && t.isAir() && below != null && !below.isAir()) cv.set(cx + p[0], y, cz + p[1], rod);
        }
    }
}
