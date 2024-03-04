package edu.ahau.jsjds.iot;

import edu.ahau.jsjds.iot.Util;
import edu.ahau.jsjds.iot.Server;
/**
 * 工具类
 */
public class Util {

	private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5', 
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	
	private Util() {
	}

	/**
	 * 字节数组转int，用于处理若干字节数组表达一个数字的情况
	 * 
	 * @param b
	 * @return
	 */
	public static int bytes2Int(byte[] b) {
		int r = b[b.length - 1] & 0xFF;
		for (int i = 1; i < b.length; i++) {
			r += (b[b.length - i - 1] & 0xFF) << (i << 3);
		}
		return r;
	}
	
	public static String toStandByte(Byte da) {//byte转8位二进制输出
    	String sd = Integer.toBinaryString(da);
    	while(sd.length() < 8) {
            sd = "0" + sd;
        }
    	return sd;
    }
	
	public static String byte2HexString(byte a)
	{
		char[] buf = new char[2];
		buf[0] = HEX_CHAR[a >> 4 & 0xf];
        buf[1] = HEX_CHAR[a & 0xf];
        return new String(buf);
	}
	
	
	public static String bytesToHexFun2(byte[] bytes) {
        char[] buf = new char[bytes.length * 2];
        String temp="";
        int index = 0;
        for(byte b : bytes) { // 利用位运算进行转换，可以看作方法一的变种
            buf[index++] = HEX_CHAR[b >> 4 & 0xf];
            buf[index++] = HEX_CHAR[b & 0xf];
        }
        for(int i=0;i<buf.length;i+=2)
        {
        	temp+="0x"+buf[i]+buf[i+1]+" ";
        }
        temp+="( "+Util.bytes2Int(bytes)+" )";
        return new String(temp);
	}

	
	public static byte[] switchController(String add, int humidity) {
		byte[] swc = new byte[2];
		swc[0] = 0x00; 
		if(humidity < Server.dev_thresholds.get(add) && Server.ifswitch.get(add) == false) {
			swc[0] = 0x3b;
			swc[1] = 0x01;
			Server.ifswitch.put(add, true);
			//Server.WaterSwitch = 1;
		}			//MIN_humidity
		else if(humidity > Server.dev_thresholds.get(add) && Server.ifswitch.get(add) == true) {
			swc[0] = 0x3b;
			swc[1] = 0x00;
			Server.ifswitch.put(add, false);
		}	
		return swc;
	}
		
}
