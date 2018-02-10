package com.meiglink.pocclienttestapk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	String Tag = "PocClientTest activity";
	private TextView TV_calibration;
	private TextView TV_testtimes;
	private TextView TV_sendtime;
	private TextView TV_recvtime;
	private TextView TV_aver_sendtime;
	private TextView TV_aver_recvtime;
	private EditText ET_NTP_service;
	private EditText ET_SERVICE;
	private Intent mIntent;

	String NTPserver = "it158.xicp.net";
	// String NTPserver = "192.168.1.99";
	//String PocServer = "192.168.43.233";
	// String PocServer = "192.168.0.105";
	String PocServer = "it158.xicp.net";
	//String PocServer = "192.168.1.99";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TV_calibration = (TextView) findViewById(R.id.TV_calibration);
		TV_testtimes = (TextView) findViewById(R.id.TV_testtimes);
		TV_testtimes.setText(getString(R.string.test_times, 0));
		TV_sendtime = (TextView) findViewById(R.id.TV_sendtime);
		TV_recvtime = (TextView) findViewById(R.id.TV_recvtime);
		TV_aver_sendtime = (TextView) findViewById(R.id.TV_sendtime_average);
		TV_aver_recvtime = (TextView) findViewById(R.id.TV_recvtime_average);
		ET_NTP_service = (EditText) findViewById(R.id.editText_NTP);
		ET_NTP_service.setText(NTPserver);
		ET_SERVICE = (EditText) findViewById(R.id.editText_Service);
		ET_SERVICE.setText(PocServer);

		mIntent = new Intent(this, SocketService.class);

		BroadcastReceiver receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				Log.d(Tag, "Receiver");
				int msgtype = intent.getIntExtra("msgtype", 0);
				String str = intent.getStringExtra("msg");
				switch (msgtype) {
				case Common.MSG_UPDATE_CALIBRATION:
					TV_calibration.setText(str);
					break;
				case Common.MSG_TEST_TIME:
					TV_testtimes.setText(str);
					break;
				case Common.MSG_UPDATE_SENDTIME:
					TV_sendtime.setText(str);
					break;
				case Common.MSG_UPDATE_RECVTIME:
					TV_recvtime.setText(str);
					break;
				case Common.MSG_UPDATE_AVER_SENDTIME:
					TV_aver_sendtime.setText(str);
					break;
				case Common.MSG_UPDATE_AVER_RECVTIME:
					TV_aver_recvtime.setText(str);
					break;
				}
			}

		};

		IntentFilter socketMsgFilter = new IntentFilter("com.meiglink.pocclienttestapk.socketmsg");
		registerReceiver(receiver, socketMsgFilter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU:
		case KeyEvent.KEYCODE_DPAD_UP:
			mIntent.putExtra("ntpserver", ET_NTP_service.getText().toString());
			mIntent.putExtra("serviceIP", ET_NTP_service.getText().toString());
			startService(mIntent);
			return true;
		case KeyEvent.KEYCODE_VOLUME_UP:
			Intent broadIntent = new Intent("com.meiglink.pocclienttestapk.activitymsg");
			broadIntent.putExtra("action", Common.ACTION_START_SEND);
			sendBroadcast(broadIntent);
			return true;

		case KeyEvent.KEYCODE_VOLUME_DOWN:
			stopService(mIntent);
			return true;
		}
		return super.onKeyDown(keyCode, event);

	}

}
