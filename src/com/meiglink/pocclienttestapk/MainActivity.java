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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	String Tag = "PocClientTest activity";
	private TextView TV_calibration;
	private TextView TV_testtimes;
	private TextView TV_sendtime;
	private TextView TV_recvtime;
	private TextView TV_aver_sendtime;
	private TextView TV_aver_recvtime;
	private Button mButton;
	private Intent mIntent;

	String NTPserver = "it158.xicp.net";
	// String NTPserver = "192.168.1.99";
	String PocServer = "192.168.43.233";
	// String PocServer = "192.168.0.105";
	//String PocServer = "it158.xicp.net";
	//String PocServer = "192.168.1.99";
	BroadcastReceiver receiver;
	private boolean mReceiverTag = false;   //广播接受者标识

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(Tag, "MainActivity create");
		setContentView(R.layout.activity_main);
		TV_calibration = (TextView) findViewById(R.id.TV_calibration);
		TV_testtimes = (TextView) findViewById(R.id.TV_testtimes);
		TV_testtimes.setText(getString(R.string.test_times, 0));
		TV_sendtime = (TextView) findViewById(R.id.TV_sendtime);
		TV_recvtime = (TextView) findViewById(R.id.TV_recvtime);
		TV_aver_sendtime = (TextView) findViewById(R.id.TV_sendtime_average);
		TV_aver_recvtime = (TextView) findViewById(R.id.TV_recvtime_average);
		
		ButtonListener b = new ButtonListener();
		mButton = (Button) findViewById(R.id.button1);
		mButton.setOnClickListener(b);
		mButton.setOnTouchListener(b);

		mIntent = new Intent(this, SocketService.class);

		receiver = new BroadcastReceiver() {

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

		if (!mReceiverTag){     //在注册广播接受者的时候 判断是否已被注册,避免重复多次注册广播
		IntentFilter socketMsgFilter = new IntentFilter("com.meiglink.pocclienttestapk.socketmsg");
        mReceiverTag = true;    //标识值 赋值为 true 表示广播已被注册
		registerReceiver(receiver, socketMsgFilter);
		}
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
        if (mReceiverTag) {   //判断广播是否注册
                mReceiverTag = false;   //Tag值 赋值为false 表示该广播已被注销
                this.unregisterReceiver(receiver);   //注销广播
        }
		super.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	class ButtonListener implements OnClickListener, OnTouchListener {
		public void onClick(View v) {
			if(v.getId() ==  R.id.button1) {
				Log.d(Tag, "button ----> click");
			}
		}
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if(v.getId()==R.id.button1) {
				if(event.getAction() ==MotionEvent.ACTION_UP) {
					Log.d(Tag, "button ---> up");
					Intent broadIntent3 = new Intent("com.meiglink.pocclienttestapk.activitymsg");
					broadIntent3.putExtra("action", Common.ACTION_STOP_RECORD);
					sendBroadcast(broadIntent3);
				}
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					Log.d(Tag, "button ---> down");
					Intent broadIntent2 = new Intent("com.meiglink.pocclienttestapk.activitymsg");
					broadIntent2.putExtra("action", Common.ACTION_START_RECORD);
					sendBroadcast(broadIntent2);
				}
			}
			return false;
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_MENU:
		case KeyEvent.KEYCODE_DPAD_UP:
			startService(mIntent);
			return true;
		case KeyEvent.KEYCODE_VOLUME_UP:
			Intent broadIntent = new Intent("com.meiglink.pocclienttestapk.activitymsg");
			broadIntent.putExtra("action", Common.ACTION_START_SEND);
			sendBroadcast(broadIntent);
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if(event.getAction() == KeyEvent.ACTION_DOWN) {
				Intent broadIntent2 = new Intent("com.meiglink.pocclienttestapk.activitymsg");
				broadIntent2.putExtra("action", Common.ACTION_START_RECORD);
				sendBroadcast(broadIntent2);
			}else if (event.getAction() == KeyEvent.ACTION_UP) {
				Intent broadIntent3 = new Intent("com.meiglink.pocclienttestapk.activitymsg");
				broadIntent3.putExtra("action", Common.ACTION_STOP_RECORD);
				sendBroadcast(broadIntent3);
			}
			return false;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			stopService(mIntent);
			return true;
		}
		return super.onKeyDown(keyCode, event);

	}

}
