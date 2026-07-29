package com.dragonaltar.soul;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DragonSoul {
    private final String id;
    private DragonSoulState state;
    private UUID holder;
    private UUID reservedFor;
    private final Instant createdAt;
    private Instant claimedAt;
    private int generation;
    private int transferCount;
    private final List<String> lineage;

    public DragonSoul(String id, DragonSoulState state, UUID holder, UUID reservedFor, Instant createdAt,
                      Instant claimedAt, int generation, int transferCount, List<String> lineage) {
        if (!SoulIdentity.CANONICAL_IDS.contains(id)) throw new IllegalArgumentException("Invalid soul id");
        this.id = id; this.state = Objects.requireNonNull(state); this.holder = holder; this.reservedFor = reservedFor;
        this.createdAt = Objects.requireNonNull(createdAt); this.claimedAt = claimedAt;
        this.generation = generation; this.transferCount = transferCount; this.lineage = new ArrayList<>(lineage);
    }

    public static DragonSoul unclaimed(String id) {
        return new DragonSoul(id, DragonSoulState.UNCLAIMED, null, null, Instant.now(), null, 0, 0, List.of());
    }
    public String id() { return id; }
    public DragonSoulState state() { return state; }
    public UUID holder() { return holder; }
    public UUID reservedFor() { return reservedFor; }
    public Instant createdAt() { return createdAt; }
    public Instant claimedAt() { return claimedAt; }
    public int generation() { return generation; }
    public int transferCount() { return transferCount; }
    public List<String> lineage() { return List.copyOf(lineage); }
    void reserve(UUID player) {
        if (state != DragonSoulState.UNCLAIMED) throw new IllegalStateException("Soul unavailable");
        state = DragonSoulState.RITUAL_RESERVED; reservedFor = player;
    }
    void release() {
        if (state != DragonSoulState.RITUAL_RESERVED) throw new IllegalStateException("Soul not reserved");
        state = DragonSoulState.UNCLAIMED; reservedFor = null;
    }
    void assign(UUID player, String reason) {
        if (player == null) throw new IllegalArgumentException("holder");
        UUID old = holder; holder = player; reservedFor = null; state = DragonSoulState.HELD;
        if (claimedAt == null) claimedAt = Instant.now();
        if (old != null && !old.equals(player)) { generation++; transferCount++; }
        lineage.add(Instant.now() + "|" + (old == null ? "-" : old) + "|" + player + "|" + reason);
    }
    void pending(String reason) {
        UUID old = holder; holder = null; reservedFor = null; state = DragonSoulState.TRANSFER_PENDING;
        lineage.add(Instant.now() + "|" + (old == null ? "-" : old) + "|PENDING|" + reason);
    }
    void makeUnclaimed(String reason) {
        UUID old=holder;holder=null;reservedFor=null;state=DragonSoulState.UNCLAIMED;
        lineage.add(Instant.now()+"|"+(old==null?"-":old)+"|UNCLAIMED|"+reason);
    }
    void fracture(String reason) {
        UUID old=holder;holder=null;reservedFor=null;state=DragonSoulState.FRACTURED;
        lineage.add(Instant.now()+"|"+(old==null?"-":old)+"|FRACTURED|"+reason);
    }
    void limbo(String reason) {
        UUID old=holder;holder=null;reservedFor=null;state=DragonSoulState.MOTHER_SOUL_LIMBO;
        lineage.add(Instant.now()+"|"+(old==null?"-":old)+"|MOTHER_SOUL_LIMBO|"+reason);
    }
    void disable(String reason){UUID old=holder;holder=null;reservedFor=null;state=DragonSoulState.DISABLED;lineage.add(Instant.now()+"|"+(old==null?"-":old)+"|DISABLED|"+reason);}
    boolean repair(){
        boolean changed=false;
        if(state==DragonSoulState.TRANSFER_ANIMATING){if(holder==null)pending("STARTUP_INTERRUPTED_TRANSFER");else{state=DragonSoulState.HELD;reservedFor=null;lineage.add(Instant.now()+"|"+holder+"|"+holder+"|STARTUP_TRANSFER_FINALIZED");}changed=true;}
        if((state==DragonSoulState.HELD||state==DragonSoulState.ADMIN_HELD)&&holder==null){pending("STARTUP_MISSING_HOLDER");changed=true;}
        if(state==DragonSoulState.RITUAL_RESERVED&&reservedFor==null){state=DragonSoulState.UNCLAIMED;lineage.add(Instant.now()+"|-|UNCLAIMED|STARTUP_MISSING_RESERVATION");changed=true;}
        if(holder!=null&&state!=DragonSoulState.HELD&&state!=DragonSoulState.ADMIN_HELD){state=DragonSoulState.HELD;reservedFor=null;lineage.add(Instant.now()+"|"+holder+"|"+holder+"|STARTUP_STATE_REPAIR");changed=true;}
        if(state!=DragonSoulState.RITUAL_RESERVED&&reservedFor!=null){reservedFor=null;lineage.add(Instant.now()+"|-|-|STARTUP_STALE_RESERVATION_CLEARED");changed=true;}
        return changed;
    }
    void clearLineage(){lineage.clear();}
}
