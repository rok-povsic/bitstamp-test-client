import api.BitstampApiEndpoint;
import dto.MarketTrade;
import dto.Order;
import dto.Trade;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    private BitstampApiEndpoint api;

    // The default values if the config file isn't present.
    private String principalMarket = "btcusd";
    private int x = 10;
    private int m = 3;
    private int n = 3;

    @Override
    public void start(Stage primaryStage) throws IOException {
        readConfig();

        api = new BitstampApiEndpoint(principalMarket, x);
        api.connect();

        TableView<Order> bidTable = createOrderBookTable();
        TableView<Order> askTable = createOrderBookTable();
        TableView<Trade> tradeTable = createTradeTable();
        constructGui(primaryStage, bidTable, askTable, tradeTable);

        Thread th = new Thread(keepUpdatingGui(bidTable, askTable, tradeTable));
        th.setDaemon(true);
        th.start();
    }

    private TableView<Order> createOrderBookTable() {
        TableView<Order> bidTable = new TableView<>();
        bidTable.setEditable(false);
        TableColumn bidPrice = new TableColumn("Price");
        TableColumn bidVolume = new TableColumn("Volume");
        bidPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        bidVolume.setCellValueFactory(new PropertyValueFactory<>("volume"));
        bidTable.getColumns().addAll(bidPrice, bidVolume);
        adjustTableSize(bidTable);
        return bidTable;
    }

    private TableView<Trade> createTradeTable() {
        TableView<Trade> tradeTable = new TableView<>();
        tradeTable.setEditable(false);
        TableColumn tradeTime = new TableColumn("Time");
        TableColumn tradePrice = new TableColumn("Price");
        TableColumn tradeVolume = new TableColumn("Volume");
        TableColumn<Trade, String> tradeIsOur = new TableColumn("Is Our Trade");
        tradeTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        tradePrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        tradePrice.setCellFactory(new Callback<TableColumn, TableCell>() {
            public TableCell call(TableColumn param) {
                return new TableCell<Trade, BigDecimal>() {
                    @Override
                    public void updateItem(BigDecimal item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!isEmpty()) {
                            Trade trade = (Trade) this.getTableRow().getItem();
                            if (trade != null) {
                                if (trade.isUptick())
                                    this.setTextFill(Color.GREEN);
                                else if (trade.isDowntick())
                                    this.setTextFill(Color.RED);
                                else
                                    this.setTextFill(Color.BLACK);

                                if (trade.getOurTrade())
                                    this.setTextFill(Color.BLUE);
                            }

                            this.setText(item.toString());
                        }
                    }
                };
            }
        });

        tradeVolume.setCellValueFactory(new PropertyValueFactory<>("volume"));
        tradeIsOur.setCellValueFactory(cellData -> {
            boolean isOurTrade = cellData.getValue().getOurTrade();
            return new ReadOnlyStringWrapper(isOurTrade ? "x" : "");
        });
        tradeTable.getColumns().addAll(tradeTime, tradePrice, tradeVolume, tradeIsOur);
        adjustTableSize(tradeTable);
        return tradeTable;
    }

    private void adjustTableSize(TableView table) {
        table.setFixedCellSize(25);
        table.prefHeightProperty()
            .bind(Bindings.size(table.getItems())
                .multiply(table.getFixedCellSize()).add(30));
    }

    private void constructGui(Stage primaryStage, TableView<Order> bidTable,
                              TableView<Order> askTable, TableView<Trade> tradeTable) {
        primaryStage.setTitle("Contrarian Trading Simulation");

        StackPane root = new StackPane();

        FlowPane mainPane = new FlowPane();
        root.getChildren().add(mainPane);

        VBox bidBox = createOrderBookBox("Bids", Color.GREEN);
        VBox askBox = createOrderBookBox("Asks", Color.RED);

        mainPane.getChildren().add(bidBox);
        mainPane.getChildren().add(askBox);

        VBox tradeBox = new VBox();
        tradeBox.setAlignment(Pos.CENTER);
        tradeBox.setPadding(new Insets(5, 5, 5, 5));
        mainPane.getChildren().add(tradeBox);

        Label tradesHeader = new Label("Trades");
        tradesHeader.setFont(new Font(18));
        tradeBox.getChildren().add(tradesHeader);

        bidBox.getChildren().add(bidTable);
        askBox.getChildren().add(askTable);
        tradeBox.getChildren().add(tradeTable);

        primaryStage.setScene(new Scene(root, 900, 800));
        primaryStage.show();
    }

    private VBox createOrderBookBox(String bids, Color green) {
        VBox bidBox = new VBox();
        bidBox.setAlignment(Pos.CENTER);
        bidBox.setPadding(new Insets(5, 5, 5, 5));

        Label bidsHeader = new Label(bids);
        bidsHeader.setFont(new Font(18));
        bidsHeader.setTextFill(green);
        bidBox.getChildren().add(bidsHeader);
        return bidBox;
    }

    private Task<Void> keepUpdatingGui(TableView<Order> bidTable,
                                       TableView<Order> askTable,
                                       TableView<Trade> tradeTable) {
        return new Task<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    api.getOrderBookUpdateLock().lock();
                    try {
                        TreeMap<BigDecimal, BigDecimal> bids = api.orderBook().getBids();
                        List<BigDecimal> bidPrices = bids
                            .descendingKeySet().stream().filter(k -> bids.get(k) != null)
                            .limit(x).collect(Collectors.toList());

                        TreeMap<BigDecimal, BigDecimal> asks = api.orderBook().getAsks();
                        List<BigDecimal> askPrices = asks
                            .keySet().stream().filter(k -> asks.get(k) != null)
                            .limit(x).collect(Collectors.toList());

                        List<MarketTrade> marketTrades = api.retrieveTrades();
                        List<Trade> trades = new AlgoSimulator(m, n, marketTrades)
                            .withOurTrades().stream()
                            .limit(x)
                            .collect(Collectors.toList());

                        Platform.runLater(() -> {
                            bidTable.getItems().clear();
                            askTable.getItems().clear();
                            tradeTable.getItems().clear();

                            for (BigDecimal price : bidPrices) {
                                bidTable.getItems().add(new Order(price, bids.get(price)));
                            }

                            for (BigDecimal price : askPrices) {
                                askTable.getItems().add(new Order(price, asks.get(price)));
                            }

                            for (Trade trade : trades) {
                                tradeTable.getItems().add(trade);
                            }
                        });
                    } finally {
                        api.getOrderBookUpdateLock().unlock();
                    }

                    Thread.sleep(2000);
                }
            }
        };
    }

    private void readConfig() {
        try {
            URI res = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            File configFile = new File(new File(res).getParentFile(), "app.config");

            InputStream is = new FileInputStream(configFile);

            // load the properties file
            Properties prop = new Properties();
            prop.load(is);

            principalMarket = prop.getProperty("principal_market");
            x = Integer.parseInt(prop.getProperty("X"));
            m = Integer.parseInt(prop.getProperty("M"));
            n = Integer.parseInt(prop.getProperty("N"));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
