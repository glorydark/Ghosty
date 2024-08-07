package net.easecation.ghosty.recording.level.updated;

import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3f;
import cn.nukkit.network.protocol.LevelEventPacket;
import cn.nukkit.utils.BinaryStream;
import net.easecation.ghosty.recording.level.LevelRecordNode;

public class LevelUpdatedLevelEvent implements LevelUpdated {

    private int evid;
    private float x = 0;
    private float y = 0;
    private float z = 0;
    private int data = 0;

    private LevelUpdatedLevelEvent(int evid, float x, float y, float z, int data) {
        this.evid = evid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.data = data;
    }

    public static LevelUpdatedLevelEvent of(LevelEventPacket packet) {
        return new LevelUpdatedLevelEvent(packet.evid, packet.x, packet.y, packet.z, packet.data);
    }

    public LevelUpdatedLevelEvent(BinaryStream stream) {
        this.read(stream);
    }

    @Override
    public int getUpdateTypeId() {
        return LevelUpdated.TYPE_LEVEL_EVENT;
    }

    @Override
    public void processTo(LevelRecordNode node) {
        LevelEventPacket packet = new LevelEventPacket();
        packet.evid = this.evid;
        packet.x = this.x;
        packet.y = this.y;
        packet.z = this.z;
        packet.data = this.data;
        node.handleLevelChunkPacket(Level.chunkHash((int) x >> 4, (int) z >> 4), packet);
    }

    public void backwardTo(LevelRecordNode node) {
        // 不需要做任何事
    }

    @Override
    public void write(BinaryStream stream) {
        stream.putVarInt(evid);
        stream.putVector3f(x, y, z);
        stream.putVarInt(data);
    }

    @Override
    public void read(BinaryStream stream) {
        this.evid = stream.getVarInt();
        Vector3f vector3f = stream.getVector3f();
        this.x = vector3f.x;
        this.y = vector3f.y;
        this.z = vector3f.z;
        this.data = stream.getVarInt();
    }

    @Override
    public String toString() {
        return "LevelUpdatedLevelEvent{" +
            "evid=" + evid +
            ", x=" + x +
            ", y=" + y +
            ", z=" + z +
            ", data=" + data +
            '}';
    }
}
