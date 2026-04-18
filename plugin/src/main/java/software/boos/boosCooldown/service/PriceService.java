package software.boos.boosCooldown.service;

import software.boos.boosCooldown.economy.EconomyProvider;
import software.boos.boosCooldown.model.CommandData;
import org.bukkit.entity.Player;

import java.util.List;

public final class PriceService {

    private final List<EconomyProvider> providers;

    public PriceService(List<EconomyProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public List<EconomyProvider> applicableProviders(CommandData data) {
        return providers.stream()
                .filter(EconomyProvider::isAvailable)
                .filter(p -> p.appliesTo(data))
                .toList();
    }

    /**
     * Attempts to charge all applicable providers. Returns a {@link Result}
     * reporting success, or the provider that failed with a reason message.
     */
    public Result chargeAll(Player player, CommandData data) {
        List<EconomyProvider> applicable = applicableProviders(data);
        for (EconomyProvider provider : applicable) {
            if (!provider.hasEnough(player, data)) {
                return Result.insufficient(provider, provider.describeShortage(player, data));
            }
        }
        for (EconomyProvider provider : applicable) {
            if (!provider.charge(player, data)) {
                return Result.chargeFailed(provider);
            }
        }
        return Result.success(applicable);
    }

    /**
     * Check-only variant of {@link #chargeAll}: walks every applicable provider
     * and reports the first shortage without mutating any balances. Used by the
     * command pipeline to pre-flight affordability before consuming a usage
     * counter so players never pay for a command they can't actually run.
     */
    public Result checkAffordable(Player player, CommandData data) {
        List<EconomyProvider> applicable = applicableProviders(data);
        for (EconomyProvider provider : applicable) {
            if (!provider.hasEnough(player, data)) {
                return Result.insufficient(provider, provider.describeShortage(player, data));
            }
        }
        return Result.success(applicable);
    }

    /**
     * Best-effort refund — used when a warmup is cancelled and
     * {@code options.refund_on_warmup_cancel} is enabled.
     */
    public void refundAll(Player player, CommandData data, List<EconomyProvider> providersToRefund) {
        for (EconomyProvider provider : providersToRefund) {
            provider.refund(player, data);
        }
    }

    public record Result(boolean success, EconomyProvider failedProvider,
                         String failureReason, List<EconomyProvider> chargedProviders) {

        static Result success(List<EconomyProvider> charged) {
            return new Result(true, null, null, charged);
        }

        static Result insufficient(EconomyProvider provider, String reason) {
            return new Result(false, provider, reason, List.of());
        }

        static Result chargeFailed(EconomyProvider provider) {
            return new Result(false, provider,
                    "Provider " + provider.name() + " reported a transaction failure", List.of());
        }
    }
}
