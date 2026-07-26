package nightfallmods.lifesteal.screen;

import nightfallmods.lifesteal.manager.EliminatedPlayersTracker;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public class PlayerCollector {

    public record Revivable(String name, boolean banned, long since) {}

    private final MinecraftServer server;
    private final List<Revivable> revivables = new ArrayList<>();

    public PlayerCollector(MinecraftServer server) {
        this.server = server;
    }

    public void collectPlayers() {
        revivables.clear();

        for (EliminatedPlayersTracker.Entry entry : EliminatedPlayersTracker.getEntries()) {
            revivables.add(new Revivable(entry.name(), entry.banned(), entry.eliminatedAt()));
        }
    }

    public List<Revivable> getRevivables()  { return revivables; }
    public boolean isBanned(String name) {
        return revivables.stream().anyMatch(r -> r.banned() && r.name().equals(name));
    }
    public int getTotalRevivableCount()     { return revivables.size(); }
}

