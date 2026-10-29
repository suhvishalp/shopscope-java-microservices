package com.shopscope.monolith.jdbc;

import com.shopscope.monolith.pricing.Price;
import com.shopscope.monolith.pricing.PriceScope;
import com.shopscope.monolith.shared.Money;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain JDBC, once, so that what Spring Data JPA does in Demo 7 is not magic: open a connection, prepare
 * a statement, iterate a result set, map each row by hand, close everything. Used by Demo 0, Lab 8
 * against a PostgreSQL started with Docker; see {@code src/main/resources/sql/schema.sql}.
 */
public final class JdbcPriceLoader {

    private static final String SELECT = """
            select sku, amount, currency, scope_type, scope_value
            from price
            where currency = ?
            order by sku
            """;

    private JdbcPriceLoader() {
    }

    public static List<Price> load(String jdbcUrl, String user, String password, String currency) throws SQLException {
        List<Price> prices = new ArrayList<>();
        // TODO(lab-8.2) open a Connection with DriverManager, prepare SELECT, bind the currency, iterate the
        // ResultSet and map each row to a Price with scope(...). Use try-with-resources for all three.
        return prices;
    }

    private static PriceScope scope(String type, String value) {
        return switch (type) {
            case "BASE" -> new PriceScope.Base();
            case "COUNTRY" -> new PriceScope.ForCountry(value);
            case "CHANNEL" -> new PriceScope.ForChannel(value);
            case "CUSTOMER_GROUP" -> new PriceScope.ForCustomerGroup(value);
            default -> throw new IllegalArgumentException("Unknown scope type " + type);
        };
    }
}
