package pro.gravit.launcher.gui.service;

import pro.gravit.launcher.core.backend.LauncherBackendAPI;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PingService {
    private final Map<UUID, CompletableFuture<PingServerReport>> reports = new ConcurrentHashMap<>();

    public CompletableFuture<PingServerReport> getPingReport(UUID serverName) {
        CompletableFuture<PingServerReport> report = reports.computeIfAbsent(serverName,
                                                                             k -> new CompletableFuture<>());
        return report;
    }

    public void addReports(Map<UUID, PingServerReport> map) {
        map.forEach((k, v) -> {
            CompletableFuture<PingServerReport> report = getPingReport(k);
            report.complete(v);
        });
    }

    public void addReport(UUID name, LauncherBackendAPI.ServerPingInfo result) {
        CompletableFuture<PingServerReport> report = getPingReport(name);
        PingServerReport value = new PingServerReport(name, result.getMaxOnline(), result.getOnline());
        report.complete(value);
    }

    public void clear() {
        reports.forEach((k, v) -> {
            if (!v.isDone()) {
                v.completeExceptionally(new InterruptedException());
            }
        });
        reports.clear();
    }

    public static class PingServerReport {
        public final UUID name;
        public final int maxPlayers;
        public final int playersOnline;

        public PingServerReport(UUID name, int maxPlayers, int playersOnline) {
            this.name = name;
            this.maxPlayers = maxPlayers;
            this.playersOnline = playersOnline;
        }
    }
}
