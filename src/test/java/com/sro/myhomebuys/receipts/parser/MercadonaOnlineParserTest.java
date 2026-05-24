package com.sro.myhomebuys.receipts.parser;

import com.sro.myhomebuys.receipts.exception.ReceiptParsingException;
import com.sro.myhomebuys.receipts.model.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MercadonaOnlineParserTest {

    private final MercadonaOnlineParser parser = new MercadonaOnlineParser();

    @Test
    @DisplayName("canHandle: true for MERCADONA (any case)")
    void canHandleMercadona() {
        assertThat(parser.canHandle("MERCADONA")).isTrue();
        assertThat(parser.canHandle("mercadona")).isTrue();
        assertThat(parser.canHandle("Mercadona")).isTrue();
    }

    @Test
    @DisplayName("canHandle: false for unknown stores")
    void canHandleUnknown() {
        assertThat(parser.canHandle("LIDL")).isFalse();
        assertThat(parser.canHandle("CARREFOUR")).isFalse();
    }

    @Test
    @DisplayName("parseText: parses a valid Mercadona receipt (PDFBox horizontal format)")
    void parseValidReceipt() {
        // PDFBox with sortByPosition=true concatenates columns horizontally
        String text = """
                Pedido Nº 30267537
                Factura simplificada 4547 - 90 - 547163
                41500, Alcalá de Guadaíra Cobrado el 07/05/26 a las 03:26

                Nombre Producto Cantidad entregada PVP
                Bolsas de basura perfumadas Bosque Verde 5L cubo pequeño 1 1,40 €
                Patatas bravas con mayonesa y salsa picante Hacendado 2 5,20 €
                Tortellini tres quesos Pagani 2 2,80 €
                Productos 138,23 €
                Coste de preparación 8,20 €
                TOTAL 146,43 €
                Desglose de impuestos
                """;

        ParsedReceipt result = parser.parseText(text);

        assertThat(result.getStore()).isEqualTo(Store.MERCADONA);
        assertThat(result.getDate()).isEqualTo(java.time.LocalDate.of(2026, 5, 7));
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("146.43"));
        assertThat(result.getItems()).hasSize(3);

        assertThat(result.getItems().get(0))
                .satisfies(item -> {
                    assertThat(item.getProductName()).isEqualTo("Bolsas de basura perfumadas Bosque Verde 5L cubo pequeño");
                    assertThat(item.getQuantity()).isEqualTo(1);
                    assertThat(item.getPricePerUnit()).isEqualByComparingTo(new BigDecimal("1.40"));
                });

        assertThat(result.getItems().get(1))
                .satisfies(item -> {
                    assertThat(item.getProductName()).isEqualTo("Patatas bravas con mayonesa y salsa picante Hacendado");
                    assertThat(item.getQuantity()).isEqualTo(2);
                    assertThat(item.getPricePerUnit()).isEqualByComparingTo(new BigDecimal("5.20"));
                });
    }

    @Test
    @DisplayName("parseText: handles two-page receipt with repeated headers")
    void parseTwoPageReceipt() {
        String text = """
                Pedido Nº 30267537
                Cobrado el 07/05/26 a las 03:26

                Nombre Producto Cantidad entregada PVP
                Producto Pagina 1 1 1,50 €
                MERCADONA.S.A | A-46103834 | Calle Alfonso Roig Alfonso, s/n.
                condiciones generales de la totalidad del contenido del pedido
                Nombre Producto Cantidad entregada PVP
                Producto Pagina 2 2 3,00 €
                Productos 4,50 €
                TOTAL 4,50 €
                """;

        ParsedReceipt result = parser.parseText(text);

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Producto Pagina 1");
        assertThat(result.getItems().get(1).getProductName()).isEqualTo("Producto Pagina 2");
    }

    @Test
    @DisplayName("parseText: throws exception when no date found")
    void parseNoDate() {
        String text = """
                Pedido Nº 30267537

                Nombre Producto Cantidad entregada PVP
                Producto 1 1,50 €
                Productos 1,50 €
                TOTAL 1,50 €
                """;

        assertThatThrownBy(() -> parser.parseText(text))
                .isInstanceOf(ReceiptParsingException.class)
                .hasMessageContaining("date");
    }

    @Test
    @DisplayName("parseText: throws exception when no products found")
    void parseNoProducts() {
        String text = """
                Pedido Nº 30267537
                Cobrado el 07/05/26 a las 03:26

                Nombre Producto Cantidad entregada PVP
                Productos 0 €
                TOTAL 0 €
                """;

        assertThatThrownBy(() -> parser.parseText(text))
                .isInstanceOf(ReceiptParsingException.class)
                .hasMessageContaining("products");
    }

    @Test
    @DisplayName("parseText: handles product names with digits")
    void parseProductNameWithDigits() {
        String text = """
                Pedido Nº 123
                Cobrado el 01/01/26 a las 10:00

                Nombre Producto Cantidad entregada PVP
                12 Mini saladas surtidas 1 1,40 €
                Productos 1,40 €
                TOTAL 1,40 €
                """;

        ParsedReceipt result = parser.parseText(text);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("12 Mini saladas surtidas");
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("parseText: ignores tax section lines")
    void parseIgnoresTaxSection() {
        String text = """
                Pedido Nº 123
                Cobrado el 01/01/26 a las 10:00

                Nombre Producto Cantidad entregada PVP
                Leche 1 1,15 €
                Productos 1,15 €
                TOTAL 1,15 €
                Desglose de impuestos
                IVA Base Cuota
                21% 1,23 € 0,26 €
                10% 85,92 € 8,59 €
                """;

        ParsedReceipt result = parser.parseText(text);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Leche");
    }
}
