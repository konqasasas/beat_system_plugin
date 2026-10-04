package dev.konqasasas.beat.spigot;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.List;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;
import org.junit.jupiter.api.Test;

class EnduranceGroundSupportTest {
    @Test void landingRequiredEvenInsideTheExistingHeightTolerance() {
        BoundingBox floor = new BoundingBox(0, 63, 0, 1, 64, 1);
        assertFalse(EnduranceGroundSupport.supports(player(0.5, 64.4, 0.5), floor));
        assertFalse(EnduranceGroundSupport.supports(player(0.5, 64.01, 0.5), floor));
        assertTrue(EnduranceGroundSupport.supports(player(0.5, 64, 0.5), floor));
        assertTrue(EnduranceGroundSupport.supports(player(0.5, 64.0000001, 0.5), floor));
    }

    @Test void sideAndCornerContactAreNotSupportButPartialFootOverlapIs() {
        BoundingBox floor = new BoundingBox(0, 63, 0, 1, 64, 1);
        assertFalse(EnduranceGroundSupport.supports(player(0.5, 63.8, 0.5), floor));
        assertFalse(EnduranceGroundSupport.supports(new BoundingBox(1, 64, 0, 1.6, 65.8, 0.6), floor));
        assertFalse(EnduranceGroundSupport.supports(new BoundingBox(1, 64, 1, 1.6, 65.8, 1.6), floor));
        assertTrue(EnduranceGroundSupport.supports(player(1.2, 64, 0.5), floor));
    }

    @Test void slabStairAndCarpetHeightsUseActualCollisionTop() {
        for (double height : List.of(0.5, 1.0, 0.0625, 1.5)) {
            BoundingBox block = new BoundingBox(0, 63, 0, 1, 63 + height, 1);
            assertTrue(EnduranceGroundSupport.supports(player(0.5, 63 + height, 0.5), block));
            assertFalse(EnduranceGroundSupport.supports(player(0.5, 63 + height + 0.1, 0.5), block));
        }
    }

    @Test void translatesLocalShapesAtNegativeCoordinatesAndFindsTallSupports() {
        BoundingBox local = new BoundingBox(0.375, 0, 0.375, 0.625, 1.5, 0.625);
        VoxelShape empty = shape(List.of());
        World world = proxy(World.class, (method, args) -> switch (method) {
            case "getMinHeight" -> -64;
            case "getMaxHeight" -> 320;
            case "isChunkLoaded" -> true;
            case "getBlockAt" -> proxy(Block.class, (name, unused) -> {
                if (!name.equals("getCollisionShape")) throw new AssertionError(name);
                return (int) args[0] == -17 && (int) args[1] == 63 && (int) args[2] == -1
                        ? shape(List.of(local)) : empty;
            });
            default -> throw new AssertionError(method);
        });
        assertTrue(EnduranceGroundSupport.isSupported(world, player(-16.5, 64.5, -0.5)));
        assertFalse(EnduranceGroundSupport.isSupported(world, player(-16.5, 64.6, -0.5)));
        assertEquals(new BoundingBox(0.375, 0, 0.375, 0.625, 1.5, 0.625), local);
    }

    private static BoundingBox player(double x, double y, double z) {
        return new BoundingBox(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
    }

    private static VoxelShape shape(List<BoundingBox> boxes) {
        return proxy(VoxelShape.class, (method, args) -> {
            if (method.equals("getBoundingBoxes")) return boxes;
            throw new AssertionError(method);
        });
    }

    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> call.invoke(method.getName(), args)));
    }

    private interface Call { Object invoke(String method, Object[] args); }
}
