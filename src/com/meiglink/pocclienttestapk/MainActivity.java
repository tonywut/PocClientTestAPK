package com.meiglink.pocclienttestapk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
	private final int MSG_UPDATE_CALIBRATION = 1;
	private final int MSG_UPDATE_SENDTIME = 2;
	private final int MSG_UPDATE_RECVTIME = 3;
	private final int MSG_TEST_TIME = 4;
	private final int MSG_UPDATE_AVER_SENDTIME = 5;
	private final int MSG_UPDATE_AVER_RECVTIME = 6;

	private TextView TV_calibration;
	private TextView TV_testtimes;
	private TextView TV_sendtime;
	private TextView TV_recvtime;
	private TextView TV_aver_sendtime;
	private TextView TV_aver_recvtime;
	private int testtimes = 200;
	String NTPserver = "it158.xicp.net";
	String PocServer = "192.168.43.233";
	int portnumber = 8888;

	Socket s = null;
	DataOutputStream dos = null;
	DataInputStream dis = null;
	private boolean bConnected = false;
	private long timeOffset = 0;
	private long average_sendtime = 0;
	private long average_recvtime = 0;

	Thread tSocketThread = new Thread(new socketThread());
	Thread tHeartbeat = new Thread(new HeartbeatPackageThread());
	Thread tRecv = new Thread(new RecvThread());

	String syncLock = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TV_calibration = (TextView) findViewById(R.id.TV_calibration);
		TV_testtimes = (TextView) findViewById(R.id.TV_testtimes);
		TV_testtimes.setText(getString(R.string.test_times, 0) + testtimes);
		TV_sendtime = (TextView) findViewById(R.id.TV_sendtime);
		TV_recvtime = (TextView) findViewById(R.id.TV_recvtime);
		TV_aver_sendtime = (TextView) findViewById(R.id.TV_sendtime_average);
		TV_aver_recvtime = (TextView) findViewById(R.id.TV_recvtime_average);
		Thread tMainThread = new Thread(new MainThread());
		tMainThread.start();
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
		disconnect();
		super.onPause();
	}

	public void disconnect() {
		try {
			bConnected = false;

			tSocketThread.interrupt();
			tHeartbeat.interrupt();
			tRecv.interrupt();

			if (dos != null)
				dos.close();
			if (dis != null)
				dis.close();
			if (s != null)
				s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class MainThread implements Runnable {
		public void run() {
			CountDownLatch latch = new CountDownLatch(1);
			Thread t_getTimeOffset = new getTimeOffsetThread(latch);
			t_getTimeOffset.start();
			try {
				latch.await();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("calibration is end");
			connect(portnumber);
			Thread tSocketThread = new Thread(new socketThread());
			tSocketThread.start();

			Thread tHeartbeat = new Thread(new HeartbeatPackageThread());
			tHeartbeat.start();

			Thread tRecv = new Thread(new RecvThread());
			tRecv.start();
		}
	}

	public void connect(int port) {
		try {
			s = new Socket(PocServer, port);
			dos = new DataOutputStream(s.getOutputStream());
			dis = new DataInputStream(s.getInputStream());
			System.out.println("~~~~connect success~~~~~!");
			bConnected = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class socketThread implements Runnable {
		public void run() {
			int count = 0;
			while (bConnected && count < testtimes) {
				try {
					System.out.println("send start ...");
					synchronized (syncLock) {
						long sendStartTime = new Date().getTime() - timeOffset;
						if (bConnected) {
							dos.writeChar('T');
							dos.flush();
							dos.writeLong(sendStartTime);
							dos.flush();
							dos.writeUTF( (count+1) + "/" + testtimes);
							dos.flush();
						}
					}
					System.out.println("send end ...");
					Thread.sleep(1000);
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				count++;
			}

		}

	}

	private class RecvThread implements Runnable {
		public void run() {
			while (bConnected) {
				try {
					char event = dis.readChar();
					if (event == 'T') {
						long sendStartTime = dis.readLong();
						long sendEndTime = dis.readLong();
						long recvStartTime = dis.readLong();
						String str = dis.readUTF();
						long recvEndTime = new Date().getTime() - timeOffset;
						System.out.println("recv End...");
						long sendtime = sendEndTime - sendStartTime;
						long recvtime = recvEndTime - recvStartTime;
						if (average_sendtime == 0) {
							average_sendtime = sendtime;
						} else {
							average_sendtime = (average_sendtime + sendtime ) /2;
						}
						if (average_recvtime == 0) {
							average_recvtime = recvtime;
						} else {
							average_recvtime = (average_recvtime + recvtime) / 2;
						}
						System.out.println("sendtime:" + sendtime + " recvtime:" + recvtime + 
								" average_sendtime" + average_sendtime + " average_recvtime" + average_recvtime);
						Message msg = new Message();
						msg.what = MSG_UPDATE_SENDTIME;
						msg.obj = sendtime;
						mHandler.sendMessage(msg);
						Message msg2 = new Message();
						msg2.what = MSG_UPDATE_RECVTIME;
						msg2.obj = recvtime;
						mHandler.sendMessage(msg2);
						Message msg3 = new Message();
						msg3.what = MSG_TEST_TIME;
						msg3.obj = str;
						mHandler.sendMessage(msg3);
						Message msg4 = new Message();
						msg4.what = MSG_UPDATE_AVER_SENDTIME;
						msg4.obj = average_sendtime;
						mHandler.sendMessage(msg4);
						Message msg5 = new Message();
						msg5.what = MSG_UPDATE_AVER_RECVTIME;
						msg5.obj = average_recvtime;
						mHandler.sendMessage(msg5);
					} else {
						String str = dis.readUTF();
						System.out.println(str);
					}
					Thread.sleep(0);
				} catch (SocketException e) {
					 e.printStackTrace();
					System.out.println("exit, bye!");
					return;
				} catch (EOFException e) {
					 e.printStackTrace();
					System.out.println("exit, bye!");
					return;
				} catch (IOException e) {
					 e.printStackTrace();
					return;
				} catch (InterruptedException e) {
					 e.printStackTrace();
					// thread.interrupt()执行后会立刻进入catch
					return;
				}
			}
		}
	}

	private class HeartbeatPackageThread implements Runnable {
		public void run() {
			while (bConnected) {
				try {
					Thread.sleep(5000);
					synchronized (syncLock) {
						if (bConnected) {
							dos.writeChar('H');
							dos.flush();
							dos.writeUTF("hearbeat");
							dos.flush();
						}
					}
				} catch (InterruptedException e) {
					// e.printStackTrace();
					// thread.interrupt()执行后会立刻进入catch
					return;
				} catch (IOException e1) {
					e1.printStackTrace();

				}
			}
		}
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			// 更新UI
			switch (msg.what) {
			case MSG_UPDATE_CALIBRATION:
				if (msg.obj != null) {
					String msgstr = (String) msg.obj;
					TV_calibration.setText(msgstr);
				}
				break;
			case MSG_UPDATE_SENDTIME:
				if (msg.obj != null) {
					long time = (long) msg.obj;
					TV_sendtime.setText(getString(R.string.send_time) + time);
				}
				break;
			case MSG_UPDATE_RECVTIME:
				if (msg.obj != null) {
					long time = (long) msg.obj;
					TV_recvtime.setText(getString(R.string.recv_time) + time);
				}
				break;
			case MSG_TEST_TIME:
				if (msg.obj != null) {
					String msgstr2 = (String) msg.obj;
					TV_testtimes.setText(getString(R.string.test_times, 0) + msgstr2);
				}
				break;
			case MSG_UPDATE_AVER_SENDTIME:
				if (msg.obj != null) {
					long time = (long) msg.obj;
					TV_aver_sendtime.setText(getString(R.string.average, 0) + time);
				}
				break;
			case MSG_UPDATE_AVER_RECVTIME:
				if (msg.obj != null) {
					long time = (long) msg.obj;
					TV_aver_recvtime.setText(getString(R.string.average, 0) + time);
				}
			}
		};
	};

	// localtime - NTPservicetime
	private class getTimeOffsetThread extends Thread {
		CountDownLatch latch;

		public getTimeOffsetThread(CountDownLatch latch) {
			this.latch = latch;
		}

		public void run() {
			TV_calibration.setText(getString(R.string.calibration_time));
			long offsetCount = 0;
			int count = 0;
			for (int i = 0; i < 10; i++) {
				try {
					NTPUDPClient timeClient = new NTPUDPClient();
					InetAddress timeServerAddress = InetAddress.getByName(NTPserver);
					TimeInfo timeInfo = timeClient.getTime(timeServerAddress);
					TimeStamp timeStamp = timeInfo.getMessage().getTransmitTimeStamp();
					Date date = timeStamp.getDate();
					Date localdate = new Date();
					offsetCount += localdate.getTime() - date.getTime();
					count++;
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			timeOffset = offsetCount / count;
			if (timeOffset == 0) {
				System.out.println("don't get timeoffset");
			}
			System.out.println("timeOffset:" + timeOffset);
			// 实例化message对象
			Message msg = new Message();
			// 给message对象赋值
			msg.what = MSG_UPDATE_CALIBRATION;
			msg.obj = getString(R.string.time_offset) + timeOffset;
			// 发送message值给Handler接收
			mHandler.sendMessage(msg);
			latch.countDown();// 工人完成工作，计数器减一
		}
	}
}
