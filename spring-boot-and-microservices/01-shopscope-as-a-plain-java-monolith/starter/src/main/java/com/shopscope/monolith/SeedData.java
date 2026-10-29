package com.shopscope.monolith;

import com.shopscope.monolith.catalog.CatalogRepository;
import com.shopscope.monolith.catalog.Product;
import com.shopscope.monolith.customer.Customer;
import com.shopscope.monolith.customer.CustomerService;
import com.shopscope.monolith.inventory.InventoryService;
import com.shopscope.monolith.pricing.DiscountRule;
import com.shopscope.monolith.pricing.Price;
import com.shopscope.monolith.pricing.PriceList;
import com.shopscope.monolith.shared.Money;

import java.util.List;

/** The data every run starts with. The same four products appear in every demo of the track. */
public final class SeedData {

    public static final String TEA = "SKU-TEA-001";
    public static final String MUG = "SKU-MUG-001";
    public static final String KETTLE = "SKU-KET-001";
    public static final String BISCUITS = "SKU-BIS-001";

    private SeedData() {
    }

    public static void products(CatalogRepository catalog) {
        catalog.save(new Product(TEA, "Assam breakfast tea 250g", "tea", "Strong, malty, the one for mornings."));
        catalog.save(new Product(MUG, "ShopScope stoneware mug", "kitchen", "Holds 350 ml and a lot of opinions."));
        catalog.save(new Product(KETTLE, "Electric kettle 1.7 L", "kitchen", "Boils a litre in under three minutes."));
        catalog.save(new Product(BISCUITS, "Oat biscuits 400g", "snacks", "Dunk responsibly."));
    }

    public static PriceList prices() {
        return new PriceList(List.of(
                Price.base(TEA, Money.of("349.00", "INR")),
                Price.forCustomerGroup(TEA, Money.of("299.00", "INR"), "wholesale"),
                Price.base(TEA, Money.of("4.50", "GBP")),
                Price.base(MUG, Money.of("499.00", "INR")),
                Price.forChannel(MUG, Money.of("449.00", "INR"), "app"),
                Price.base(MUG, Money.of("6.00", "GBP")),
                Price.base(KETTLE, Money.of("2499.00", "INR")),
                Price.forCountry(KETTLE, Money.of("2299.00", "INR"), "IN"),
                Price.base(KETTLE, Money.of("29.00", "GBP")),
                Price.base(BISCUITS, Money.of("129.00", "INR")),
                Price.base(BISCUITS, Money.of("1.80", "GBP"))));
    }

    public static List<DiscountRule> discountRules() {
        return List.of(
                new DiscountRule.PercentageOff("TEA10", TEA, 10),
                new DiscountRule.BuyXGetYFree("BISCUITS-3-FOR-2", BISCUITS, 2, 1),
                new DiscountRule.BasketOver("BIG-BASKET-5", Money.of("5000.00", "INR"), 5));
    }

    public static void stock(InventoryService inventory) {
        inventory.setStock(TEA, 50);
        inventory.setStock(MUG, 5);
        inventory.setStock(KETTLE, 2);
        inventory.setStock(BISCUITS, 100);
    }

    public static void customers(CustomerService customers) {
        customers.register(new Customer("c-1", "priya@example.com", "Priya", "retail"));
        customers.register(new Customer("c-2", "arjun@example.com", "Arjun", "wholesale"));
    }
}
