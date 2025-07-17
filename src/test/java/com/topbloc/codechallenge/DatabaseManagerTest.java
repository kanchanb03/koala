package com.topbloc.codechallenge;

import com.topbloc.codechallenge.db.DatabaseManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseManagerTest {

    @BeforeAll  static void open()      { DatabaseManager.connect(); }
    @BeforeEach void reset()           { DatabaseManager.resetDatabase(); }

    /* items */

    @Test @Order(1)
    void addItem_success() {
        JSONObject out = DatabaseManager.addItem("{\"name\":\"Swedish Fish\"}");
        assertEquals("Swedish Fish", out.get("name"));
        assertTrue(((Number) out.get("id")).longValue() > 0);
    }

    @Test
    void addItem_duplicateGivesError() {
        DatabaseManager.addItem("{\"name\":\"Pop Rocks\"}");
        JSONObject dup = DatabaseManager.addItem("{\"name\":\"Pop Rocks\"}");
        assertEquals("Item already exists", dup.get("error"));
    }

    @Test
    void deleteItem_cascadesInventory() {
        /* item 1 (Licorice) has one inventory row in seed data */
        long before = DatabaseManager.getAllInventory()
                .stream()
                .filter(o -> ((Number) ((JSONObject) o).get("item_id")).longValue() == 1L)
                .count();
        assertEquals(1, before);

        JSONObject res = DatabaseManager.deleteItem(1);
        assertEquals("ok", res.get("status"));

        long after = DatabaseManager.getAllInventory()
                .stream()
                .filter(o -> ((Number) ((JSONObject) o).get("item_id")).longValue() == 1L)
                .count();
        assertEquals(0, after);
    }

    /*  inventory */

    @Test
    void inventory_addUpdateDelete() {
        JSONObject newItem = DatabaseManager.addItem("{\"name\":\"Pop Rocks\"}");
        long itemId        = ((Number) newItem.get("id")).longValue();

        JSONObject inv = DatabaseManager.addInventory(
                String.format("{\"item\":%d,\"stock\":4,\"capacity\":9}", itemId));
        int invId = ((Number) inv.get("id")).intValue();
        assertEquals(4, ((Number) inv.get("amount_in_stock")).intValue());

        /* update */
        DatabaseManager.updateInventory(invId, "{\"stock\":7,\"capacity\":12}");
        JSONArray after = DatabaseManager.getInventoryById(invId);
        assertEquals(7, ((Number) ((JSONObject) after.get(0)).get("amount_in_stock")).intValue());

        /* delete */
        JSONObject del = DatabaseManager.deleteInventory(invId);
        assertEquals("ok", del.get("status"));
        assertTrue(DatabaseManager.getInventoryById(invId).isEmpty());
    }

    /* pricing  */

    @Test
    void cheapestOffer_returnsCorrectDistributor() {
        JSONObject best = DatabaseManager.getCheapestOffer(10, 100); // Snickers
        assertEquals("The Sweet Suite", best.get("distributor_name"));
        assertEquals(25.0, best.get("total_cost"));
    }

    /*  CSV  */

    @Test
    void exportInventoryCsv_containsNames() {
        String csv = DatabaseManager.exportTableAsCsv("inventory");
        assertTrue(csv.startsWith("inventory_id,item_name,"));
        assertTrue(csv.contains("Candy Corn"));
        assertFalse(csv.contains(",8,8,"));  // raw ids side-by-side shouldn’t appear
    }
}
