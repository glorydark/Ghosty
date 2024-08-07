package net.easecation.ghosty.recording.level;

import cn.nukkit.utils.BinaryStream;
import net.easecation.ghosty.PlaybackIterator;
import net.easecation.ghosty.recording.level.updated.LevelUpdated;

public interface LevelRecord {

    byte VERSION_0 = 0;

    void record(int tick, LevelRecordNode node);

    PlaybackIterator<LevelUpdated> iterator();

    byte[] toBinary();

    static LevelRecord fromBinary(byte[] data) {
        BinaryStream stream = new BinaryStream(data);
        byte type = (byte) stream.getByte();
        return new LevelRecordImpl(stream);
    }

}
