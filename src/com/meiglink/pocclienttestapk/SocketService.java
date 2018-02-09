package com.meiglink.pocclienttestapk;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import com.meiglink.MsgPackage;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class SocketService extends Service {

	public static final String TAG = "SocketService";
	String NTPserver = "it158.xicp.net";
	// String NTPserver = "192.168.1.99";
	//String PocServer = "192.168.43.233";
	// String PocServer = "192.168.0.105";
	String PocServer = "it158.xicp.net";
	//String PocServer = "192.168.1.99";
	private int portnumber = 8888;
	private int testtimes = 200;

	Socket s = null;
	ObjectOutputStream dos = null;
	ObjectInputStream dis = null;
	private boolean bConnected = false;
	private long timeOffset = 0;
	private long average_sendtime = 0;
	private long average_recvtime = 0;

	Thread tSocketThread = new Thread(new socketThread());
	Thread tHeartbeat = new Thread(new HeartbeatPackageThread());
	Thread tRecv = new Thread(new RecvThread());

	String syncLock = "";
	CountDownLatch latch;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate() executed");

		Thread tMainThread = new Thread(new MainThread());
		tMainThread.start();

		BroadcastReceiver receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				Log.d(TAG, "Receiver");
				int action = intent.getIntExtra("action", 0);
				switch (action) {
				case Common.ACTION_START_SEND:
					try {
						latch.await();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Thread tSocketThread = new Thread(new socketThread());
					tSocketThread.start();
					break;
				}
			}
		};

		IntentFilter socketMsgFilter = new IntentFilter("com.meiglink.pocclienttestapk.activitymsg");
		registerReceiver(receiver, socketMsgFilter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand() executed");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		disconnect();
		super.onDestroy();
		Log.d(TAG, "onDestroy() executed");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	private class MainThread implements Runnable {
		public void run() {
			latch = new CountDownLatch(1);// 线程同步
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

			Thread tHeartbeat = new Thread(new HeartbeatPackageThread());
			tHeartbeat.start();

			Thread tRecv = new Thread(new RecvThread());
			tRecv.start();
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
							MsgPackage msg = new MsgPackage(MsgPackage.MSG_SENDTIME, sendStartTime, (count + 1) + "/" + testtimes);
							dos.writeObject(msg);
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
					MsgPackage msg = null;
					Object obj = dis.readObject();
					if(obj != null) {
						msg = (MsgPackage) obj;
					}
					System.out.println("recv End...");
					if (msg!= null && msg.getMsgType() == MsgPackage.MSG_RECVTIME)
					{
						long sendtime = msg.getSendEndTime() - msg.getSendStartTime();
						long recvEndTime = new Date().getTime() - timeOffset;
						long recvtime = recvEndTime - msg.getRecvStartTime();
						if (average_sendtime == 0) {
							average_sendtime = sendtime;
						} else {
							average_sendtime = (average_sendtime + sendtime) / 2;
						}
						if (average_recvtime == 0) {
							average_recvtime = recvtime;
						} else {
							average_recvtime = (average_recvtime + recvtime) / 2;
						}

						System.out.println("sendtime:" + sendtime + " recvtime:" + recvtime + " average_sendtime"
								+ average_sendtime + " average_recvtime" + average_recvtime);
						Intent broadIntent = new Intent("com.meiglink.pocclienttestapk.socketmsg");
						broadIntent.putExtra("msgtype", Common.MSG_TEST_TIME);
						broadIntent.putExtra("msg", getString(R.string.test_times) + msg.getStr());
						sendBroadcast(broadIntent);
						broadIntent.putExtra("msgtype", Common.MSG_UPDATE_SENDTIME);
						broadIntent.putExtra("msg", getString(R.string.send_time) + sendtime);
						sendBroadcast(broadIntent);
						broadIntent.putExtra("msgtype", Common.MSG_UPDATE_RECVTIME);
						broadIntent.putExtra("msg", getString(R.string.recv_time) + recvtime);
						sendBroadcast(broadIntent);
						broadIntent.putExtra("msgtype", Common.MSG_UPDATE_AVER_SENDTIME);
						broadIntent.putExtra("msg", getString(R.string.average) + average_sendtime);
						sendBroadcast(broadIntent);
						broadIntent.putExtra("msgtype", Common.MSG_UPDATE_AVER_RECVTIME);
						broadIntent.putExtra("msg", getString(R.string.average) + average_recvtime);
						sendBroadcast(broadIntent);
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
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
							MsgPackage msg = new MsgPackage(MsgPackage.MSG_HEARBEAT, "hearbeat");
							dos.writeObject(msg);
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

	public void connect(int port) {
		try {
			s = new Socket(PocServer, port);
			dos = new ObjectOutputStream(s.getOutputStream());
			dis = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
			System.out.println("~~~~connect success~~~~~!");
			bConnected = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

	// localtime - NTPservicetime
	private class getTimeOffsetThread extends Thread {
		CountDownLatch latch;

		public getTimeOffsetThread(CountDownLatch latch) {
			this.latch = latch;
		}

		public void run() {
			// TV_calibration.setText(getString(R.string.calibration_time));
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
			Intent broadIntent = new Intent("com.meiglink.pocclienttestapk.socketmsg");
			broadIntent.putExtra("msgtype", Common.MSG_UPDATE_CALIBRATION);
			broadIntent.putExtra("msg", getString(R.string.time_offset) + timeOffset);
			sendBroadcast(broadIntent);
			latch.countDown();// 工人完成工作，计数器减一
		}
	}

}
