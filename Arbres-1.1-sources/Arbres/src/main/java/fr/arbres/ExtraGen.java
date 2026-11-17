package fr.arbres;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Palmiers de plage, pics de glace, pics de pierre moussus au bord de l'eau. */
final class ExtraGen {
    private ExtraGen() {}

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

    /** Premier bloc solide en descendant (feuillage, herbes... ignorés). Eau => NONE. */
    static int surface(Canvas cv, int x, int z) {
        int y = cv.topY(x, z);
        while (y > cv.minY()) {
            Material m = cv.type(x, y, z);
            if (m == null) return NONE;
            if (m.isAir() || Blocks.leaf(m) || Blocks.log(m) || Blocks.replaceable(m)) {
                y--;
                continue;
            }
            if (Blocks.liquid(m)) return NONE;
            return y;
        }
        return NONE;
    }

    private static boolean nearWater(Canvas cv, int x, int y, int z, int maxD) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int d = 2; d <= maxD; d += 2) {
            for (int[] v : dirs) {
                int px = x + v[0] * d, pz = z + v[1] * d;
                for (int dy = -2; dy <= 0; dy++) {
                    if (cv.type(px, y + dy, pz) == Material.WATER) return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ palmiers de plage

    static int palms(Canvas cv, int chunkX, int chunkZ, long seed, double chance) {
        Random r = new Random(TreeGen.mix(seed * 13L + chunkX * 341873128712L + chunkZ * 132897987541L + 9001L));
        if (r.nextDouble() >= chance) return 0;
        BlockData log = Material.JUNGLE_LOG.createBlockData();
        BlockData leaf = Material.JUNGLE_LEAVES.createBlockData();
        if (leaf instanceof Leaves l) {
            l.setPersistent(true);
            l.setDistance(1);
        }
        int wanted = 1 + r.nextInt(4);
        int mx = chunkX * 16 + 4 + r.nextInt(8);
        int mz = chunkZ * 16 + 4 + r.nextInt(8);
        List<int[]> placed = new ArrayList<>();
        int made = 0;
        for (int attempt = 0; attempt < 24 && made < wanted; attempt++) {
            int x = mx + r.nextInt(11) - 5, z = mz + r.nextInt(11) - 5;
            int y = surface(cv, x, z);
            if (y == NONE) continue;
            if (cv.type(x, y, z) != Material.SAND) continue;
            String biome = cv.biome(x, y, z);
            if (!biome.contains("beach") || biome.contains("snowy")) continue;
            boolean far = true;
            for (int[] p : placed) {
                if (Math.abs(p[0] - x) + Math.abs(p[1] - z) < 4) far = false;
            }
            if (!far || !nearWater(cv, x, y, z, 10)) continue;
            boolean free = true;
            for (int k = 1; k <= 12 && free; k++) {
                Material a = cv.type(x, y + k, z);
                if (a == null || !(a.isAir() || Blocks.replaceable(a))) free = false;
            }
            if (!free) continue;
            OasisGen.palm(cv, r, x, y, z, log, leaf, 6 + r.nextInt(6));
            placed.add(new int[] {x, z});
            made++;
        }
        return made;
    }

    // ------------------------------------------------------------------ pics de glace

    private static boolean iceGround(Material m) {
        String n = m.name();
        return n.equals("SNOW_BLOCK") || n.equals("PACKED_ICE") || n.equals("ICE") || n.equals("BLUE_ICE")
                || n.equals("POWDER_SNOW");
    }

    private static boolean iceish(Material m) {
        String n = m.name();
        return n.equals("PACKED_ICE") || n.equals("ICE") || n.equals("BLUE_ICE");
    }

    static int iceSpikes(Canvas cv, int chunkX, int chunkZ, long seed, double chance) {
        Random r = new Random(TreeGen.mix(seed * 19L + chunkX * 341873128712L + chunkZ * 132897987541L + 5151L));
        if (r.nextDouble() >= chance) return 0;
        int x = chunkX * 16 + 4 + r.nextInt(8), z = chunkZ * 16 + 4 + r.nextInt(8);
        int y = surface(cv, x, z);
        if (y == NONE || !cv.biome(x, y, z).contains("ice_spikes")) return 0;
        Material g = cv.type(x, y, z);
        if (g == null || !iceGround(g)) return 0;
        iceSpike(cv, r, seed, x, y, z, 18 + r.nextInt(18), 3.0 + r.nextDouble() * 2.0, 0.30);
        int made = 1;
        int sat = 2 + r.nextInt(4);
        for (int i = 0; i < sat; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = 4 + r.nextDouble() * 4;
            int sx = x + (int) Math.round(Math.cos(a) * d), sz = z + (int) Math.round(Math.sin(a) * d);
            int sy = surface(cv, sx, sz);
            if (sy == NONE) continue;
            Material sg = cv.type(sx, sy, sz);
            if (sg == null || !iceGround(sg)) continue;
            iceSpike(cv, r, seed, sx, sy, sz, 6 + r.nextInt(11), 1.4 + r.nextDouble() * 1.4, 0.35);
            made++;
        }
        return made;
    }

    private static void iceSpike(Canvas cv, Random r, long seed, int x, int y, int z, int h, double baseR, double leanMax) {
        double ang = r.nextDouble() * Math.PI * 2;
        double lean = r.nextDouble() * leanMax;
        double lx = Math.cos(ang) * lean, lz = Math.sin(ang) * lean;
        BlockData packed = Material.PACKED_ICE.createBlockData();
        BlockData ice = Material.ICE.createBlockData();
        BlockData blue = Material.BLUE_ICE.createBlockData();
        long s = seed ^ (x * 73856093L) ^ (z * 19349663L);
        for (int k = -2; k <= h; k++) {
            double t = Math.max(0, k) / (double) h;
            double rad = baseR * Math.pow(1.0 - t, 0.9) + 0.45;
            double ccx = x + lx * Math.max(0, k), ccz = z + lz * Math.max(0, k);
            int ir = (int) Math.ceil(rad) + 1;
            int cxi = (int) Math.round(ccx), czi = (int) Math.round(ccz);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    int px = cxi + dx, pz = czi + dz;
                    double ddx = px - ccx, ddz = pz - ccz;
                    double d2 = (ddx * ddx + ddz * ddz) / (rad * rad);
                    double n = hash(s, px, y + k, pz);
                    if (d2 + (n - 0.5) * 0.5 > 1.0) continue;
                    Material cur = cv.type(px, y + k, pz);
                    if (cur == null) continue;
                    boolean ok = cur.isAir() || Blocks.replaceable(cur) || (k <= 0 ? iceGround(cur) : iceish(cur));
                    if (!ok) continue;
                    double n2 = hash(s + 1, px, y + k, pz);
                    BlockData d;
                    if (k <= 0) d = packed;
                    else if (k == h) d = ice;
                    else if (d2 < 0.35) d = n2 < 0.4 ? blue : packed;
                    else d = n2 < 0.55 ? packed : ice;
                    cv.set(px, y + k, pz, d);
                }
            }
        }
    }

    // ------------------------------------------------------------------ pics de pierre moussus

    private static boolean rockGround(Material m) {
        String n = m.name();
        return n.equals("STONE") || n.equals("ANDESITE") || n.equals("DIORITE") || n.equals("GRANITE")
                || n.equals("TUFF") || n.equals("GRAVEL") || n.equals("COBBLESTONE") || n.equals("DIRT")
                || n.equals("GRASS_BLOCK") || n.equals("COARSE_DIRT") || n.equals("SAND");
    }

    static int stoneSpikes(Canvas cv, int chunkX, int chunkZ, long seed, double chance, List<String> biomes) {
        Random r = new Random(TreeGen.mix(seed * 23L + chunkX * 341873128712L + chunkZ * 132897987541L + 3131L));
        if (r.nextDouble() >= chance) return 0;
        int wanted = 1 + r.nextInt(3);
        int made = 0;
        for (int attempt = 0; attempt < 12 && made < wanted; attempt++) {
            int x = chunkX * 16 + 3 + r.nextInt(10), z = chunkZ * 16 + 3 + r.nextInt(10);
            int y = surface(cv, x, z);
            if (y == NONE) continue;
            Material g = cv.type(x, y, z);
            if (g == null || !rockGround(g)) continue;
            String biome = cv.biome(x, y, z);
            boolean okBiome = false;
            for (String b : biomes) {
                if (biome.contains(b)) okBiome = true;
            }
            if (!okBiome || !nearWater(cv, x, y, z, 9)) continue;
            stoneSpike(cv, r, seed, x, y, z, 5 + r.nextInt(12), 1.3 + r.nextDouble() * 1.6);
            made++;
        }
        return made;
    }

    private static void stoneSpike(Canvas cv, Random r, long seed, int x, int y, int z, int h, double baseR) {
        double ang = r.nextDouble() * Math.PI * 2;
        double lean = r.nextDouble() * 0.22;
        double lx = Math.cos(ang) * lean, lz = Math.sin(ang) * lean;
        long s = seed ^ (x * 73856093L) ^ (z * 19349663L);
        Map<Long, Integer> tops = new HashMap<>();
        Map<Long, int[]> where = new HashMap<>();
        BlockData stone = Material.STONE.createBlockData();
        BlockData andesite = Material.ANDESITE.createBlockData();
        BlockData cobble = Material.COBBLESTONE.createBlockData();
        BlockData mossyCobble = Material.MOSSY_COBBLESTONE.createBlockData();
        BlockData moss = Material.MOSS_BLOCK.createBlockData();
        for (int k = -1; k <= h; k++) {
            double t = Math.max(0, k) / (double) h;
            double rad = baseR * (1.0 - Math.pow(t, 1.15)) + 0.35;
            double ccx = x + lx * Math.max(0, k), ccz = z + lz * Math.max(0, k);
            int ir = (int) Math.ceil(rad) + 1;
            int cxi = (int) Math.round(ccx), czi = (int) Math.round(ccz);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    int px = cxi + dx, pz = czi + dz;
                    double ddx = px - ccx, ddz = pz - ccz;
                    double d2 = (ddx * ddx + ddz * ddz) / (rad * rad);
                    double n = hash(s, px, y + k, pz);
                    if (d2 + (n - 0.5) * 0.5 > 1.0) continue;
                    Material cur = cv.type(px, y + k, pz);
                    if (cur == null) continue;
                    boolean ok = cur.isAir() || Blocks.replaceable(cur) || (k <= 0 && rockGround(cur));
                    if (!ok) continue;
                    double n2 = hash(s + 1, px, y + k, pz);
                    BlockData d;
                    if (k >= h - 1 || (k > h * 0.4 && n2 < 0.12)) d = moss;
                    else if (n2 < 0.50) d = stone;
                    else if (n2 < 0.70) d = andesite;
                    else if (n2 < 0.85) d = cobble;
                    else d = mossyCobble;
                    cv.set(px, y + k, pz, d);
                    long key = ((long) px << 32) ^ (pz & 0xffffffffL);
                    Integer old = tops.get(key);
                    if (old == null || old < y + k) {
                        tops.put(key, y + k);
                        where.put(key, new int[] {px, pz});
                    }
                }
            }
        }
        // mousse au sommet de chaque colonne : bloc de mousse + tapis, et parfois une azalée
        BlockData carpet = mat("MOSS_CARPET");
        Material az = Material.getMaterial(r.nextBoolean() ? "AZALEA" : "FLOWERING_AZALEA");
        int best = Integer.MIN_VALUE;
        int[] bestPos = null;
        for (Map.Entry<Long, Integer> e : tops.entrySet()) {
            int[] p = where.get(e.getKey());
            int ty = e.getValue();
            if (ty > best) {
                best = ty;
                bestPos = p;
            }
            Material cur = cv.type(p[0], ty, p[1]);
            if (cur == null) continue;
            cv.set(p[0], ty, p[1], moss);
            Material above = cv.type(p[0], ty + 1, p[1]);
            if (above != null && above.isAir() && carpet != null && hash(s + 2, p[0], ty, p[1]) < 0.75) {
                cv.set(p[0], ty + 1, p[1], carpet);
            }
        }
        if (bestPos != null && az != null) {
            Material above = cv.type(bestPos[0], best + 1, bestPos[1]);
            if (above != null && (above.isAir() || Blocks.replaceable(above)) && r.nextDouble() < 0.6) {
                cv.set(bestPos[0], best + 1, bestPos[1], az.createBlockData());
            }
        }
    }

    private static BlockData mat(String name) {
        Material m = Material.getMaterial(name);
        return m == null ? null : m.createBlockData();
    }
}
