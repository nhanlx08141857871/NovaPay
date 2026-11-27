package com.novabank.novapay.domain.model.account;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class SavingCalculator {

    public static final double DEFAULT_DEMAND_ANNUAL_RATE = 0.5;
    public static final ZoneId HO_CHI_MINH_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long MILLIS_PER_DAY = 86_400_000L;

    private SavingCalculator() {
    }

    /**
     * Lãi dự kiến hàng tháng: floor((principal * annualRate) / (12.0 * 100.0)).
     */
    public static long calculateMonthlyProfit(long principal, double annualRate) {
        return SavingInfo.calculateMonthlyProfit(principal, annualRate);
    }

    /**
     * Tổng lãi cuối kỳ khi gửi đủ kỳ hạn termMonths:
     * floor((principal * annualRate * termMonths) / (12.0 * 100.0)).
     */
    public static long calculateMaturityInterest(long principal, double annualRate, int termMonths) {
        if (principal <= 0L || annualRate <= 0.0 || termMonths <= 0) {
            return 0L;
        }
        return (long) Math.floor((principal * annualRate * termMonths) / (12.0 * 100.0));
    }

    /**
     * Lãi tất toán trước hạn hưởng lãi không kỳ hạn theo số ngày thực gửi:
     * floor((principal * demandRate * daysHeld) / (365.0 * 100.0)).
     */
    public static long calculateEarlyInterest(long principal, double demandAnnualRate, long daysHeld) {
        if (principal <= 0L || demandAnnualRate <= 0.0 || daysHeld <= 0L) {
            return 0L;
        }
        return (long) Math.floor((principal * demandAnnualRate * daysHeld) / (365.0 * 100.0));
    }

    public static SavingSettlementPreview previewSettlement(
            SavingProjection projection,
            double demandAnnualRate,
            Instant now
    ) {
        if (projection == null || projection.getAccount() == null) {
            return new SavingSettlementPreview(0L, 0L, 0L, false, 0.0, 0L);
        }
        Account account = projection.getAccount();
        long principal = account.getBalance();
        SavingInfo saving = account.getSaving();
        Instant effectiveNow = now != null ? now : Instant.now();

        if (saving == null) {
            double rate = demandAnnualRate > 0.0 ? demandAnnualRate : DEFAULT_DEMAND_ANNUAL_RATE;
            Instant openedAt = account.getCreatedAt() != null ? account.getCreatedAt() : effectiveNow;
            long daysHeld = Math.max(0L, (effectiveNow.toEpochMilli() - openedAt.toEpochMilli()) / MILLIS_PER_DAY);
            long interest = calculateEarlyInterest(principal, rate, daysHeld);
            return new SavingSettlementPreview(
                    principal,
                    interest,
                    principal + interest,
                    false,
                    rate,
                    daysHeld
            );
        }

        Instant openedAt = saving.getOpenedAt() != null
                ? saving.getOpenedAt()
                : (account.getCreatedAt() != null ? account.getCreatedAt() : effectiveNow);
        long daysHeld = Math.max(0L, (effectiveNow.toEpochMilli() - openedAt.toEpochMilli()) / MILLIS_PER_DAY);

        Instant maturityDate = saving.getMaturityDate();
        boolean isEarly = maturityDate != null && effectiveNow.isBefore(maturityDate);

        if (isEarly) {
            double effectiveDemandRate = demandAnnualRate > 0.0 ? demandAnnualRate : DEFAULT_DEMAND_ANNUAL_RATE;
            long interest = calculateEarlyInterest(principal, effectiveDemandRate, daysHeld);
            return new SavingSettlementPreview(
                    principal,
                    interest,
                    principal + interest,
                    true,
                    effectiveDemandRate,
                    daysHeld
            );
        }

        double termRate = projection.getEffectiveAnnualRate();
        long interest = saving.getTermMonths() > 0
                ? calculateMaturityInterest(principal, termRate, saving.getTermMonths())
                : calculateEarlyInterest(principal, termRate > 0.0 ? termRate : DEFAULT_DEMAND_ANNUAL_RATE, daysHeld);
        return new SavingSettlementPreview(
                principal,
                interest,
                principal + interest,
                false,
                termRate,
                daysHeld
        );
    }

    public static Instant estimateMaturityDate(Instant openedAt, int termMonths) {
        Instant base = openedAt != null ? openedAt : Instant.now();
        int safeMonths = Math.max(1, termMonths);
        LocalDate targetDate = LocalDate.ofInstant(base, HO_CHI_MINH_ZONE).plusMonths(safeMonths);
        ZonedDateTime baseZoned = ZonedDateTime.ofInstant(base, HO_CHI_MINH_ZONE);
        return targetDate.atTime(baseZoned.toLocalTime()).atZone(HO_CHI_MINH_ZONE).toInstant();
    }
}
