package agh.bozon;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

public class SkyMapTime {
    private boolean useSystemTime = true;
    private LocalDateTime selectedDateTime = LocalDateTime.now();

    public boolean isUseSystemTime() {
        return useSystemTime;
    }

    public void setUseSystemTime(boolean useSystemTime) {
        this.useSystemTime = useSystemTime;
    }

    public LocalDateTime getSelectedDateTime() {
        return selectedDateTime;
    }

    public void setSelectedDateTime(LocalDateTime selectedDateTime) {
        this.selectedDateTime = Objects.requireNonNull(selectedDateTime, "selectedDateTime");
    }

    public Date resolveDate() {
        if (useSystemTime) {
            return new Date();
        }
        return Date.from(selectedDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
