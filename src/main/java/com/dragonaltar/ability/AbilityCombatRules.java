package com.dragonaltar.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Stateful combat rules kept independent from Bukkit so their caps and one-use
 * guarantees can be tested without a running server.
 */
public final class AbilityCombatRules {
    private AbilityCombatRules() {
    }

    public record Brittle(UUID caster, long expiresAtMillis) {
    }

    public static final class BrittleTracker {
        private final Map<UUID, Brittle> targets = new HashMap<>();

        public void apply(UUID target, UUID caster, long expiresAtMillis) {
            targets.put(target, new Brittle(caster, expiresAtMillis));
        }

        public Optional<Brittle> consume(UUID target, long nowMillis, double attackDamage) {
            Brittle brittle = targets.get(target);
            if (brittle == null) {
                return Optional.empty();
            }
            if (brittle.expiresAtMillis() <= nowMillis) {
                targets.remove(target);
                return Optional.empty();
            }
            if (attackDamage <= 0) {
                return Optional.empty();
            }
            targets.remove(target);
            return Optional.of(brittle);
        }

        public boolean active(UUID target, long nowMillis) {
            Brittle brittle = targets.get(target);
            if (brittle == null) {
                return false;
            }
            if (brittle.expiresAtMillis() <= nowMillis) {
                targets.remove(target);
                return false;
            }
            return true;
        }

        public int countFor(UUID caster, long nowMillis) {
            prune(nowMillis);
            return (int) targets.values().stream().filter(value -> value.caster().equals(caster)).count();
        }

        public void extend(UUID target, long nowMillis, long extensionMillis, long maximumExpiryMillis) {
            Brittle brittle = targets.get(target);
            if (brittle == null || brittle.expiresAtMillis() <= nowMillis) {
                targets.remove(target);
                return;
            }
            long extended = Math.min(maximumExpiryMillis, brittle.expiresAtMillis() + Math.max(0, extensionMillis));
            targets.put(target, new Brittle(brittle.caster(), extended));
        }

        public void clear() {
            targets.clear();
        }

        private void prune(long nowMillis) {
            targets.values().removeIf(value -> value.expiresAtMillis() <= nowMillis);
        }
    }

    public enum CombatDamageSource {
        DIRECT_PLAYER,
        PLAYER_PROJECTILE,
        FIRE_TICK,
        SCRIPTED_ABILITY,
        REFLECTION,
        OTHER
    }

    public record HeatGain(int heat, boolean granted, boolean finisherArmed) {
    }

    public record RampageGain(int progress, boolean granted, boolean maximumReached) {
    }

    /**
     * Rev's hunt state. All time values are absolute milliseconds so the
     * lifecycle and cap rules remain deterministic and independent of Bukkit.
     */
    public static final class RevHuntTracker {
        private static final class HunterState {
            int heat;
            long lastAttackAt;
            long lastDecayAt;
            boolean finisherArmed;
            long finisherEndsAt;
            long huntEndsAt;
            long mobilityEndsAt;
            long mobilityCapAt;
            int rampage;
            long recastEndsAt;
            final Map<UUID, Long> heatReadyAt = new HashMap<>();
            final Map<UUID, Integer> heatGains = new HashMap<>();
            final Map<UUID, Integer> rampageGains = new HashMap<>();
        }

        private record Mark(UUID caster, long expiresAtMillis, long maximumExpiresAtMillis) {
        }

        private final Map<UUID, HunterState> hunters = new HashMap<>();
        private final Map<UUID, Mark> marks = new HashMap<>();

        public static boolean acceptsDamage(CombatDamageSource source) {
            return source == CombatDamageSource.DIRECT_PLAYER || source == CombatDamageSource.PLAYER_PROJECTILE;
        }

        public HeatGain gainHeat(UUID caster, UUID target, long nowMillis, int amount, int maximum,
                                 long perTargetCooldownMillis, int maximumGainsPerTarget) {
            HunterState state = hunters.computeIfAbsent(caster, ignored -> new HunterState());
            int cap = Math.max(0, maximum);
            if (amount <= 0 || cap == 0 || maximumGainsPerTarget <= 0
                    || state.heatReadyAt.getOrDefault(target, 0L) > nowMillis
                    || state.heatGains.getOrDefault(target, 0) >= maximumGainsPerTarget) {
                return new HeatGain(state.heat, false, state.finisherArmed);
            }
            int previous = state.heat;
            state.heat = Math.min(cap, state.heat + amount);
            state.lastAttackAt = nowMillis;
            state.lastDecayAt = nowMillis;
            state.heatReadyAt.put(target, nowMillis + Math.max(0, perTargetCooldownMillis));
            state.heatGains.merge(target, 1, Integer::sum);
            if (previous < cap && state.heat >= cap) {
                state.finisherArmed = true;
            }
            return new HeatGain(state.heat, state.heat > previous, state.finisherArmed);
        }

        public int decayHeat(UUID caster, long nowMillis, long delayMillis, long intervalMillis, int amount) {
            HunterState state = hunters.get(caster);
            if (state == null || state.heat <= 0 || amount <= 0 || intervalMillis <= 0) {
                return state == null ? 0 : state.heat;
            }
            long decayStart = Math.max(state.lastAttackAt + Math.max(0, delayMillis), state.lastDecayAt);
            if (nowMillis < decayStart + intervalMillis) {
                return state.heat;
            }
            long intervals = (nowMillis - decayStart) / intervalMillis;
            state.heat = Math.max(0, state.heat - (int) Math.min(Integer.MAX_VALUE, intervals * amount));
            state.lastDecayAt = decayStart + intervals * intervalMillis;
            if (state.heat == 0 && !huntActive(caster, nowMillis)) {
                state.finisherArmed = false;
            }
            return state.heat;
        }

        public boolean mark(UUID target, UUID caster, long nowMillis, long durationMillis,
                            long maximumRemainingMillis, int maximumMarks) {
            prune(nowMillis);
            Mark existing = marks.get(target);
            long expiry = nowMillis + Math.max(0, durationMillis);
            long cap = nowMillis + Math.max(0, maximumRemainingMillis);
            if (existing != null && existing.caster().equals(caster)) {
                marks.put(target, new Mark(caster, Math.min(existing.maximumExpiresAtMillis(),
                        Math.max(existing.expiresAtMillis(), expiry)), existing.maximumExpiresAtMillis()));
                return true;
            }
            long count = marks.values().stream().filter(mark -> mark.caster().equals(caster)).count();
            if (maximumMarks <= 0 || count >= maximumMarks) {
                return false;
            }
            marks.put(target, new Mark(caster, Math.min(expiry, cap), cap));
            return true;
        }

        public boolean markedBy(UUID target, UUID caster, long nowMillis) {
            Mark mark = marks.get(target);
            if (mark == null || mark.expiresAtMillis() <= nowMillis) {
                marks.remove(target);
                return false;
            }
            return mark.caster().equals(caster);
        }

        public boolean marked(UUID target, long nowMillis) {
            Mark mark = marks.get(target);
            if (mark == null || mark.expiresAtMillis() <= nowMillis) {
                marks.remove(target);
                return false;
            }
            return true;
        }

        public int activeMarks(UUID caster, long nowMillis) {
            prune(nowMillis);
            return (int) marks.values().stream().filter(mark -> mark.caster().equals(caster)).count();
        }

        public void armRecast(UUID caster, long expiresAtMillis) {
            hunters.computeIfAbsent(caster, ignored -> new HunterState()).recastEndsAt = expiresAtMillis;
        }

        public boolean recastAvailable(UUID caster, long nowMillis) {
            HunterState state = hunters.get(caster);
            if (state == null || state.recastEndsAt <= nowMillis) {
                if (state != null) state.recastEndsAt = 0;
                return false;
            }
            return true;
        }

        public boolean consumeRecast(UUID caster, long nowMillis) {
            if (!recastAvailable(caster, nowMillis)) {
                return false;
            }
            hunters.get(caster).recastEndsAt = 0;
            return true;
        }

        public long recastEndsAt(UUID caster) {
            HunterState state = hunters.get(caster);
            return state == null ? 0 : state.recastEndsAt;
        }

        public void beginHunt(UUID caster, long nowMillis, long huntDurationMillis,
                              long mobilityDurationMillis, long maximumMobilityDurationMillis) {
            HunterState state = hunters.computeIfAbsent(caster, ignored -> new HunterState());
            state.huntEndsAt = nowMillis + Math.max(0, huntDurationMillis);
            state.mobilityEndsAt = nowMillis + Math.max(0, mobilityDurationMillis);
            state.mobilityCapAt = nowMillis + Math.max(mobilityDurationMillis, maximumMobilityDurationMillis);
            state.rampage = 0;
            state.rampageGains.clear();
        }

        public RampageGain gainRampage(UUID caster, UUID target, long nowMillis, int amount,
                                       int maximum, int perTargetLimit) {
            HunterState state = hunters.get(caster);
            if (state == null || !huntActive(caster, nowMillis) || amount <= 0 || maximum <= 0
                    || perTargetLimit <= 0 || state.rampageGains.getOrDefault(target, 0) >= perTargetLimit) {
                return new RampageGain(state == null ? 0 : state.rampage, false,
                        state != null && state.rampage >= maximum);
            }
            int previous = state.rampage;
            state.rampage = Math.min(maximum, state.rampage + amount);
            state.rampageGains.merge(target, 1, Integer::sum);
            if (previous < maximum && state.rampage >= maximum) {
                state.finisherArmed = true;
            }
            return new RampageGain(state.rampage, state.rampage > previous, state.rampage >= maximum);
        }

        public long extendMobility(UUID caster, long nowMillis, long extensionMillis) {
            HunterState state = hunters.get(caster);
            if (state == null || !huntActive(caster, nowMillis)) {
                return 0;
            }
            long current = Math.max(nowMillis, state.mobilityEndsAt);
            state.mobilityEndsAt = Math.min(state.mobilityCapAt, current + Math.max(0, extensionMillis));
            return state.mobilityEndsAt;
        }

        public boolean huntActive(UUID caster, long nowMillis) {
            HunterState state = hunters.get(caster);
            return state != null && state.huntEndsAt > nowMillis;
        }

        public long huntEndsAt(UUID caster) {
            HunterState state = hunters.get(caster);
            return state == null ? 0 : state.huntEndsAt;
        }

        public int heat(UUID caster) {
            HunterState state = hunters.get(caster);
            return state == null ? 0 : state.heat;
        }

        public int rampage(UUID caster) {
            HunterState state = hunters.get(caster);
            return state == null ? 0 : state.rampage;
        }

        public boolean finisherArmed(UUID caster) {
            HunterState state = hunters.get(caster);
            return state != null && state.finisherArmed;
        }

        public boolean finisherArmed(UUID caster, long nowMillis) {
            HunterState state = hunters.get(caster);
            if (state == null) return false;
            if (state.finisherArmed && state.finisherEndsAt > 0 && state.finisherEndsAt <= nowMillis) {
                state.finisherArmed = false;
                state.finisherEndsAt = 0;
            }
            return state.finisherArmed;
        }

        public void armFinisher(UUID caster, long expiresAtMillis) {
            HunterState state = hunters.computeIfAbsent(caster, ignored -> new HunterState());
            state.finisherArmed = true;
            state.finisherEndsAt = expiresAtMillis;
        }

        public boolean consumeFinisher(UUID caster, boolean validClaim, boolean resetHeat) {
            HunterState state = hunters.get(caster);
            if (state == null || !validClaim || !state.finisherArmed) {
                return false;
            }
            state.finisherArmed = false;
            state.finisherEndsAt = 0;
            if (resetHeat) state.heat = 0;
            return true;
        }

        public boolean consumeFinisher(UUID caster, long nowMillis, boolean validClaim, boolean resetHeat) {
            if (!finisherArmed(caster, nowMillis)) return false;
            return consumeFinisher(caster, validClaim, resetHeat);
        }

        public void endHunt(UUID caster) {
            HunterState state = hunters.get(caster);
            if (state == null) return;
            state.huntEndsAt = 0;
            state.mobilityEndsAt = 0;
            state.mobilityCapAt = 0;
            state.rampage = 0;
            state.rampageGains.clear();
            state.finisherArmed = false;
            state.finisherEndsAt = 0;
        }

        public void reset(UUID caster) {
            hunters.remove(caster);
            marks.entrySet().removeIf(entry -> entry.getValue().caster().equals(caster));
        }

        public void clear() {
            hunters.clear();
            marks.clear();
        }

        private void prune(long nowMillis) {
            marks.values().removeIf(mark -> mark.expiresAtMillis() <= nowMillis);
        }
    }

    public static double storedBulwarkDamage(double current, double preventedDamage,
                                             double storageFraction, double cap) {
        double addition = Math.max(0, preventedDamage) * clamp(storageFraction, 0, 1);
        return clamp(current + addition, 0, Math.max(0, cap));
    }

    public static double scaledBulwarkValue(double stored, double storageCap,
                                             double minimum, double maximum) {
        if (maximum < minimum) {
            return minimum;
        }
        double ratio = storageCap <= 0 ? 0 : clamp(stored / storageCap, 0, 1);
        return minimum + (maximum - minimum) * ratio;
    }

    public static final class ReflectionGuard {
        private boolean reflecting;

        public boolean enter() {
            if (reflecting) {
                return false;
            }
            reflecting = true;
            return true;
        }

        public void exit() {
            reflecting = false;
        }

        public boolean active() {
            return reflecting;
        }
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
