package agh.bozon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Test;

public class AppTest {
    @Test
    public void shouldUseSelectedDateWhenSystemTimeIsDisabled() {
        SkyMapTime skyMapTime = new SkyMapTime();
        LocalDateTime selected = LocalDateTime.of(2025, 1, 15, 18, 30);

        skyMapTime.setUseSystemTime(false);
        skyMapTime.setSelectedDateTime(selected);

        Date resolved = skyMapTime.resolveDate();
        LocalDateTime actual = LocalDateTime.ofInstant(resolved.toInstant(), ZoneId.systemDefault());

        assertFalse(skyMapTime.isUseSystemTime());
        assertEquals(selected, actual);
    }

    @Test
    public void shouldUseCurrentTimeByDefault() {
        SkyMapTime skyMapTime = new SkyMapTime();

        assertTrue(skyMapTime.isUseSystemTime());
        assertTrue(skyMapTime.resolveDate() instanceof Date);
    }
}
