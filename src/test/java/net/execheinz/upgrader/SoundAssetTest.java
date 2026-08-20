package net.execheinz.upgrader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

final class SoundAssetTest {
    private static final String[] SOUNDS = {
        "ui_click", "key_tap", "item_select", "bet_confirm",
        "wheel_start", "wheel_tick", "upgrade_success", "upgrade_failure"
    };

    @Test
    void everyRegisteredSoundIsARealOggAsset() throws IOException {
        for (String sound : SOUNDS) {
            String path = "/assets/upgrader/sounds/" + sound + ".ogg";
            try (InputStream stream = SoundAssetTest.class.getResourceAsStream(path)) {
                assertNotNull(stream, path);
                byte[] header = stream.readNBytes(4);
                assertArrayEquals(new byte[]{'O', 'g', 'g', 'S'}, header, path);
                assertTrue(stream.readAllBytes().length > 1000, path);
            }
        }
    }
}
