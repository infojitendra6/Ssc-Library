package com.kopykitab.ereader.components;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

	private static final String TAG = NetworkChangeReceiver.class.getName();

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (Utils.isNetworkConnected(context)) {
			Log.i(TAG, "Network Available");

			String directory = Utils.getDirectory(context);
			String customerId = intent.getStringExtra("customer_id");
			if(customerId != null && !customerId.isEmpty() && !customerId.equals("")) {
				if(!directory.contains(customerId)) {
					directory += customerId + "/";
				}
			}

			List<HashMap<String, String>> gATriggeredData = new ArrayList<HashMap<String, String>>();
			try {
				final File jsonFile = new File(directory + Constants.GA_TRIGGER_OFFLINE_DATA_JSON_FILENAME);
				if (jsonFile.exists()) {
					gATriggeredData = new Gson().fromJson(new FileReader(jsonFile),new TypeToken<List<HashMap<String, String>>>() {}.getType());
				}

				if(gATriggeredData.size() > 0) {

					new AsyncTask<Void, Void, String>(){

						@Override
						protected String doInBackground(Void... params) {
							// TODO Auto-generated method stub
							String response = null;

							try {
								MultipartUtility multipart = new MultipartUtility(Constants.GA_TRIGGER_OFFLINE_URL, "UTF-8");

								multipart.addFormField("customer_id", AppSettings.getInstance(context).get("CUSTOMER_ID"));
								multipart.addFormField("login_source", Constants.LOGIN_SOURCE);

								multipart.addFilePart("offline_data", jsonFile);

								response = multipart.finish().toString();

								if (jsonFile.exists()) {
									jsonFile.delete();
								}
							} catch (Exception e) {
								e.printStackTrace();
							}

							return response;
						}

						@Override
						protected void onPostExecute(String response) {
							// TODO Auto-generated method stub
							super.onPostExecute(response);

							if(response != null) {
								Log.i("NetworkChangeReceiver URL Response", response);
							}
						}
					}.execute();
				} else {
					Log.i(TAG,"Empty Offline Data");
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.i(TAG, "Network Unavailable");
		}
	}
}