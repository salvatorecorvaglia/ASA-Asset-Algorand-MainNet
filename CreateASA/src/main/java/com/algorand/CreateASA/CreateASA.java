package com.algorand.CreateASA;

import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.v2.client.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import com.algorand.algosdk.v2.client.common.*;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.Encoder;

public class CreateASA {
    public AlgodClient client = null;
    // Connect to a node
    private AlgodClient connectToNetwork() {

        // Initialize an algod client
        final String ALGOD_API_ADDR = "http://127.0.0.1";
        final int ALGOD_PORT = 5001;
        final String ALGOD_API_TOKEN = "insert token";
        return new AlgodClient(ALGOD_API_ADDR, ALGOD_PORT, ALGOD_API_TOKEN);
    }
    // Print created asset
    public void printCreatedAsset(Account account, Long assetID) throws Exception {
        if (client == null)
            this.client = connectToNetwork();
        String accountInfo = client.AccountInformation(account.getAddress()).execute().toString();
        JSONObject jsonObj = new JSONObject(accountInfo);
        JSONArray jsonArray = (JSONArray) jsonObj.get("created-assets");
        if (jsonArray.length() > 0) {
            for (Object o : jsonArray) {
                JSONObject ca = (JSONObject) o;
                Integer myassetIDInt = (Integer) ca.get("index");
                if (assetID == myassetIDInt.longValue()) {
                    System.out.println("Created Asset Info: " + ca.toString(2)); // pretty print
                    break;
                }
            }
        }
    }
    // Function to print asset holding
    public void printAssetHolding(Account account, Long assetID) throws Exception {
        if (client == null)
            this.client = connectToNetwork();
        String accountInfo = client.AccountInformation(account.getAddress()).execute().toString();
        JSONObject jsonObj = new JSONObject(accountInfo);
        JSONArray jsonArray = (JSONArray) jsonObj.get("assets");
        if (jsonArray.length() > 0) {
            for (Object o : jsonArray) {
                JSONObject ca = (JSONObject) o;
                Integer myassetIDInt = (Integer) ca.get("asset-id");
                if (assetID == myassetIDInt.longValue()) {
                    System.out.println("Asset Holding Info: " + ca.toString(2)); // pretty print
                    break;
                }
            }
        }
    }

    // Function to wait on a transaction to be confirmed
    public void waitForConfirmation(String txID) throws Exception {
        if (client == null)
            this.client = connectToNetwork();

        Long lastRound = client.GetStatus().execute().body().lastRound;

        while (true) {
            // Check the pending transaction
            Response<PendingTransactionResponse> pendingInfo = client.PendingTransactionInformation(txID).execute();
            if (pendingInfo.body().confirmedRound != null && pendingInfo.body().confirmedRound > 0) {
                // Got the completed Transaction
                System.out.println(
                        "Transaction " + txID + " confirmed in round " + pendingInfo.body().confirmedRound);
                break;
            }
            lastRound++;
            client.WaitForBlock(lastRound).execute();
        }
    }

    // Function for sending a raw signed transaction to the network
    public String submitTransaction(SignedTransaction signedTx) throws Exception {

        // Msgpack encode the signed transaction
        byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTx);
        return (client.RawTransaction().rawtxn(encodedTxBytes).execute().body().txId);
    }
    public void createAsset() throws Exception {
        if (client == null)
            this.client = connectToNetwork();

        // Recover account
        final String account1_mnemonic = "insert mnemonic";
        Account account1 = new Account(account1_mnemonic);
        System.out.println("Account1: " + account1.getAddress());

        // Create the asset
        // get changing network parameters for each transaction
        TransactionParametersResponse params = client.TransactionParams().execute().body();

        Long assetTotal = 1000000000000000L;
        String unitName = "insert unitname";
        String assetName = "insert assetname";
        String url = "insert url";
        Address manager = account1.getAddress();
        Address reserve = account1.getAddress();
        Address freeze = account1.getAddress();
        Address clawback = account1.getAddress();
        int decimals = 6;
        Transaction tx = Transaction.AssetCreateTransactionBuilder().sender(account1.getAddress()).assetTotal(assetTotal)
                .assetDecimals(decimals).assetUnitName(unitName).assetName(assetName).url(url)
                .manager(manager).reserve(reserve).freeze(freeze)
                .defaultFrozen(false).clawback(clawback).suggestedParams(params).build();

        // Sign the transaction with creator account
        SignedTransaction signedTx = account1.signTransaction(tx);
        Long assetID;
        try {
            String id = submitTransaction(signedTx);
            System.out.println("Transaction ID: " + id);
            waitForConfirmation(id);
            // Read the transaction
            PendingTransactionResponse pTrx = client.PendingTransactionInformation(id).execute().body();
            // Now that the transaction is confirmed we can get the assetID
            assetID = pTrx.assetIndex;
            System.out.println("AssetID = " + assetID);
            printCreatedAsset(account1, assetID);
            printAssetHolding(account1, assetID);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {
        CreateASA ex = new CreateASA();
        ex.createAsset();

    }

}
