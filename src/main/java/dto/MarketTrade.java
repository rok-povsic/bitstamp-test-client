package dto;

import java.math.BigDecimal;

/**
 * A trade that occurred on the market.
 */
public class MarketTrade {
    private final long time;
    private final BigDecimal price;
    private final BigDecimal volume;

    /**
     * Ctor.
     * @param time the trade time
     * @param price the price
     * @param volume the volume
     */
    public MarketTrade(long time, BigDecimal price, BigDecimal volume) {
        this.time = time;
        this.price = price;
        this.volume = volume;
    }

    public long getTime() { return time; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getVolume() { return volume; }
}
