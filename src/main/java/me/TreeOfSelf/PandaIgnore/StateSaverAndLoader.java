package me.TreeOfSelf.PandaIgnore;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side persistent storage for PandaIgnore
 */
public class StateSaverAndLoader extends PersistentState {

    private final HashMap<UUID, PlayerIgnoreData> players = new HashMap<>();

    // ──────────────────────────────────────────────────────────────
    // PersistentState Type (Mojang's new recommended way)
    // ──────────────────────────────────────────────────────────────
    private static final Type<StateSaverAndLoader> TYPE = new Type<>(
            StateSaverAndLoader::new,                    // constructor if file doesn't exist
            StateSaverAndLoader::createFromNbt,          // deserializer
            null                                         // data fixers – not needed here
    );

    // ──────────────────────────────────────────────────────────────
    // Load from NBT
    // ──────────────────────────────────────────────────────────────
    private static StateSaverAndLoader createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        StateSaverAndLoader state = new StateSaverAndLoader();

        NbtCompound playersTag = tag.getCompound("players");
        for (String key : playersTag.getKeys()) {
            PlayerIgnoreData data = new PlayerIgnoreData();

            NbtList list = playersTag.getCompound(key).getList("ignoredPlayers", NbtCompound.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound entry = list.getCompound(i);
                data.ignoredPlayers.add(UUID.fromString(entry.getString("uuid")));
            }

            state.players.put(UUID.fromString(key), data);
        }

        return state;
    }

    // ──────────────────────────────────────────────────────────────
    // Save to NBT
    // ──────────────────────────────────────────────────────────────
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtCompound playersTag = new NbtCompound();

        players.forEach((uuid, data) -> {
            NbtCompound playerTag = new NbtCompound();

            NbtList list = new NbtList();
            for (UUID ignored : data.ignoredPlayers) {
                NbtCompound entry = new NbtCompound();
                entry.putString("uuid", ignored.toString());
                list.add(entry);
            }

            playerTag.put("ignoredPlayers", list);
            playersTag.put(uuid.toString(), playerTag);
        });

        nbt.put("players", playersTag);
        return nbt;
    }

    // ──────────────────────────────────────────────────────────────
    // Public accessors
    // ──────────────────────────────────────────────────────────────
    public static StateSaverAndLoader getServerState(MinecraftServer server) {
        // Always use Overworld – it is loaded first and never unloads
        PersistentStateManager manager = server.getWorld(World.OVERWORLD)
                .getPersistentStateManager();

        // getOrCreate automatically loads or creates the file "panda-ignore.dat" file
        StateSaverAndLoader state = manager.getOrCreate(TYPE, PandaIgnore.MOD_ID);

        // Important: only mark dirty when we actually change something!
        // (Removed the automatic markDirty() here)
        return state;
    }

    public static PlayerIgnoreData getPlayerState(net.minecraft.entity.LivingEntity entity) {
        if (entity.getWorld().isClient) {
            throw new RuntimeException("Tried to access server ignore data from client!");
        }

        StateSaverAndLoader serverState = getServerState(entity.getServer());
        return serverState.players.computeIfAbsent(entity.getUuid(), uuid -> new PlayerIgnoreData());
    }

    // Helper for commands / mixins – mark dirty when you modify data!
    public PlayerIgnoreData getPlayerData(UUID uuid) {
        return players.computeIfAbsent(uuid, u -> new PlayerIgnoreData());
    }

    // ──────────────────────────────────────────────────────────────
    // Data container
    // ──────────────────────────────────────────────────────────────
    public static class PlayerIgnoreData {
        public final Set<UUID> ignoredPlayers = new HashSet<>();
    }
}