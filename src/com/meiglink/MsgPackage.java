package com.meiglink;

public class MsgPackage implements java.io.Serializable{

	private static final long serialVersionUID = 8333134476452897853L;
	public static final int MSG_HEARBEAT = 0;
	public static final int MSG_SENDTIME = 1;
	public static final int MSG_RECVTIME = 2;
	
	private int msgType ;
	private String str;
	private long sendStartTime;
	private long sendEndTime;
	private long recvStartTime;
	
	public MsgPackage() {
		
	}
	
	public MsgPackage(int type, String str) {
		this.msgType = type;
		this.str = str;
	}	
	
	public MsgPackage(int type,long sendStartTime, String str) {
		this.msgType = type;
		this.sendStartTime = sendStartTime;
		this.str = str;
	}
		
	
	
	public MsgPackage(int type, long sendStartTime, long sendEndTime, long recvStartTime, String str) {
		this.msgType = type;
		this.sendStartTime = sendStartTime;
		this.sendEndTime = sendEndTime;
		this.recvStartTime = recvStartTime;
		this.str = str;
	}
	
	public int getMsgType() {
		return msgType;
	}
	
	public String getStr() {
		return str;
	}
	
	public long getSendStartTime() {
		return sendStartTime;
	}
	
	public long getSendEndTime() {
		return sendEndTime;
	}
	
	public long getRecvStartTime() {
		return recvStartTime;
	}
}
