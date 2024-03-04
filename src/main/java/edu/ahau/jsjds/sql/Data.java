package edu.ahau.jsjds.sql;

import java.util.List;

public class Data {
	private int address; //地址码
	private int funcnum; //功能码
	private int datalen; //数据长度
	private List<Integer> data; //数据
	
	public int getAddress() {
		return address;
	}
	public void setAddress(int address) {
		this.address = address;
	}
	public int getFuncnum() {
		return funcnum;
	}
	public void setFuncnum(int funcnum) {
		this.funcnum = funcnum;
	}
	public int getDatalen() {
		return datalen;
	}
	public void setDatalen(int datalen) {
		this.datalen = datalen;
	}
	public List<Integer> getData() {
		return data;
	}
	public void setData(List<Integer> data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		return "Data [address=" + address + ", funcnum=" + funcnum + ", datalen=" + datalen + ", data=" + data + "]";
	}
	
	
}
