package dev.konqasasas.beat.spigot;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/** Block support for progress acquisition; never uses the client's on-ground flag. */
final class EnduranceGroundSupport {
    // Numerical tolerance only: do not turn the space above a landing into a trigger.
    private static final double EPSILON = 1.0E-5;

    private EnduranceGroundSupport() {}

    static boolean isGrounded(Player player) {
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle() || player.isInWater()) {
            return false;
        }
        return isSupported(player.getWorld(), player.getBoundingBox());
    }

    static boolean isSupported(World world, BoundingBox feet) {
        // Fences and walls can extend 1.5 blocks above their block origin.
        int minY = Math.max(world.getMinHeight(), (int) Math.floor(feet.getMinY() - 1.5 - EPSILON));
        int maxY = Math.min(world.getMaxHeight() - 1, (int) Math.floor(feet.getMinY() + EPSILON));
        for (int x = (int) Math.floor(feet.getMinX()); x <= (int) Math.floor(feet.getMaxX()); x++) {
            for (int z = (int) Math.floor(feet.getMinZ()); z <= (int) Math.floor(feet.getMaxZ()); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                for (int y = minY; y <= maxY; y++) {
                    var block = world.getBlockAt(x, y, z);
                    for (BoundingBox shape : block.getCollisionShape().getBoundingBoxes()) {
                        // Spigot 26.2 returns block-local collision boxes. Verified against
                        // CraftBlock/CraftVoxelShape in the development server JAR.
                        if (supports(feet, shape.clone().shift(x, y, z))) return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean supports(BoundingBox player, BoundingBox block) {
        return block.getHeight() > 0
                && Math.abs(player.getMinY() - block.getMaxY()) <= EPSILON
                && Math.min(player.getMaxX(), block.getMaxX()) > Math.max(player.getMinX(), block.getMinX())
                && Math.min(player.getMaxZ(), block.getMaxZ()) > Math.max(player.getMinZ(), block.getMinZ());
    }
}
