package zapsolutions.zap.channelManagement;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.JsonReader;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import me.dm7.barcodescanner.zbar.Result;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import zapsolutions.zap.R;
import zapsolutions.zap.baseClasses.BaseScannerActivity;
import zapsolutions.zap.connection.HttpClient;
import zapsolutions.zap.lightning.LightningNodeUri;
import zapsolutions.zap.lightning.LightningParser;
import zapsolutions.zap.util.ClipBoardUtil;
import zapsolutions.zap.util.RefConstants;
import zapsolutions.zap.util.Wallet;
import zapsolutions.zap.util.ZapLog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class ScanNodePubKeyActivity extends BaseScannerActivity implements LightningNodeRecyclerAdapter.LightningNodeSelectedListener {

    public static final String EXTRA_NODE_URI = "EXTRA_NODE_URI";
    private static final String LOG_TAG = ScanNodePubKeyActivity.class.getName();

    private RecyclerView mRecyclerViewPeers;
    private TextView mTextViewPeers;
    private ArrayList<LightningNodeUri> suggestedLightningNodes = new ArrayList<>();
    private LightningNodeRecyclerAdapter mAdapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        mRecyclerViewPeers = findViewById(R.id.recyclerViewPeers);
        mTextViewPeers = findViewById(R.id.textViewPeers);

        LinearLayoutManager horizontalLayoutManager
                = new LinearLayoutManager(ScanNodePubKeyActivity.this, LinearLayoutManager.HORIZONTAL, false);
        mRecyclerViewPeers.setLayoutManager(horizontalLayoutManager);

        mAdapter = new LightningNodeRecyclerAdapter(suggestedLightningNodes, this, this);
        mRecyclerViewPeers.setAdapter(mAdapter);
        mScannerInstructions.setText(R.string.scan_qr_code);

        showCameraWithPermissionRequest();

        getSuggestedPeers();
    }

    public void getSuggestedPeers() {
        JsonObjectRequest suggestedPeersRequest = new JsonObjectRequest(Request.Method.GET, RefConstants.URL_SUGGESTED_NODES, null,
                response -> {
                    try {
                        JSONObject bitcoin = response.getJSONObject("groestlcoin");
                        boolean isMainnet = !Wallet.getInstance().isTestnet();
                        JSONArray nodeArray;
                        if (isMainnet) {
                            nodeArray = bitcoin.getJSONArray("mainnet");
                        } else {
                            nodeArray = bitcoin.getJSONArray("testnet");
                        }

                        for (int i = 0; i < nodeArray.length(); i++) {
                            JSONObject jsonNode = nodeArray.getJSONObject(i);

                            LightningNodeUri node = new LightningNodeUri.Builder()
                                    .setPubKey(jsonNode.getString("pubkey"))
                                    .setHost(jsonNode.getString("host"))
                                    .setNickname(jsonNode.getString("nickname"))
                                    .setDescription(jsonNode.getString("description"))
                                    .setImage(jsonNode.getString("image"))
                                    .build();

                            suggestedLightningNodes.add(node);
                        }

                        updateSuggestedNodes();
                    } catch (JSONException e) {
                        ZapLog.debug(LOG_TAG, "Could not parse suggested peers: " + e.getMessage());
                        getSuggestedPeersGRS();
                    }
                }, error -> getSuggestedPeersGRS()/* ZapLog.debug(LOG_TAG, "Could not fetch suggested peers: " + error.getMessage())*/);

        HttpClient.getInstance().addToRequestQueue(suggestedPeersRequest, "SuggestedPeers");
    }

    public void getSuggestedPeersGRS() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("suggestedpeers.json"), "UTF-8"));

            // do reading, usually loop until end of file reading
            String mLine;
            StringBuilder json = new StringBuilder();
            while ((mLine = reader.readLine()) != null) {
                //process line
                json.append(mLine);
            }

            JSONTokener tokener = new JSONTokener(json.toString());
            JSONObject root = new JSONObject(tokener);

            JSONObject bitcoin = root.getJSONObject("groestlcoin");
            boolean isMainnet = !Wallet.getInstance().isTestnet();
            JSONArray nodeArray;
            if (isMainnet) {
                nodeArray = bitcoin.getJSONArray("mainnet");
            } else {
                nodeArray = bitcoin.getJSONArray("testnet");
            }

            for (int i = 0; i < nodeArray.length(); i++) {
                JSONObject jsonNode = nodeArray.getJSONObject(i);

                LightningNodeUri node = new LightningNodeUri.Builder()
                        .setPubKey(jsonNode.getString("pubkey"))
                        .setHost(jsonNode.getString("host"))
                        .setNickname(jsonNode.getString("nickname"))
                        .setDescription(jsonNode.getString("description"))
                        .setImage("R.drawable.ic_cloudlightning")
                        .build();

                suggestedLightningNodes.add(node);
            }

            updateSuggestedNodes();



        } catch (IOException e) {
            ZapLog.debug(LOG_TAG, "Could not parse suggested peers: " + e.getMessage());
        } catch (JSONException e) {
            ZapLog.debug(LOG_TAG, "Could not parse suggested peers: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
    }

    public void updateSuggestedNodes() {
        runOnUiThread(() -> {
            if (!suggestedLightningNodes.isEmpty()) {
                mAdapter.updateData(suggestedLightningNodes);

                mTextViewPeers.setVisibility(View.VISIBLE);
                mRecyclerViewPeers.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        mRecyclerViewPeers.setAdapter(null);
        super.onDestroy();
    }

    @Override
    public void onButtonPasteClick() {
        super.onButtonPasteClick();

        try {
            String clipboardContent = ClipBoardUtil.getPrimaryContent(getApplicationContext());
            processUserData(clipboardContent);
        } catch (NullPointerException e) {
            showError(getResources().getString(R.string.error_emptyClipboardConnect), 2000);
        }
    }

    @Override
    public void handleCameraResult(Result rawResult) {
        super.handleCameraResult(rawResult);

        if (!processUserData(rawResult.getContents())) {
            // Note:
            // * Wait 2 seconds to resume the preview.
            // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
            // * I don't know why this is the case but I don't have the time to figure out.
            Handler handler = new Handler();
            handler.postDelayed(() -> mScannerView.resumeCameraPreview(ScanNodePubKeyActivity.this), 2000);
        }
    }

    private boolean processUserData(String rawData) {
        LightningNodeUri nodeUri = LightningParser.parseNodeUri(rawData);

        if (nodeUri == null) {
            showError(getResources().getString(R.string.error_lightning_uri_invalid), 5000);
            return false;
        } else {
            return finishWithNode(nodeUri);
        }
    }

    private boolean finishWithNode(LightningNodeUri nodeUri) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_NODE_URI, nodeUri);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }

    @Override
    public void onNodeSelected(LightningNodeUri suggestedLightningNode) {
        finishWithNode(suggestedLightningNode);
    }
}
