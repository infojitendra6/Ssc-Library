package com.kopykitab.ereader;

/**
 * Created by ramesh on 19/7/17.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.kopykitab.ereader.components.CircularProgressView;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StreamsSelectionActivity extends AppCompatActivity {

    private class StreamsAdapter extends BaseAdapter {

        private Context mContext;
        private List<HashMap<String, String>> allStreams;

        public StreamsAdapter(Context context, List<HashMap<String, String>> streams) {
            mContext = context;
            this.allStreams = streams;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return allStreams.size();
        }

        @Override
        public HashMap<String, String> getItem(int position) {
            // TODO Auto-generated method stub
            return allStreams.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.streams_selection_item, parent, false);
            }
            final HashMap<String, String> userData = getItem(position);
            ((TextView) convertView.findViewById(R.id.stream_name)).setText(userData.get("text"));

            final CheckBox streamSelectionYesNo = (CheckBox) convertView.findViewById(R.id.stream_selection_yes_no);
            streamSelectionYesNo.setChecked(Boolean.valueOf(userData.get("assigned")));
            if (Boolean.parseBoolean(userData.get("always_selected"))) {
                streamSelectionYesNo.setChecked(true);
                selectedStreams.add(userData.get("login_source"));
            }
            streamSelectionYesNo.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    if (Boolean.parseBoolean(userData.get("always_selected"))) {
                        streamSelectionYesNo.setChecked(true);
                        selectedStreams.add(userData.get("login_source"));
                    }
                    if (streamSelectionYesNo.isChecked()) {
                        selectedStreams.add(userData.get("login_source"));
                    } else {
                        selectedStreams.remove(userData.get("login_source"));
                    }
                    allStreams.get(position).put("assigned", Boolean.toString(streamSelectionYesNo.isChecked()));
                }
            });

            return convertView;
        }
    }

    private ViewFlipper streamsFlip;
    private ListView streamsListView;
    private StreamsAdapter streamsAdapter;
    private String customerId;
    private Set<String> selectedStreams = new HashSet<String>();

    private String screenName = "StreamsSelection";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        customerId = AppSettings.getInstance(this).get("CUSTOMER_ID");

        setContentView(R.layout.streams_selection);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.back_button);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>" + getResources().getString(R.string.streams_label) + "</font>"));
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.action_bar_background));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.action_bar_background_dark));
        }

        streamsFlip = (ViewFlipper) findViewById(R.id.streams_flip);
        if (Utils.isNetworkConnected(StreamsSelectionActivity.this)) {
            streamsFlip.setDisplayedChild(0);

            streamsListView = (ListView) findViewById(R.id.streams_list);

            new GetStreams().execute();
        } else {
            streamsFlip.setDisplayedChild(2);
        }
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        super.onBackPressed();

        if (selectedStreams.size() > 0) {
            AppSettings.getInstance(StreamsSelectionActivity.this).set("STREAMS_CONFIGURED", "1");
        }

        finish();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Utils.triggerScreen(StreamsSelectionActivity.this, screenName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void OnStreamsSubmitClick(View v) {
        if (selectedStreams.size() <= 0) {
            Constants.showToast("Select atleast one Stream", StreamsSelectionActivity.this);
        } else {
            new UpdateStreams().execute();
        }
    }

    private class GetStreams extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            CircularProgressView streamsProgressBar = (CircularProgressView) findViewById(R.id.streams_progress);
            if (streamsProgressBar.getVisibility() == View.GONE) {
                streamsProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub
            String response = null;

            try {
                response = Utils.sendPost(StreamsSelectionActivity.this, Constants.STREAMS_URL, "customer_id=" + URLEncoder.encode(customerId, "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            try {
                JSONArray jsonArray = new JSONArray(result);
                if (jsonArray.length() > 0) {
                    List<HashMap<String, String>> streamsList = new ArrayList<HashMap<String, String>>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject tempJsonObject = jsonArray.getJSONObject(i);
                        HashMap<String, String> temp = new HashMap<String, String>();
                        temp.put("text", tempJsonObject.getString("text"));
                        temp.put("login_source", tempJsonObject.getString("login_source"));
                        if (tempJsonObject.has("always_selected") && tempJsonObject.getBoolean("always_selected")) {
                            temp.put("always_selected", Boolean.toString(tempJsonObject.getBoolean("always_selected")));
                        } else {
                            temp.put("always_selected", Boolean.toString(false));
                        }
                        if (tempJsonObject.has("assigned") && tempJsonObject.getBoolean("assigned")) {
                            temp.put("assigned", Boolean.toString(tempJsonObject.getBoolean("assigned")));
                            selectedStreams.add(tempJsonObject.getString("login_source"));
                        } else {
                            temp.put("assigned", Boolean.toString(false));
                        }

                        streamsList.add(temp);
                    }
                    streamsAdapter = new StreamsAdapter(StreamsSelectionActivity.this, streamsList);
                    streamsListView.setAdapter(streamsAdapter);
                    streamsListView.setOnItemClickListener(new OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // TODO Auto-generated method stub

                            CheckBox userAttendenceYesNo = (CheckBox) view.findViewById(R.id.stream_selection_yes_no);
                            userAttendenceYesNo.performClick();
                        }
                    });
                } else {
                    streamsFlip.setDisplayedChild(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            CircularProgressView streamsProgressBar = (CircularProgressView) findViewById(R.id.streams_progress);
            streamsProgressBar.setVisibility(View.GONE);
        }
    }

    private class UpdateStreams extends AsyncTask<Void, Void, String> {

        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            pDialog = new ProgressDialog(StreamsSelectionActivity.this);
            pDialog.setMessage("Updating... Please Wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub

            String postParams = "customer_id=" + customerId + "&login_source=" + Constants.LOGIN_SOURCE;
            for (String stream : selectedStreams) {
                postParams += "&streams[]=" + stream;
            }

            String response = null;
            try {
                response = Utils.sendPost(StreamsSelectionActivity.this, Constants.ADD_UPDATE_STREAMS_URL, postParams);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            try {
                JSONObject jsonObject = new JSONObject(result);
                String statusMessage = jsonObject.getString("message");

                boolean status = jsonObject.getBoolean("status");
                if (status) {
                    pDialog.dismiss();
                    AppSettings.getInstance(StreamsSelectionActivity.this).set("STREAMS_CONFIGURED", "1");
                    Utils.showLibraryAndSyncData(StreamsSelectionActivity.this);
                    onBackPressed();
                } else {
                    Constants.showToast(statusMessage, StreamsSelectionActivity.this);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
        }
    }
}
