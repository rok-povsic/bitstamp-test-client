package dto;

import java.math.BigDecimal;

/**
 * The order on the order book.
 */
public class Order {
    private final BigDecimal price;
    private final BigDecimal volume;

    /**
     * Ctor.
     * @param price the price
     * @param volume the volume
     */
    public Order(BigDecimal price, BigDecimal volume) {
        this.price = price.setScale(2);
        this.volume = volume;
    }

    public BigDecimal getPrice() { return price; }
    public BigDecimal getVolume() { return volume; }
}
