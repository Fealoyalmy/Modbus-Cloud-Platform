package edu.ahau.jsjds.iot;

import java.util.Arrays;

/**
 * modbus-rtu 指令工具类
 */
public class ModbusDirectHelper {

	/**
	 * 功能码：读保持寄存器
	 */
	public static final int OP_READ_HOLDING_REGISTER = 0x03;
	/**
	 * 功能码：设备注册
	 */
	public static final int OP_DEVICE_REGISTER = 0x41;
	/**
	 * 功能码：设备心跳
	 */
	public static final int OP_DEVICE_HEARTBEAT = 0x42;
	/**
	 * 功能码：写单个寄存器
	 */
	public static final int WRITE_SINGLE_REGISTER = 0x06;

	private ModbusDirectHelper() {
	}

	/**
	 * 打包一个读保持寄存器的指令数据包，结果可以直接发送
	 * 
	 * @param addr
	 *            传感器地址（1~255）
	 * @param offset
	 *            寄存器起始地址（0~65535）
	 * @param count
	 *            要读取的寄存器个数（0~65535）
	 * @return 指令数据打包
	 */
	public static byte[] readHoldingRegister(int addr, int offset, int count) {
		byte[] packet = new byte[8];// 固定8字节，1地址1功能2起始地址2数据个数2校验
		packet[0] = (byte) (addr & 0xff);
		packet[1] = (byte) (OP_READ_HOLDING_REGISTER & 0xff);// 功能码固定为3
		packet[2] = (byte) ((offset >> 8) & 0xff);
		packet[3] = (byte) (offset & 0xff);
		packet[4] = (byte) ((count >> 8) & 0xff);
		packet[5] = (byte) (count & 0xff);
		
		byte[] src = Crc16.getCrc16(Arrays.copyOfRange(packet, 0, packet.length - 2));
		packet[packet.length - 2] = (byte) (src[0]);// 存入低/高位
		packet[packet.length - 1] = (byte) (src[1]);
		
		Server.jiankong+="服务端下发申请数据包...\n";
		for (int j = 0; j < packet.length; j++) {
			Server.jiankong+=Util.byte2HexString(packet[j])+" ";	
		}
		Server.jiankong+="\n";
		
		return packet;
	}

	/**
	 * 打包一个客户端注册的指令数据包，结果可以直接发送
	 * 
	 * @param deviceId
	 * @return
	 */
	public static byte[] deviceRegister(int deviceId, long hb) 
	{
		byte[] packet = new byte[7];// 固定7字节，1地址1功能1数据长度2数据2校验
		packet[0] = (byte) (deviceId & 0xff);
		packet[1] = (byte) (OP_DEVICE_REGISTER & 0xff);// 功能码固定为0x41
		packet[2] = (byte) (0x02 & 0xff);// 长度固定为0x02
		packet[3] = (byte) ((hb >> 8) & 0xff);//(deviceId >> 8)
		packet[4] = (byte) (hb & 0xff);//deviceId & 0xff
		
		byte[] src = Crc16.getCrc16(Arrays.copyOfRange(packet, 0, packet.length - 2));
		packet[packet.length - 2] = (byte) (src[0]);// 存入低/高位
		packet[packet.length - 1] = (byte) (src[1]);
		
		Server.jiankong+="设备"+deviceId+" 上传注册数据包...\n";
		for (int j = 0; j < packet.length; j++) {
			Server.jiankong+=Util.byte2HexString(packet[j])+" ";	
		}
		Server.jiankong+="\n";
		
		return packet;
	}

	/**
	 * 打包一个客户端心跳的指令数据包，结果可以直接发送
	 * 
	 * @param deviceId
	 * @return
	 */
	public static byte[] deviceHeartbeat(int deviceId) {
		byte[] packet = new byte[7];// 固定7字节，1地址1功能1数据长度2数据2校验
		packet[0] = (byte) (0x00 & 0xff);
		packet[1] = (byte) (OP_DEVICE_HEARTBEAT & 0xff);// 功能码固定为0x42
		packet[2] = (byte) (0x02 & 0xff);// 长度固定为0x02
		packet[3] = (byte) ((deviceId >> 8) & 0xff);
		packet[4] = (byte) (deviceId & 0xff);
		
		byte[] src = Crc16.getCrc16(Arrays.copyOfRange(packet, 0, packet.length - 2));
		packet[packet.length - 2] = (byte) (src[0]);// 存入低/高位
		packet[packet.length - 1] = (byte) (src[1]);
		
		Server.jiankong+="设备"+deviceId+" 上传心跳数据包...\n";
		for (int j = 0; j < packet.length; j++) {
			Server.jiankong+=Util.byte2HexString(packet[j])+" ";	
		}
		Server.jiankong+="\n";
		return packet;
	}
	
	/**
	 * 打包一个写单个寄存器的指令数据包，指令可以直接发送
	 * 
	 * @param addr 传感器地址（1~255）
	 * @param count 要写入的数据个数（0~65535）
	 * @param data 要写入的操作数据
	 * @return 指令数据打包
	 */
	public static byte[] writeSingleRegister(int addr, int count, byte[] data) {
		// socket.getOutputStream().write(ModbusDirectHelper.writeSingleRegister(Server.addr, 1, HEART_BEAT_INTERVAL));
		byte[] packet = new byte[8];// 固定8字节，1地址1功能2数据长度2数据个数2校验
		packet[0] = (byte) (addr & 0xff);
		packet[1] = (byte) (WRITE_SINGLE_REGISTER & 0xff);// 功能码固定为6
		packet[2] = (byte) ((count >> 8) & 0xff);
		packet[3] = (byte) (count & 0xff);
		for(int i = 4; i < 4 + count * 2; i++) {
			packet[i] = data[i - 4]; //(byte) ((data >> 8) & 0xff);
			//packet[5] = (byte) (data & 0xff);
		}		
		byte[] src = Crc16.getCrc16(Arrays.copyOfRange(packet, 0, packet.length - 2));
		packet[packet.length - 2] = (byte) (src[0]);// 存入低/高位
		packet[packet.length - 1] = (byte) (src[1]);
		
		Server.jiankong+="服务端下发写控制数据包...\n";
		for (int j = 0; j < packet.length; j++) {
			Server.jiankong+=Util.byte2HexString(packet[j])+" ";	
		}
		Server.jiankong+="\n";
		
		return packet;
	}

}
