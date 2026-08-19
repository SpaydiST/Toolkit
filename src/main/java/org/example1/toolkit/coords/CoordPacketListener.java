package org.example1.toolkit.coords;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionTypeRef;
import com.github.retrooper.packetevents.protocol.world.waypoint.ChunkWaypointInfo;
import com.github.retrooper.packetevents.protocol.world.waypoint.TrackedWaypoint;
import com.github.retrooper.packetevents.protocol.world.waypoint.Vec3iWaypointInfo;
import com.github.retrooper.packetevents.protocol.world.waypoint.WaypointInfo;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientGenerateStructure;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItemFromBlock;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSetStructureBlock;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateCommandBlock;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateJigsawBlock;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAcknowledgePlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityPositionSync;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerFacePlayer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerInitializeWorldBorder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMoveMinecart;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMoveMinecart.MinecartStep;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnExperienceOrb;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPosition;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateLight;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateViewPosition;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerVehicleMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWaypoint;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderCenter;
import org.bukkit.Bukkit;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CoordPacketListener implements PacketListener {

    private final Set<String> loggedErrors = ConcurrentHashMap.newKeySet();

    private static final String LSS_CHANNEL_REQUEST = "lss:batch_chunk_req";
    private static final String LSS_CHANNEL_COLUMN = "lss:voxel_column";
    private static final String LSS_CHANNEL_BATCH_RESPONSE = "lss:batch_response";
    private static final String LSS_CHANNEL_DIRTY_COLUMNS = "lss:dirty_columns";

    public CoordPacketListener() {
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (markVoxySend(event)) {
            return;
        }
        PlayerOffsets offsets = CoordObfuscator.offsets(event.getUser());
        if (offsets == null && event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            offsets = CoordObfuscator.getOrCreateOffsets(event.getUser());
        }
        if (offsets == null || offsets.bypass() || offsets.isCurrentZero()) {
            return;
        }
        try {
            handleSend(event, offsets.current());
        } catch (Throwable t) {
            logError(event.getPacketName(), t);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (markVoxyReceive(event)) {
            return;
        }
        PlayerOffsets offsets = CoordObfuscator.offsets(event.getUser());
        if (offsets == null || offsets.bypass() || offsets.isCurrentZero()) {
            return;
        }
        try {
            handleReceive(event, offsets.current());
        } catch (Throwable t) {
            logError(event.getPacketName(), t);
        }
    }

    private void logError(String packetName, Throwable t) {
        if (loggedErrors.add(packetName)) {
            Bukkit.getLogger().warning("[Toolkit] Ошибка обфускации координат в пакете " + packetName + ": " + t);
        }
    }

    private void handleSend(PacketSendEvent event, FixedOffset offset) {
        boolean handled = true;
        if (event.getPacketType() instanceof PacketType.Play.Server type) {
            switch (type) {
                case PLAYER_POSITION_AND_LOOK -> {
                    WrapperPlayServerPlayerPositionAndLook wrapper = new WrapperPlayServerPlayerPositionAndLook(event);
                    if (!wrapper.isRelativeFlag(RelativeFlag.X)) {
                        wrapper.setX(wrapper.getX() - offset.x());
                    }
                    if (!wrapper.isRelativeFlag(RelativeFlag.Z)) {
                        wrapper.setZ(wrapper.getZ() - offset.z());
                    }
                }
                case SPAWN_ENTITY -> {
                    WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                }
                case SPAWN_LIVING_ENTITY -> {
                    WrapperPlayServerSpawnLivingEntity wrapper = new WrapperPlayServerSpawnLivingEntity(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                }
                case SPAWN_PLAYER -> {
                    WrapperPlayServerSpawnPlayer wrapper = new WrapperPlayServerSpawnPlayer(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                }
                case SPAWN_EXPERIENCE_ORB -> {
                    WrapperPlayServerSpawnExperienceOrb wrapper = new WrapperPlayServerSpawnExperienceOrb(event);
                    wrapper.setX(wrapper.getX() - offset.x());
                    wrapper.setZ(wrapper.getZ() - offset.z());
                }
                case ENTITY_POSITION_SYNC -> {
                    WrapperPlayServerEntityPositionSync wrapper = new WrapperPlayServerEntityPositionSync(event);
                    wrapper.getValues().setPosition(CoordObfuscator.apply(wrapper.getValues().getPosition(), offset));
                }
                case ENTITY_TELEPORT -> {
                    WrapperPlayServerEntityTeleport wrapper = new WrapperPlayServerEntityTeleport(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                }
                case VEHICLE_MOVE -> {
                    WrapperPlayServerVehicleMove wrapper = new WrapperPlayServerVehicleMove(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                }
                case MOVE_MINECART -> {
                    WrapperPlayServerMoveMinecart wrapper = new WrapperPlayServerMoveMinecart(event);
                    for (MinecartStep step : wrapper.getLerpSteps()) {
                        step.setPosition(CoordObfuscator.apply(step.getPosition(), offset));
                    }
                }
                case CHUNK_DATA -> {
                    WrapperPlayServerChunkData wrapper = new WrapperPlayServerChunkData(event);
                    wrapper.setColumn(CoordObfuscator.applyColumn(wrapper.getColumn(), offset));
                }
                case UPDATE_LIGHT -> {
                    WrapperPlayServerUpdateLight wrapper = new WrapperPlayServerUpdateLight(event);
                    wrapper.setX(offset.applyChunkX(wrapper.getX()));
                    wrapper.setZ(offset.applyChunkZ(wrapper.getZ()));
                }
                case UNLOAD_CHUNK -> {
                    WrapperPlayServerUnloadChunk wrapper = new WrapperPlayServerUnloadChunk(event);
                    wrapper.setChunkX(offset.applyChunkX(wrapper.getChunkX()));
                    wrapper.setChunkZ(offset.applyChunkZ(wrapper.getChunkZ()));
                }
                case UPDATE_VIEW_POSITION -> {
                    WrapperPlayServerUpdateViewPosition wrapper = new WrapperPlayServerUpdateViewPosition(event);
                    wrapper.setChunkX(offset.applyChunkX(wrapper.getChunkX()));
                    wrapper.setChunkZ(offset.applyChunkZ(wrapper.getChunkZ()));
                }
                case SPAWN_POSITION -> {
                    WrapperPlayServerSpawnPosition wrapper = new WrapperPlayServerSpawnPosition(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                }
                case BLOCK_CHANGE -> {
                    WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(event);
                    wrapper.setBlockPosition(CoordObfuscator.apply(wrapper.getBlockPosition(), offset));
                }
                case MULTI_BLOCK_CHANGE -> {
                    WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange(event);
                    wrapper.setChunkPosition(CoordObfuscator.applyChunk(wrapper.getChunkPosition(), offset));
                }
                case BLOCK_ENTITY_DATA -> {
                    WrapperPlayServerBlockEntityData wrapper = new WrapperPlayServerBlockEntityData(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                    if (wrapper.getNBT() != null) {
                        NBTCompound nbt = wrapper.getNBT();
                        NBTNumber x = nbt.getNumberTagOrNull("x");
                        NBTNumber z = nbt.getNumberTagOrNull("z");
                        if (x != null && z != null) {
                            nbt.setTag("x", new NBTInt(x.getAsInt() - offset.x()));
                            nbt.setTag("z", new NBTInt(z.getAsInt() - offset.z()));
                        }
                    }
                }
                case BLOCK_ACTION -> {
                    WrapperPlayServerBlockAction wrapper = new WrapperPlayServerBlockAction(event);
                    wrapper.setBlockPosition(CoordObfuscator.apply(wrapper.getBlockPosition(), offset));
                }
                case BLOCK_BREAK_ANIMATION -> {
                    WrapperPlayServerBlockBreakAnimation wrapper = new WrapperPlayServerBlockBreakAnimation(event);
                    wrapper.setBlockPosition(CoordObfuscator.apply(wrapper.getBlockPosition(), offset));
                }
                case ACKNOWLEDGE_PLAYER_DIGGING -> {
                    WrapperPlayServerAcknowledgePlayerDigging wrapper = new WrapperPlayServerAcknowledgePlayerDigging(event);
                    wrapper.setBlockPosition(CoordObfuscator.apply(wrapper.getBlockPosition(), offset));
                }
                case EFFECT -> {
                    WrapperPlayServerEffect wrapper = new WrapperPlayServerEffect(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                }
                case OPEN_SIGN_EDITOR -> {
                    WrapperPlayServerOpenSignEditor wrapper = new WrapperPlayServerOpenSignEditor(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                }
                case SOUND_EFFECT -> {
                    WrapperPlayServerSoundEffect wrapper = new WrapperPlayServerSoundEffect(event);
                    if (wrapper.getPosition() != null) {
                        wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                    }
                    if (wrapper.getEffectPosition() != null) {
                        wrapper.setEffectPosition(CoordObfuscator.applyTimes8(wrapper.getEffectPosition(), offset));
                    }
                }
                case PARTICLE -> {
                    WrapperPlayServerParticle wrapper = new WrapperPlayServerParticle(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                    CoordObfuscator.applyParticleData(wrapper.getParticle(), offset);
                }
                case EXPLOSION -> {
                    WrapperPlayServerExplosion wrapper = new WrapperPlayServerExplosion(event);
                    wrapper.setPosition(CoordObfuscator.apply(wrapper.getPosition(), offset));
                    if (wrapper.getRecords() != null) {
                        wrapper.setRecords(wrapper.getRecords().stream()
                                .map(v -> CoordObfuscator.apply(v, offset))
                                .toList());
                    }
                }
                case FACE_PLAYER -> {
                    WrapperPlayServerFacePlayer wrapper = new WrapperPlayServerFacePlayer(event);
                    if (wrapper.getTargetPosition() != null) {
                        wrapper.setTargetPosition(CoordObfuscator.apply(wrapper.getTargetPosition(), offset));
                    }
                }
                case JOIN_GAME -> {
                    WrapperPlayServerJoinGame wrapper = new WrapperPlayServerJoinGame(event);
                    DimensionTypeRef dimensionType = wrapper.getDimensionTypeRef();
                    String dimensionName = dimensionType == null
                            ? wrapper.getWorldName()
                            : dimensionType.getName().toString();
                    FixedOffset joinOffset = CoordObfuscator.switchDimension(event.getUser(), dimensionName);
                    if (joinOffset != null && wrapper.getLastDeathPosition() != null) {
                        wrapper.setLastDeathPosition(CoordObfuscator.apply(wrapper.getLastDeathPosition(), joinOffset));
                    }
                }
                case RESPAWN -> {
                    WrapperPlayServerRespawn wrapper = new WrapperPlayServerRespawn(event);
                    java.util.Optional<String> worldName = wrapper.getWorldName();
                    FixedOffset respawnOffset = CoordObfuscator.switchDimension(event.getUser(), worldName.orElse(null));
                    if (respawnOffset != null && wrapper.getLastDeathPosition() != null) {
                        wrapper.setLastDeathPosition(CoordObfuscator.apply(wrapper.getLastDeathPosition(), respawnOffset));
                    }
                }
                case WORLD_BORDER_CENTER -> {
                    WrapperPlayServerWorldBorderCenter wrapper = new WrapperPlayServerWorldBorderCenter(event);
                    wrapper.setX(wrapper.getX() - offset.x());
                    wrapper.setZ(wrapper.getZ() - offset.z());
                }
                case INITIALIZE_WORLD_BORDER -> {
                    WrapperPlayServerInitializeWorldBorder wrapper = new WrapperPlayServerInitializeWorldBorder(event);
                    wrapper.setX(wrapper.getX() - offset.x());
                    wrapper.setZ(wrapper.getZ() - offset.z());
                }
                case SET_SLOT -> {
                    WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
                    CoordObfuscator.applyItem(wrapper.getItem(), offset);
                }
                case WINDOW_ITEMS -> {
                    WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
                    for (ItemStack item : wrapper.getItems()) {
                        CoordObfuscator.applyItem(item, offset);
                    }
                    wrapper.getCarriedItem().ifPresent(item -> CoordObfuscator.applyItem(item, offset));
                }
                case SET_PLAYER_INVENTORY -> {
                    WrapperPlayServerSetPlayerInventory wrapper = new WrapperPlayServerSetPlayerInventory(event);
                    CoordObfuscator.applyItem(wrapper.getStack(), offset);
                }
                case SET_CURSOR_ITEM -> {
                    WrapperPlayServerSetCursorItem wrapper = new WrapperPlayServerSetCursorItem(event);
                    CoordObfuscator.applyItem(wrapper.getStack(), offset);
                }
                case ENTITY_EQUIPMENT -> {
                    WrapperPlayServerEntityEquipment wrapper = new WrapperPlayServerEntityEquipment(event);
                    for (Equipment equipment : wrapper.getEquipment()) {
                        CoordObfuscator.applyItem(equipment.getItem(), offset);
                    }
                }
                case ENTITY_METADATA -> {
                    WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
                    CoordObfuscator.applyEntityMetadata(wrapper, offset);
                }
                case WAYPOINT -> {
                    WrapperPlayServerWaypoint wrapper = new WrapperPlayServerWaypoint(event);
                    TrackedWaypoint waypoint = wrapper.getWaypoint();
                    WaypointInfo info = waypoint.getInfo();
                    WaypointInfo newInfo = null;
                    if (info instanceof Vec3iWaypointInfo vec3i) {
                        newInfo = new Vec3iWaypointInfo(CoordObfuscator.apply(vec3i.getPosition(), offset));
                    } else if (info instanceof ChunkWaypointInfo chunk) {
                        newInfo = new ChunkWaypointInfo(offset.applyChunkX(chunk.getChunkX()),
                                offset.applyChunkZ(chunk.getChunkZ()));
                    }
                    if (newInfo != null) {
                        wrapper.setWaypoint(new TrackedWaypoint(waypoint.getIdentifier(), waypoint.getIcon(), newInfo));
                    }
                }
                default -> handled = false;
            }
        } else {
            handled = false;
        }
        if (handled) {
            event.markForReEncode(true);
        }
    }

    private void handleReceive(PacketReceiveEvent event, FixedOffset offset) {
        boolean handled = true;
        if (event.getPacketType() instanceof PacketType.Play.Client type) {
            switch (type) {
                case PLAYER_POSITION, PLAYER_POSITION_AND_ROTATION -> {
                    WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
                    Location location = wrapper.getLocation();
                    location.setPosition(CoordObfuscator.unapply(location.getPosition(), offset));
                    wrapper.setLocation(location);
                }
                case PLAYER_DIGGING -> {
                    WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
                    DiggingAction action = wrapper.getAction();
                    if (action != DiggingAction.DROP_ITEM_STACK
                            && action != DiggingAction.DROP_ITEM
                            && action != DiggingAction.RELEASE_USE_ITEM
                            && action != DiggingAction.SWAP_ITEM_WITH_OFFHAND) {
                        wrapper.setBlockPosition(CoordObfuscator.unapply(wrapper.getBlockPosition(), offset));
                    }
                }
                case PLAYER_BLOCK_PLACEMENT -> {
                    WrapperPlayClientPlayerBlockPlacement wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
                    wrapper.setBlockPosition(CoordObfuscator.unapply(wrapper.getBlockPosition(), offset));
                }
                case VEHICLE_MOVE -> {
                    WrapperPlayClientVehicleMove wrapper = new WrapperPlayClientVehicleMove(event);
                    wrapper.setPosition(CoordObfuscator.unapply(wrapper.getPosition(), offset));
                }
                case UPDATE_SIGN -> {
                    WrapperPlayClientUpdateSign wrapper = new WrapperPlayClientUpdateSign(event);
                    wrapper.setBlockPosition(CoordObfuscator.unapply(wrapper.getBlockPosition(), offset));
                }
                case CLICK_WINDOW -> {
                    WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
                    if (wrapper.getSlots().isPresent()) {
                        for (ItemStack item : wrapper.getSlots().get().values()) {
                            CoordObfuscator.unapplyItem(item, offset);
                        }
                    }
                    if (wrapper.getCarriedItemStack() != null) {
                        CoordObfuscator.unapplyItem(wrapper.getCarriedItemStack(), offset);
                    }
                }
                case CREATIVE_INVENTORY_ACTION -> {
                    WrapperPlayClientCreativeInventoryAction wrapper = new WrapperPlayClientCreativeInventoryAction(event);
                    CoordObfuscator.unapplyItem(wrapper.getItemStack(), offset);
                }
                case UPDATE_COMMAND_BLOCK -> {
                    WrapperPlayClientUpdateCommandBlock wrapper = new WrapperPlayClientUpdateCommandBlock(event);
                    wrapper.setPosition(CoordObfuscator.unapply(wrapper.getPosition(), offset));
                }
                case UPDATE_STRUCTURE_BLOCK -> {
                    WrapperPlayClientSetStructureBlock wrapper = new WrapperPlayClientSetStructureBlock(event);
                    wrapper.setPosition(CoordObfuscator.unapply(wrapper.getPosition(), offset));
                }
                case GENERATE_STRUCTURE -> {
                    WrapperPlayClientGenerateStructure wrapper = new WrapperPlayClientGenerateStructure(event);
                    wrapper.setBlockPosition(CoordObfuscator.unapply(wrapper.getBlockPosition(), offset));
                }
                case PICK_ITEM_FROM_BLOCK -> {
                    WrapperPlayClientPickItemFromBlock wrapper = new WrapperPlayClientPickItemFromBlock(event);
                    wrapper.setBlockPos(CoordObfuscator.unapply(wrapper.getBlockPos(), offset));
                }
                case UPDATE_JIGSAW_BLOCK -> {
                    WrapperPlayClientUpdateJigsawBlock wrapper = new WrapperPlayClientUpdateJigsawBlock(event);
                    wrapper.setPosition(CoordObfuscator.unapply(wrapper.getPosition(), offset));
                }
                default -> handled = false;
            }
        } else {
            handled = false;
        }
        if (handled) {
            event.markForReEncode(true);
        }
    }

    private boolean markVoxySend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLUGIN_MESSAGE) {
            WrapperPlayServerPluginMessage wrapper = new WrapperPlayServerPluginMessage(event);
            String channel = wrapper.getChannelName();
            boolean lss = isLssChannel(channel);
            if (lss) {
                CoordObfuscator.markVoxy(event.getUser());
                translateLssSend(event, wrapper, channel);
            }
            return lss;
        }
        if (event.getPacketType() == PacketType.Configuration.Server.PLUGIN_MESSAGE) {
            WrapperConfigServerPluginMessage wrapper = new WrapperConfigServerPluginMessage(event);
            if (isLssChannel(wrapper.getChannelName())) {
                CoordObfuscator.markVoxy(event.getUser());
                return true;
            }
        }
        return false;
    }

    private void translateLssSend(PacketSendEvent event, WrapperPlayServerPluginMessage wrapper, String channel) {
        FixedOffset off = CoordObfuscator.lssOffsetFor(event.getUser());
        if (off == null) {
            return;
        }
        int offX = off.chunkX();
        int offZ = off.chunkZ();
        byte[] data = wrapper.getData();
        byte[] translated = switch (channel) {
            case LSS_CHANNEL_COLUMN -> LssTranslator.translateColumn(data, offX, offZ);
            case LSS_CHANNEL_BATCH_RESPONSE -> LssTranslator.translateBatchResponse(data, offX, offZ);
            case LSS_CHANNEL_DIRTY_COLUMNS -> LssTranslator.translateDirtyColumns(data, offX, offZ);
            default -> null;
        };
        if (translated != null && translated != data) {
            wrapper.setData(translated);
            event.markForReEncode(true);
        }
    }

    private boolean markVoxyReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
            String channel = wrapper.getChannelName();
            boolean lss = isLssChannel(channel);
            boolean voxy = lss
                    || ("minecraft:register".equals(channel) && registerDataContainsLss(wrapper.getData()));
            if (voxy) {
                CoordObfuscator.markVoxy(event.getUser());
            }
            if (lss) {
                translateLssReceive(event, wrapper, channel);
            }
            return voxy;
        }
        if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage wrapper = new WrapperConfigClientPluginMessage(event);
            String channel = wrapper.getChannelName();
            boolean voxy = isLssChannel(channel)
                    || ("minecraft:register".equals(channel) && registerDataContainsLss(wrapper.getData()));
            if (voxy) {
                CoordObfuscator.markVoxy(event.getUser());
            }
            return voxy;
        }
        return false;
    }

    private void translateLssReceive(PacketReceiveEvent event, WrapperPlayClientPluginMessage wrapper, String channel) {
        if (!LSS_CHANNEL_REQUEST.equals(channel)) {
            return;
        }
        FixedOffset off = CoordObfuscator.lssOffsetFor(event.getUser());
        if (off == null) {
            return;
        }
        byte[] translated = LssTranslator.translateRequest(wrapper.getData(), off.chunkX(), off.chunkZ());
        if (translated != wrapper.getData()) {
            wrapper.setData(translated);
            event.markForReEncode(true);
        }
    }

    private static boolean isLssChannel(String channel) {
        return channel != null && channel.startsWith("lss:");
    }

    /**
     * Тело minecraft:register — список каналов, которые клиент хочет принимать:
     * VarInt count, затем count x (VarInt длина + UTF-8 строка канала).
     */
    private static boolean registerDataContainsLss(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }
        int[] countAndIndex = readVarInt(data, 0);
        if (countAndIndex == null) {
            return false;
        }
        int count = countAndIndex[0];
        int index = countAndIndex[1];
        for (int i = 0; i < count && i < 64; i++) {
            int[] lenAndIndex = readVarInt(data, index);
            if (lenAndIndex == null) {
                return false;
            }
            int len = lenAndIndex[0];
            index = lenAndIndex[1];
            if (len < 0 || index + len > data.length) {
                return false;
            }
            if (len >= 4 && data[index] == 'l' && data[index + 1] == 's'
                    && data[index + 2] == 's' && data[index + 3] == ':') {
                return true;
            }
            index += len;
        }
        return false;
    }

    private static int[] readVarInt(byte[] data, int index) {
        int value = 0;
        int shift = 0;
        while (index < data.length && shift <= 28) {
            byte b = data[index++];
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return new int[]{value, index};
            }
            shift += 7;
        }
        return null;
    }
}
