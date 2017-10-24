package api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pusher.client.Pusher;
import com.pusher.client.channel.Channel;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;
import dto.MarketTrade;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The class used to connect to the Bitstamp API endpoint.
 */
public class BitstampApiEndpoint {
    private final OrderBook orderBook = new OrderBook();

    private final Gson gson = new Gson();
    private final OkHttpClient client = new OkHttpClient();

    private final String diffChannelBtcUsd = "diff_order_book";
    private final String diffChannel = "diff_order_book_";
    private final String dataEvent = "data";
    private final String orderBookApiUrl = "https://www.bitstamp.net/api/v2/order_book/";
    private final String tradesApiUrl = "https://www.bitstamp.net/api/v2/transactions/";
    private final String bitstampPusherKey = "de504dc5763aeef9ff52";

    private final CountDownLatch webSocketLatch = new CountDownLatch(1);
    private final ReentrantLock orderBookUpdateLock = new ReentrantLock();

    private final String principalMarket;
    private final int x;

    /**
     * Ctor.
     * @param principalMarket the market to show and trade
     * @param x the number of rows to display
     */
    public BitstampApiEndpoint(String principalMarket, int x) {
        this.principalMarket = principalMarket;
        this.x = x;
    }

    /**
     * Connect to the websocket, retrieve full order book.
     */
    public void connect() {
        connectToDiffWebSocket();

        try {
            // Only do the request for the full order book after the first diff event is received.
            webSocketLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        retrieveInitialFullOrderBook();
    }

    private void connectToDiffWebSocket() {
        Pusher pusher = new Pusher(bitstampPusherKey);

        pusher.connect(new ConnectionEventListener() {
            public void onConnectionStateChange(ConnectionStateChange change) {
                System.out.println("State changed to " + change.getCurrentState() +
                    " from " + change.getPreviousState());
            }

            public void onError(String message, String code, Exception e) {
                System.out.println("There was a problem connecting!");
            }
        }, ConnectionState.ALL);

        String channelName;
        if (principalMarket.equals("btcusd")) {
            channelName = diffChannelBtcUsd;
        } else {
            channelName = diffChannel + principalMarket;
        }
        Channel channel = pusher.subscribe(channelName);
        channel.bind(dataEvent, (channel1, event, data) -> {
            JsonObject jsonObject = gson.fromJson(data, JsonElement.class).getAsJsonObject();

            orderBookUpdateLock.lock();
            try {
                updateBids(jsonObject);
                updateAsks(jsonObject);
            } finally {
                orderBookUpdateLock.unlock();
            }

            webSocketLatch.countDown();
        });
    }

    /**
     * Get current order book.
     * @return the order book
     */
    public OrderBook orderBook() {
        return orderBook;
    }

    private void retrieveInitialFullOrderBook() {
        Request request = new Request.Builder()
            .url(orderBookApiUrl + principalMarket)
            .build();
        String initialResponse = null;
        try {
            Response response = client.newCall(request).execute();
            initialResponse = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JsonObject initialObject = gson.fromJson(initialResponse, JsonElement.class).getAsJsonObject();
        orderBookUpdateLock.lock();
        try {
            updateBids(initialObject);
            updateAsks(initialObject);
        } finally {
            orderBookUpdateLock.unlock();
        }
    }

    private void updateBids(JsonObject jsonObject) {
        JsonArray bids = jsonObject.get("bids").getAsJsonArray();
        for (int i = 0; i < bids.size(); i++) {
            JsonArray level = bids.get(i).getAsJsonArray();
            orderBook.updateBid(level.get(0).getAsBigDecimal(), level.get(1).getAsBigDecimal());
        }
    }

    private void updateAsks(JsonObject jsonObject) {
        JsonArray asks = jsonObject.get("asks").getAsJsonArray();
        for (int i = 0; i < asks.size(); i++) {
            JsonArray level = asks.get(i).getAsJsonArray();
            orderBook.updateAsk(level.get(0).getAsBigDecimal(), level.get(1).getAsBigDecimal());
        }
    }

    /**
     * Execute a GET request to get the latest trades.
     * @return the latest trades
     */
    public List<MarketTrade> retrieveTrades() {
        Request request = new Request.Builder()
            .url(tradesApiUrl + principalMarket)
            .build();
        String initialResponse = null;
        try {
            Response response = client.newCall(request).execute();
            initialResponse = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<MarketTrade> trades = new ArrayList<>();
        JsonArray jsonTrades = gson.fromJson(initialResponse, JsonElement.class).getAsJsonArray();
        for (int i = 0; i < Math.min(jsonTrades.size(), x * 2); i++) {
            JsonObject jsonTrade = jsonTrades.get(i).getAsJsonObject();
            trades.add(new MarketTrade(
                jsonTrade.get("date").getAsLong(),
                new BigDecimal(jsonTrade.get("price").getAsString()),
                new BigDecimal(jsonTrade.get("amount").getAsString())));
        }
        return trades;
    }

    /**
     * The lock that's used when the order book gets updated.
     * @return the order book update lock
     */
    public ReentrantLock getOrderBookUpdateLock() {
        return orderBookUpdateLock;
    }
}
