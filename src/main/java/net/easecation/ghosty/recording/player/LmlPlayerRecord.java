package net.easecation.ghosty.recording.player;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.*;
import net.easecation.ghosty.GhostyPlugin;
import net.easecation.ghosty.MathUtil;
import net.easecation.ghosty.PlaybackIterator;
import net.easecation.ghosty.recording.player.updated.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by Mulan Lin('Snake1999') on 2016/11/19 15:34.
 */
public class LmlPlayerRecord implements PlayerRecord {

    private PlayerRecordNode last = PlayerRecordNode.ZERO;

    private List<RecordPair> rec = new LinkedList<>();

    private String playerName;
    private Skin skin;

    public LmlPlayerRecord(BinaryStream stream) {
        this.playerName = stream.getString();
        int offset = stream.getOffset();
        try {
            this.skin = getSkin(stream, GhostyPlugin.DATA_SAVE_PROTOCOL);
        } catch (IllegalArgumentException e) {
            stream.setOffset(offset);
            this.skin = getSkin(stream, GhostyPlugin.DATA_SAVE_PROTOCOL);
        }
        int len = (int) stream.getUnsignedVarInt();
        for (int i = 0; i < len; i++) {
            RecordPair pair = new RecordPair(stream);
            rec.add(pair);
        }
    }

    public Skin getSkin(BinaryStream binaryStream, int protocol) {
        Skin skin = new Skin();
        skin.setSkinId(binaryStream.getString());
        if (protocol >= 428) {
            skin.setPlayFabId(binaryStream.getString());
        }

        skin.setSkinResourcePatch(binaryStream.getString());
        skin.setSkinData(binaryStream.getImage(1048576));
        int animationCount = binaryStream.getLInt();

        int piecesLength;
        int i;
        for(piecesLength = 0; piecesLength < Math.min(animationCount, 1024); ++piecesLength) {
            SerializedImage image = binaryStream.getImage(1048576);
            i = binaryStream.getLInt();
            float frames = binaryStream.getLFloat();
            int expression = protocol >= 419 ? binaryStream.getLInt() : 0;
            skin.getAnimations().add(new SkinAnimation(image, i, frames, expression));
        }

        skin.setCapeData(binaryStream.getImage(8192));
        skin.setGeometryData(binaryStream.getString());
        if (protocol >= 465) {
            skin.setGeometryDataEngineVersion(binaryStream.getString());
        }

        skin.setAnimationData(binaryStream.getString());
        if (protocol < 465) {
            skin.setPremium(binaryStream.getBoolean());
            skin.setPersona(binaryStream.getBoolean());
            skin.setCapeOnClassic(binaryStream.getBoolean());
        }

        skin.setCapeId(binaryStream.getString());
        skin.setFullSkinId(binaryStream.getString());
        if (protocol >= 390) {
            skin.setArmSize(binaryStream.getString());
            skin.setSkinColor(binaryStream.getString());
            piecesLength = binaryStream.getLInt();

            int tintsLength;
            String pieceType;
            for(tintsLength = 0; tintsLength < Math.min(piecesLength, 1024); ++tintsLength) {
                String pieceId = binaryStream.getString();
                pieceType = binaryStream.getString();
                String packId = binaryStream.getString();
                boolean isDefault = binaryStream.getBoolean();
                String productId = binaryStream.getString();
                skin.getPersonaPieces().add(new PersonaPiece(pieceId, pieceType, packId, isDefault, productId));
            }

            tintsLength = binaryStream.getLInt();

            for(i = 0; i < Math.min(tintsLength, 1024); ++i) {
                pieceType = binaryStream.getString();
                List<String> colors = new ArrayList();
                int colorsLength = binaryStream.getLInt();

                for(int i2 = 0; i2 < Math.min(colorsLength, 1024); ++i2) {
                    colors.add(binaryStream.getString());
                }

                skin.getTintColors().add(new PersonaPieceTint(pieceType, colors));
            }

            if (protocol >= 465) {
                skin.setPremium(binaryStream.getBoolean());
                skin.setPersona(binaryStream.getBoolean());
                skin.setCapeOnClassic(binaryStream.getBoolean());
                skin.setPrimaryUser(binaryStream.getBoolean());
                if (protocol >= 568) {
                    skin.setOverridingPlayerAppearance(binaryStream.getBoolean());
                }
            }
        }

        return skin;
    }

    public LmlPlayerRecord(Player player) {
        this.skin = player.getSkin();
        this.playerName = player.getName();
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void record(int tick, PlayerRecordNode node) {
        double lx = last.getX(), x = node.getX();
        double ly = last.getY(), y = node.getY();
        double lz = last.getZ(), z = node.getZ();
        if (lx != x || ly != y || lz != z)
            push(tick, PlayerUpdatedPositionXYZ.of(x, y, z));
        double la = last.getYaw(), a = node.getYaw();
        double lp = last.getPitch(), p = node.getPitch();
        if(la != a || lp != p)
            push(tick, PlayerUpdatedRotation.of(a, p));
        String ln = last.getTagName(), n = node.getTagName();
        if(!Objects.equals(ln, n))
            push(tick, PlayerUpdatedTagName.of(n));
        String lw = last.getLevel(), w = node.getLevel();
        if(!Objects.equals(lw, w))
            push(tick, PlayerUpdatedWorldChanged.of(w));
        Item li = last.getItem(), i = node.getItem();
        if(!Objects.equals(li, i))
            push(tick, PlayerUpdatedItem.of(i));
        long lastFlags = last.getDataFlags(), flags = node.getDataFlags();
        if(lastFlags != flags)
            push(tick, PlayerUpdatedDataFlags.of(flags));
        last = node;
    }

    private void push(int tick, PlayerUpdated updated) {
        rec.add(new RecordPair(tick, updated));
    }

    private class RecordPair {

        private RecordPair(BinaryStream stream) {
            try {
                this.tick = (int) stream.getUnsignedVarInt();
                this.updated = PlayerUpdated.fromBinaryStream(stream);
            } catch (Exception e) {
                Server.getInstance().getLogger().logException(e);
                throw e;
            }
        }

        private RecordPair(int tick, PlayerUpdated updated) {
            this.tick = tick;
            this.updated = updated;
        }

        int tick; PlayerUpdated updated;

        private void write(BinaryStream stream) {
            stream.putUnsignedVarInt((int) tick);
            stream.putByte((byte) updated.getUpdateTypeId());
            updated.write(stream);
        }
    }

    @Override
    public PlaybackIterator<PlayerUpdated> iterator() {
        PlaybackIterator<PlayerUpdated> iterator = new PlaybackIterator<>();
        rec.forEach((e) -> iterator.insert(e.tick, e.updated));
        return iterator;
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    @Override
    public byte[] toBinary() {
        BinaryStream stream = new BinaryStream();
        stream.putByte(PlayerRecord.OBJECT_LML);
        stream.putString(this.playerName);
        stream.putSkin(GhostyPlugin.DATA_SAVE_PROTOCOL, this.skin);
        stream.putUnsignedVarInt(this.rec.size());
        for (RecordPair pair : this.rec) {
            pair.write(stream);
        }
        return stream.getBuffer();
    }

    public double getMaxMovement() {
        Vector3 lastPos = null;
        double maxMovement = 0;
        for (RecordPair pair : this.rec.stream().filter(p -> p.updated instanceof PlayerUpdatedPositionXYZ).collect(Collectors.toList())) {
            PlayerUpdatedPositionXYZ pos = (PlayerUpdatedPositionXYZ) pair.updated;
            Vector3 newPos = pos.asVector3();
            if (lastPos != null) {
                double distance = newPos.distance(lastPos);
                if (distance > maxMovement) maxMovement = distance;
            }
            lastPos = newPos;
        }
        return maxMovement;
    }

    public double calculateMovementVariance() {
        Vector3 lastPos = null;

        List<RecordPair> pairs = this.rec.stream().filter(p -> p.updated instanceof PlayerUpdatedPositionXYZ).collect(Collectors.toList());
        if (pairs.size() <= 1) return 0;
        double[] distances = new double[pairs.size() - 1];
        for (int i = 0; i < pairs.size(); i++) {
            RecordPair pair = pairs.get(i);
            PlayerUpdatedPositionXYZ pos = (PlayerUpdatedPositionXYZ) pair.updated;
            Vector3 newPos = pos.asVector3();
            if (lastPos != null) {
                distances[i - 1] = newPos.distance(lastPos);
            }
            lastPos = newPos;
        }
        return MathUtil.getVariance(distances);
    }
}
