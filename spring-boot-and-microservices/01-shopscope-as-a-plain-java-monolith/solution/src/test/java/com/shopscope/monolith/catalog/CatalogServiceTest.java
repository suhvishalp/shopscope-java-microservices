package com.shopscope.monolith.catalog;

import com.shopscope.monolith.SeedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogServiceTest {

    private CatalogService catalog;

    @BeforeEach
    void seed() {
        CatalogRepository repository = new CatalogRepository();
        SeedData.products(repository);
        catalog = new CatalogService(repository);
    }

    @Test
    void findReturnsAnOptionalThatMayBeEmpty() {
        assertThat(catalog.find(SeedData.TEA)).hasValueSatisfying(product -> assertThat(product.category()).isEqualTo("tea"));
        assertThat(catalog.find("SKU-NOPE")).isEmpty();
    }

    @Test
    void getThrowsANamedExceptionForAnUnknownSku() {
        assertThatThrownBy(() -> catalog.get("SKU-NOPE"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("No product with SKU SKU-NOPE")
                .extracting("sku").isEqualTo("SKU-NOPE");
    }

    @Test
    void inCategoryFiltersWithoutExposingTheMap() {
        assertThat(catalog.inCategory("kitchen")).extracting(Product::sku).containsExactly(SeedData.MUG, SeedData.KETTLE);
        assertThat(catalog.all()).hasSize(4);
    }
}
