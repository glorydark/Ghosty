package net.easecation.ghosty.entity;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.AddPlayerPacket;
import net.easecation.ghosty.playback.PlayerPlaybackEngine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaybackNPC extends EntityHuman implements InventoryHolder {

    public static Skin defaultSkin;

    private final PlayerPlaybackEngine engine;
    private List<Player> watchers;
    private final Set<Player> hideFrom = new HashSet<>();

    public PlaybackNPC(FullChunk chunk, CompoundTag nbt, PlayerPlaybackEngine engine, Skin skin, String name, List<Player> watchers) {
        super(chunk, nbt);
        this.engine = engine;
        this.setSkin(skin == null ? defaultSkin : skin);
        this.setNameTagVisible(true);
        this.setNameTagAlwaysVisible(true);
        this.getInventory().setHeldItemSlot(0);
        this.setNameTag(name);
        this.watchers = watchers;
    }

    public PlaybackNPC(PlayerPlaybackEngine engine, Location pos, Skin skin, String name, List<Player> watchers){
        this(pos.getLevel().getChunk(pos.getFloorX() >> 4, pos.getFloorZ() >> 4),
                new CompoundTag()
                        .putList(new ListTag<DoubleTag>("Pos")
                                .add(new DoubleTag("", pos.x))
                                .add(new DoubleTag("", pos.y))
                                .add(new DoubleTag("", pos.z)))
                        .putList(new ListTag<DoubleTag>("Motion")
                                .add(new DoubleTag("", 0))
                                .add(new DoubleTag("", 0))
                                .add(new DoubleTag("", 0)))
                        .putList(new ListTag<FloatTag>("Rotation")
                                        .add(new FloatTag("", (float)pos.yaw))
                                        .add(new FloatTag("", (float)pos.pitch)))
                        .putCompound("Skin", (new CompoundTag())
                                        .putByteArray("Data", skin.getSkinData().data)
                                        .putString("ModelId", skin.getSkinId())
                        ), engine, skin, name, watchers);
        this.saveNBT();
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (source instanceof EntityDamageByEntityEvent event && event.getDamager() instanceof Player player) {
            if (this.engine.getInteractNPCCallback() != null) {
                this.engine.getInteractNPCCallback().accept(engine, player);
            }
        }
        return false;
    }

    @Override
    public boolean onInteract(Player player, Item item) {
        if (this.engine.getInteractNPCCallback() != null) {
            this.engine.getInteractNPCCallback().accept(engine, player);
        }
        return super.onInteract(player, item);
    }

    @Override
    public boolean onInteract(Player player, Item item, Vector3 clickedPos) {
        if (this.engine.getInteractNPCCallback() != null) {
            this.engine.getInteractNPCCallback().accept(engine, player);
        }
        return super.onInteract(player, item, clickedPos);
    }

    public void hideFrom(Player player) {
        this.hideFrom.add(player);
        this.despawnFrom(player);
    }

    public void removeHideFrom(Player player) {
        this.hideFrom.remove(player);
        this.spawnTo(player);
    }

    @Override
    public void spawnTo(Player player) {
        if (this.watchers != null && !this.watchers.contains(player)) return;
        if (this.hideFrom.contains(player)) return;
        if (!this.hasSpawned.containsKey(player.getLoaderId())) {
            this.hasSpawned.put(player.getLoaderId(), player);

            if (!this.skin.isValid()) {
                throw new IllegalStateException(this.getClass().getSimpleName() + " must have a valid skin set");
            }
            this.server.updatePlayerListData(this.getUniqueId(), this.getId(), this.getName(), this.skin, new Player[]{player});
            AddPlayerPacket pk = new AddPlayerPacket();
            pk.uuid = this.getUniqueId();
            pk.username = this.getNameTag();
            pk.entityUniqueId = this.getId();
            pk.entityRuntimeId = this.getId();
            pk.x = (float) this.x;
            pk.y = (float) this.y;
            pk.z = (float) this.z;
            pk.speedX = (float) this.motionX;
            pk.speedY = (float) this.motionY;
            pk.speedZ = (float) this.motionZ;
            pk.yaw = (float) this.yaw;
            //TODO check
            //pk.headYaw = (float) this.yaw;
            pk.pitch = (float) this.pitch;
            pk.item = this.getInventory().getItemInHand();
            pk.metadata = this.dataProperties;
            player.dataPacket(pk);
            this.inventory.sendArmorContents(player);
            this.getInventory().sendHeldItem(player);
        }
    }

    @Override
    public void despawnFrom(Player player) {
        super.despawnFrom(player);
        this.server.removePlayerListData(this.getUniqueId());
    }

    @Override
    public void kill() {
        if (this.getInventory() != null) this.getInventory().clearAll();
        super.kill();
    }

    public void resendPosition() {
        this.getLevel().addEntityMovement(this, this.x, this.y, this.z, this.yaw, this.pitch, this.yaw);
    }

    @Override
    public Skin getSkin() {
        return super.getSkin() == null ? defaultSkin : super.getSkin();
    }

    @Override
    public void close() {
        super.close();
    }

    public List<Player> getWatchers() {
        return watchers;
    }

    public void setWatchers(List<Player> watchers) {
        this.watchers = watchers;
    }
}
