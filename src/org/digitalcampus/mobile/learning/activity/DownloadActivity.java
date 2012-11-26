package org.digitalcampus.mobile.learning.activity;

import java.util.ArrayList;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.mobile.learning.adapter.DownloadListAdapter;
import org.digitalcampus.mobile.learning.application.DbHelper;
import org.digitalcampus.mobile.learning.application.MobileLearning;
import org.digitalcampus.mobile.learning.listener.GetModuleListListener;
import org.digitalcampus.mobile.learning.model.Lang;
import org.digitalcampus.mobile.learning.model.Module;
import org.digitalcampus.mobile.learning.task.GetModuleListTask;
import org.digitalcampus.mobile.learning.task.Payload;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.ListView;

import com.bugsense.trace.BugSenseHandler;

public class DownloadActivity extends Activity implements GetModuleListListener {

	public static final String TAG = "DownloadActivity";

	private ProgressDialog pDialog;
	private JSONObject json;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);
		// Get Module list
		getModuleList();

	}

	private void getModuleList() {
		// show progress dialog
		pDialog = new ProgressDialog(this);
		pDialog.setTitle(R.string.loading);
		pDialog.setMessage(getString(R.string.loading_module_list));
		pDialog.setCancelable(true);
		pDialog.show();

		GetModuleListTask task = new GetModuleListTask(this);
		Payload p = new Payload(0,null);
		task.setGetModuleListListener(this);
		task.execute(p);
	}

	public void refreshModuleList() {
		// process the response and display on screen in listview
		// Create an array of Modules, that will be put to our ListActivity

		DbHelper db = new DbHelper(this);
		try {
			ArrayList<Module> modules = new ArrayList<Module>();
			
			for (int i = 0; i < (json.getJSONArray("modules").length()); i++) {
				JSONObject json_obj = (JSONObject) json.getJSONArray("modules").get(i);
				Module dm = new Module();
				// TODO LANG
				ArrayList<Lang> titles = new ArrayList<Lang>();
				Lang l = new Lang("en",json_obj.getString("title"));
				titles.add(l);
				dm.setTitles(titles);
				dm.setShortname(json_obj.getString("shortname"));
				dm.setVersionId(json_obj.getDouble("version"));
				dm.setDownloadUrl(json_obj.getString("url"));
				dm.setInstalled(db.isInstalled(dm.getShortname()));
				dm.setToUpdate(db.toUpdate(dm.getShortname(), dm.getVersionId()));
				modules.add(dm);
			}
			

			DownloadListAdapter mla = new DownloadListAdapter(this, modules);
			ListView listView = (ListView) findViewById(R.id.module_list);
			listView.setAdapter(mla);

		} catch (Exception e) {
			e.printStackTrace();
			BugSenseHandler.log(TAG, e);
			MobileLearning.showAlert(this, R.string.loading, R.string.error_processing_response);
		}
		db.close();

	}

	public void moduleListComplete(Payload response) {
		// close dialog and process results
		pDialog.dismiss();
		if(response.result){
			try {
				json = new JSONObject(response.resultResponse);
				refreshModuleList();
			} catch (JSONException e) {
				BugSenseHandler.log(TAG, e);
				MobileLearning.showAlert(this, R.string.loading, R.string.error_connection);
				e.printStackTrace();
			}
		} else {
			MobileLearning.showAlert(this, R.string.loading, response.resultResponse);
		}

	}

}
