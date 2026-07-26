package nightfallmods.lifesteal.screen;

import java.util.Comparator;
import java.util.List;

public enum ReviveSort {
    EARLIEST_BANNED("Earliest Banned", Comparator.comparingLong(PlayerCollector.Revivable::since)),
    LATEST_BANNED("Latest Banned", Comparator.comparingLong(PlayerCollector.Revivable::since).reversed()),
    A_TO_Z("A → Z", Comparator.comparing(r -> r.name().toLowerCase()));

    private final String label;
    private final Comparator<PlayerCollector.Revivable> comparator;

    ReviveSort(String label, Comparator<PlayerCollector.Revivable> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public String label() { return label; }

    public ReviveSort next() {
        ReviveSort[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public void apply(List<PlayerCollector.Revivable> list) {
        list.sort(comparator);
    }
}

