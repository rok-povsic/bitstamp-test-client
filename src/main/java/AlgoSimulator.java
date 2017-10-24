import dto.MarketTrade;
import dto.Trade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes the market trades and inserts our simulated trades.
 */
public class AlgoSimulator {
    private final List<MarketTrade> allTrades;

    private final int m;
    private final int n;

    /**
     * Ctor.
     * @param m the number of upticks to go short
     * @param n the number of downticks to go short
     * @param marketTrades the market trades
     */
    public AlgoSimulator(int m, int n, List<MarketTrade> marketTrades) {
        this.m = m;
        this.n = n;
        this.allTrades = marketTrades;
    }

    /**
     * Returns market trades + our simulated trades
     * @return all trades
     */
    public List<Trade> withOurTrades() {
        ArrayList<Trade> result = new ArrayList<>();

        int upTicks = 0;
        int downTicks = 0;
        for (int i = allTrades.size() - 1; i >= 0; i--) {
            MarketTrade curTrade = allTrades.get(i);

            if (i == allTrades.size() - 1) {
                result.add(0, new Trade(curTrade.getTime(), curTrade.getPrice(),
                    curTrade.getVolume(), false, false, false));
            } else {
                MarketTrade prevTrade = allTrades.get(i + 1);

                int compare = curTrade.getPrice().compareTo(prevTrade.getPrice());
                if (compare > 0) {
                    upTicks++;
                    downTicks = 0;
                } else if (compare < 0) {
                    upTicks = 0;
                    downTicks++;
                }

                result.add(0, new Trade(curTrade.getTime(), curTrade.getPrice(),
                    curTrade.getVolume(), false, compare > 0, compare < 0));

                if (m <= upTicks) {
                    result.add(0, new Trade(curTrade.getTime(), curTrade.getPrice(),
                        new BigDecimal("-1"), true, false, false));
                    upTicks = 0;
                }

                if (n <= downTicks) {
                    result.add(0, new Trade(curTrade.getTime(), curTrade.getPrice(),
                        new BigDecimal("1"), true, false, false));
                    downTicks = 0;
                }
            }
        }
        return result;
    }
}
