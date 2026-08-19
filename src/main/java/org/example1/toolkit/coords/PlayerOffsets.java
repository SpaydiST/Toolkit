package org.example1.toolkit.coords;

import org.bukkit.World;
import org.bukkit.entity.Player;

public final class PlayerOffsets {

    private final FixedOffset overworld;
    private final FixedOffset nether;
    private final FixedOffset end;

    private volatile FixedOffset current;
    private volatile boolean bypass;
    private volatile boolean voxy;

    public PlayerOffsets(FixedOffset base) {
        this.overworld = base;
        this.nether = base.scaled(8);
        this.end = base;
        this.current = base;
    }

    public FixedOffset current() {
        return current;
    }

    public boolean bypass() {
        return bypass;
    }

    public boolean voxy() {
        return voxy;
    }

    public void setVoxy(boolean voxy) {
        this.voxy = voxy;
    }

    public boolean isCurrentZero() {
        return current == null || current.isZero();
    }

    public void setEnvironment(World.Environment env) {
        this.current = switch (env) {
            case NETHER -> nether;
            case THE_END -> end;
            case NORMAL -> overworld;
            case CUSTOM -> overworld;
        };
    }

    public void update(Player player, String bypassPermission) {
        World world = player.getWorld();
        World.Environment env = world == null ? World.Environment.NORMAL : world.getEnvironment();
        setEnvironment(env);
        this.bypass = bypassPermission != null
                && !bypassPermission.isEmpty()
                && player.isPermissionSet(bypassPermission);
    }
}
