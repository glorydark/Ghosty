package net.easecation.ghosty.recording.player.updated;

import cn.nukkit.inventory.PlayerOffhandInventory;
import cn.nukkit.item.Item;
import cn.nukkit.utils.BinaryStream;
import net.easecation.ghosty.GhostyPlugin;
import net.easecation.ghosty.entity.PlaybackNPC;
import net.easecation.ghosty.recording.player.PlayerRecordNode;

/**
 * Created by Mulan Lin('Snake1999') on 2016/11/19 17:02.
 * All rights reserved
 */
public class PlayerUpdatedOffhand implements PlayerUpdated {

    private Item item;

    public static PlayerUpdatedOffhand of(Item item) {
        return new PlayerUpdatedOffhand(item);
    }

    @Override
    public int getUpdateTypeId() {
        return PlayerUpdated.TYPE_OFFHAND;
    }

    @Override
    public boolean hasStates() {
        return true;
    }

    @Override
    public void processTo(PlaybackNPC ghost) {
        if (ghost != null && ghost.getOffhandInventory() != null) {
            PlayerOffhandInventory inv = ghost.getOffhandInventory();
            inv.setItem(0, item == null ? Item.get(Item.AIR) : item);
            inv.sendContents(ghost.getViewers().values());
        }
    }

    @Override
    public PlayerRecordNode applyTo(PlayerRecordNode node) {
        node.setOffhand(item);
        return node;
    }

    public PlayerUpdatedOffhand(BinaryStream stream) {
        read(stream);
    }

    private PlayerUpdatedOffhand(Item item) {
        this.item = item;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PlayerUpdatedOffhand o)) return false;
        return (item.equals(o.item));
    }

    @Override
    public void write(BinaryStream stream) {
        stream.putSlot(GhostyPlugin.DATA_SAVE_PROTOCOL, this.item);
    }

    @Override
    public void read(BinaryStream stream) {
        this.item = stream.getSlot(GhostyPlugin.DATA_SAVE_PROTOCOL);
    }

    @Override
    public String toString() {
        return "PlayerUpdatedOffhand{" +
            "item=" + item +
            '}';
    }
}
