package edu.ahau.jsjds.iot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

/**
 * 一个仿真模拟客户端，随服务器端程序启动，用于仿真演示
 *
 */
public class DummyClient {
	/**
	 * 心跳时间间隔
	 */
	private static long HEART_BEAT_INTERVAL = 10 * 1000L;
	/**
	 * 异常情况下（连接不成功或意外断开）重新尝试连接的时间间隔
	 */
	private static final long RECONNECT_INTERVAL = 5 * 1000L;

	/**
	 * 服务端地址，因为本仿真客户端跟随服务器运行，所以总是本地环回地址
	 */
	private String host = "127.0.0.1";
	/**
	 * 服务器端口，以配置文件为准
	 */
	private int port;
	/**
	 * 通信管道
	 */
	private Socket client = null;
	/*
	 * 是否连接
	 */
	private static boolean connect = false;
	/**
	 * 仿真客户端的设备id
	 */
	private int deviceId = 4;
	/**
	 * 心跳线程是否执行
	 */
	private boolean dcthread_on = true;
	
	public DummyClient() {
		// 从配置文件中读取端口
		port = Config.instance().getServerPort();
	}
	
	public static void changeHeartBreak(int n)
	{
		HEART_BEAT_INTERVAL=n*1000;
	}
	
	public static long getHeartBreak()
	{
		return HEART_BEAT_INTERVAL/1000L;
	}
	
	public static boolean isConnected()
	{
		return connect;
	}

	
	/**
	 * 启动仿真客户端（非阻塞，内置子线程，上下文无需做异步处理）
	 */
	public void start() {
		dcthread_on = true;
		new Thread() {
			@Override
			public void run() {
				try {
					DummyClient.this.run();
				} catch (InterruptedException e) {
				}
			}
		}.start();
	}

	/**
	 * 运行仿真客户端的主要业务逻辑
	 * 
	 * @throws InterruptedException
	 */
	private void run() throws InterruptedException {
		// 心跳线程启动
		new HeartBeatThread().start();

		// 主循环，当连接不成功或被意外关闭，会重新尝试连接，否则会一直阻塞读取数据
		while (dcthread_on) {
			try {
				client = new Socket(host, port);
				connect = true;
				OutputStream out = client.getOutputStream();
				out.write(ModbusDirectHelper.deviceRegister(deviceId, getHeartBreak()));
				
				InputStream in = client.getInputStream();
				byte[] packet = new byte[8];
				int readBytes = 0;
				while ((readBytes = in.read(packet)) > -1) {
					// 阻塞读取服务端下发的指令，若读到-1表示服务器端主动关闭连接，退出本循环，重新尝试连接。
					int addr = packet[0] & 0xFF;// 地址1字节
					int func = packet[1] & 0xFF;// 功能码1字节
					// 解析数据包，若是读取数据的指令，那么马上发送响应，也就是上送数据，数据格式详见modbus-rtu指导文档
					if (func == ModbusDirectHelper.OP_READ_HOLDING_REGISTER) 
					{
						if(Crc16.check(packet))
						{							
							System.out.println("[client]数据校验正确");							
							System.out.println("[client]"+deviceId+"[读到指令] addr = " + addr + ", oprt = " + func);
							int count=Util.bytes2Int(Arrays.copyOfRange(packet, 4, 6));
							byte[] data=new byte[5+count*2];int len=count*2;//数据字节数
							data[0]= (byte) (deviceId & 0xff);//设备地址
							data[1]=packet[1];//功能码
							data[2]=(byte)(len & 0xff);//数据字节数
							
							for(int i=3;i<3+len;i++)
							{
								data[i]=(byte)(((int)(Math.random() * 50)) & 0xff);
							}
							
							byte[] src = Crc16.getCrc16(Arrays.copyOfRange(data, 0, data.length - 2));
							data[data.length - 2] = (byte) (src[0]);// 存入低/高位
							data[data.length - 1] = (byte) (src[1]);
							
							Server.jiankong+="设备"+deviceId+" 上传传感器数据包...\n";
							for (int j = 0; j < data.length; j++) {
								Server.jiankong+=Util.byte2HexString(data[j])+" ";	
							}
							Server.jiankong+="\n- - - - - - - - - - - - - - - - - - - -\n";
							
							System.out.println("[client]"+deviceId+"[上送数据] .....");
							out.write(data);
						}
						else
						{
							System.out.println("[client]数据校验错误");
						}		
					} 
					else if(func == ModbusDirectHelper.WRITE_SINGLE_REGISTER){
						int hb = packet[packet.length - 2]*256 + packet[packet.length - 1];// 存入低/高位					
						changeHeartBreak(hb);
					}
					else {
						// 其他指令暂不支持，不做响应
					}
				}
			} catch (SocketException e) {
				// 连接被拒绝refused或连接成功后被强制断开reset（服务器非正常离线或网络故障），都会捕获到异常，应重新尝试连接
				System.out.println("[client]"+deviceId+"[连接异常]" + e.getMessage());
			} catch (IOException e1) {
				e1.printStackTrace();// 未知异常
			} finally {
				Thread.sleep(RECONNECT_INTERVAL);
			}
		}
		client = null;
		System.out.println("DummyClient 线程结束");
	}

	/**
	 * 心跳线程
	 *
	 */
	private class HeartBeatThread extends Thread {
		@Override
		public void run() {
			while (dcthread_on) {
				// 定时发送心跳
				try {
					Thread.sleep(HEART_BEAT_INTERVAL);// 心跳间隔
					if (client == null || client.isClosed())
						continue;
					client.getOutputStream().write(ModbusDirectHelper.deviceHeartbeat(deviceId)); // 发送心跳包
				} catch (IOException e) {
					System.out.println("[心跳异常]" + e.getMessage());
				} catch (InterruptedException e2) {
					System.out.println(e2.getMessage() + " 线程被打断（不会发生）");
				}
			}
			client = null;
			System.out.println("DummyClient 线程结束");
		}
	}
	
	/**
	 * 结束线程操作
	 */
	public void closeThread() {
		dcthread_on = false;
	}

}
