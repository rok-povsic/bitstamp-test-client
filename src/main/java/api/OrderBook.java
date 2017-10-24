package api;

import java.math.BigDecimal;
import java.util.TreeMap;

/**
 * Represents the current order book.
 */
public class OrderBook {

    private final TreeMap<BigDecimal, BigDecimal> bids = new TreeMap<>();
    private final TreeMap<BigDecimal, BigDecimal> asks = new TreeMap<>();

    /**
     * Get the bids.
     * @return the bids
     */
    public TreeMap<BigDecimal, BigDecimal> getBids() {
        return bids;
    }

    /**
     * Get the asks.
     * @return the asks
     */
    public TreeMap<BigDecimal, BigDecimal> getAsks() {
        return asks;
    }

    /**
     * Update the bid or delete it if zero volume
     * @param price the bid price
     * @param volume the bid volume
     */
    public void updateBid(BigDecimal price, BigDecimal volume) {
        updateSide(bids, price, volume);
    }

    /**
     * Update the ask or delete it if zero volume
     * @param price the ask price
     * @param volume the ask volume
     */
    public void updateAsk(BigDecimal price, BigDecimal volume) {
        updateSide(asks, price, volume);
    }

    private void updateSide(TreeMap<BigDecimal, BigDecimal> side, BigDecimal price,
                            BigDecimal volume) {
        if (volume.compareTo(BigDecimal.ZERO) != 0) {
            side.put(price, volume);
        } else {
            if (side.containsKey(price)) {
                side.remove(price);
            }
        }
    }
}
