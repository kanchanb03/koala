package com.topbloc.codechallenge;

import com.topbloc.codechallenge.db.DatabaseManager;

import static spark.Spark.*;


import java.io.PrintWriter;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        DatabaseManager.connect();

        before((req, res) -> res.header("Access-Control-Allow-Origin", "*"));

        // Don’t change – browsers send a pre‑flight OPTIONS request for JSON
        options("/*", (req, res) -> {
            res.header("Access-Control-Allow-Headers", "content-type");
            res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            return "OK";
        });

        // Don't change - if required you can reset your database by hitting this endpoint at localhost:4567/reset
        get("/reset", (req, res) -> {
            DatabaseManager.resetDatabase();
            return "OK";
        });

        // JSON error handler
        exception(Exception.class, (e, req, res) -> {
            res.type("application/json");
            res.status(400);
            String safe = (e.getMessage() == null ? " error" : e.getMessage()).replace("\"", "\\\"");
            res.body("{\"error\":\"" + safe + "\"}");
        });

        //  version string
        get("/version", (req, res) -> "TopBloc Code Challenge v1.0");

        /* ---Item route---*/

        get("/items",  (req, res) -> DatabaseManager.getItems());
        post("/items", (req, res) -> DatabaseManager.addItem(req.body()));
        delete("/items/:id", (req, res) -> DatabaseManager.deleteItem(Integer.parseInt(req.params("id"))).toJSONString());

        /* ---  Inventory route---*/
        get("/inventory",               (req, res) -> DatabaseManager.getAllInventory().toJSONString());
        get("/inventory/out-of-stock",  (req, res) -> DatabaseManager.getOutOfStock().toJSONString());
        get("/inventory/overstocked",   (req, res) -> DatabaseManager.getOverstocked().toJSONString());
        get("/inventory/low-stock",     (req, res) -> DatabaseManager.getLowStock().toJSONString());
        get("/inventory/:id",           (req, res) -> DatabaseManager.getInventoryById(Integer.parseInt(req.params("id"))).toJSONString());
        post("/inventory",              (req, res) -> DatabaseManager.addInventory(req.body()).toJSONString());
        put("/inventory/:id",           (req, res) -> DatabaseManager.updateInventory(Integer.parseInt(req.params("id")), req.body()).toJSONString());
        delete("/inventory/:id",        (req, res) -> DatabaseManager.deleteInventory(Integer.parseInt(req.params("id"))).toJSONString());

        /* --- Distributor routes----*/
        get("/distributors",                      (req, res) -> DatabaseManager.getDistributors().toJSONString());
        post("/distributors",                     (req, res) -> DatabaseManager.addDistributor(req.body()).toJSONString());
        delete("/distributors/:id",               (req, res) -> DatabaseManager.deleteDistributor(Integer.parseInt(req.params("id"))).toJSONString());

        get("/distributors/:id/items",            (req, res) -> DatabaseManager.getOfferingsByDistributor(Integer.parseInt(req.params("id"))).toJSONString());
        post("/distributors/:id/catalog",         (req, res) -> DatabaseManager.addPrice(Integer.parseInt(req.params("id")), req.body()).toJSONString());
        put("/distributors/:id/catalog/:itemId",  (req, res) -> DatabaseManager.updatePrice(Integer.parseInt(req.params("id")), Integer.parseInt(req.params("itemId")), req.body()).toJSONString());

        /* --- distributor look‑ups  --- */
        get("/items/:id/distributors",            (req, res) -> DatabaseManager.getOfferingsByItem(Integer.parseInt(req.params("id"))).toJSONString());
        get("/items/:id/restock/:quantity/cheapest", (req, res) -> DatabaseManager.getCheapestOffer(Integer.parseInt(req.params("id")), Integer.parseInt(req.params("quantity"))).toJSONString());

        /* ---- CSV export---*/
        get("/export", (req, res) -> { res.type("text/csv"); return DatabaseManager.exportTableAsCsv(req.queryParams("table")); });

        /* --- live inventory stream----*/
        get("/stream/inventory", (req, res) -> {
            res.type("text/event-stream");
            res.header("Cache-Control", "no-cache");
            res.header("Connection", "keep-alive");
            PrintWriter out = res.raw().getWriter();
            while (!Thread.currentThread().isInterrupted()) {
                out.print("data: " + DatabaseManager.getAllInventory().toJSONString() + "\n\n");
                out.flush();
                Thread.sleep(5000);
            }
            return null;
        });

        /* --- Start server ----*/
        init();
        awaitInitialization();
        System.out.println("Spark listening to localhost:4567");


    }
}