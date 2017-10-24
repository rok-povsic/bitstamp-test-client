package dto;

import java.math.BigDecimal;

/**
 * A trade that occurred on the market or is our simulated trade.
 */
public class Trade {
    private final long time;
    private final BigDecimal price;
    private final BigDecimal volume;
    private final boolean ourTrade;
    private final boolean isUptick;
    private final boolean isDowntick;

    /**
     * Ctor.
     * @param time the trade time
     * @param price the price
     * @param volume the volume
     * @param ourTrade is this our simulated trade
     * @param isUptick is this trade's price higher than previous trade's price
     * @param isDowntick is this trade's price lower than previous trade's price
     */
    public Trade(long time, BigDecimal price, BigDecimal volume, boolean ourTrade, boolean isUptick,
                 boolean isDowntick) {
        this.time = time;
        this.price = price;
        this.volume = volume;
        this.ourTrade = ourTrade;
        this.isUptick = isUptick;
        this.isDowntick = isDowntick;
    }

    public long getTime() { return time; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getVolume() { return volume; }
    public boolean getOurTrade() { return ourTrade; }
    public boolean isUptick() { return isUptick; }
    public boolean isDowntick() { return isDowntick; }
}
