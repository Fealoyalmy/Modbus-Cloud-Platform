package edu.ahau.jsjds.iot;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.math.*;

/**
 * 
 * 服务端类
 *
 */
public class Server {
	private ServerSocket server;

	private ExecutorService pool;

	public static int isrun = 1;

	public static int addr = 0, offset = 0, count = 1;

	public static int WaterSwitch = 0; // 水泵开关
	public static int MIN_humidity = 60; // 小于则开水

	public static Map<String, Integer> heartbreak = new HashMap<>();

	public static Map<String, Boolean> isWork = new HashMap<>();

	public static Map<String, Boolean> ifswitch = new HashMap<>();
	
	public static Map<String, Integer> dev_thresholds = new HashMap<>();

	public static Map<String, String> sensorData = new HashMap<>();
	
	public static int device_ch;
	public static int threshold;

	public static String jiankong = "服务端已上线\n\n";
	public static String change = "准备中\n";

	public void start() throws IOException {
		Config config = Config.instance();
		server = new ServerSocket(config.getServerPort());
		pool = Executors.newFixedThreadPool(config.getServerPoolSize());
		while (true) {
			Socket socket = server.accept();
			pool.execute(new DataProcessor(socket));
		}
	}

	/**
	 * 数据处理线程类。启动后一直尝试阻塞读取数据， 若客户端主动关闭连接或意外离线，则本线程退出，释放资源。
	 * 
	 *
	 */
	class DataProcessor implements Runnable {
		/**
		 * 通信管道
		 */
		private Socket socket;
		/**
		 * 设备id
		 */
		private int id;

		public DataProcessor(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				InputStream in = socket.getInputStream();
				byte[] packet = new byte[1024];
				int readBytes = 0;

				// 阻塞读取客户端的数据，若读到-1表示客户端主动断开连接，退出本循环，释放资源。
				while ((readBytes = in.read(packet)) > -1) {
					// 1、识别心跳指令或数据指令；2、心跳指令后紧跟着发读数据指令；3、数据指令后紧跟着解析数据
					int addr = packet[0] & 0xFF;// 地址1字节
					int oprt = packet[1] & 0xFF;// 指令1字节
					int len = Util.bytes2Int(Arrays.copyOfRange(packet, 2, 3));// 数据长度2字节 1
					byte[] data = Arrays.copyOfRange(packet, 3, 3 + len);

					int sdata = Util.bytes2Int(Arrays.copyOfRange(packet, 5, 7));

					if (isWork.get(Integer.toString(addr)) == null) {
						isWork.put(Integer.toString(addr), true);
					}

					if (isWork.get(Integer.toString(addr)) == true) {
						switch (oprt) {
						case ModbusDirectHelper.OP_READ_HOLDING_REGISTER:

							if (Crc16.check(Arrays.copyOfRange(packet, 0, 5 + len))) {
								Date currentTime = new Date();
								int humidity_data = (1023 - sdata) * 100 / 650;
								int water_data = sdata * 100 / 1023;// (Math.exp(0.0056 * 3300000 / 4096 * sdata /
																	// 1000)) * 0.467;
								if (addr == 0x00) {
									byte[] sw = Util.switchController(Integer.toString(addr), humidity_data);
									if (sw[0] != 0x00)
										socket.getOutputStream()
												.write(ModbusDirectHelper.writeSingleRegister(Server.addr, 1, sw));
								} else if (addr == 0x01) {

									byte[] sw = Util.switchController(Integer.toString(addr), water_data);
									if (sw[0] != 0x00)
										socket.getOutputStream()
												.write(ModbusDirectHelper.writeSingleRegister(Server.addr, 1, sw));
								}

								if (addr == 0) {
									sensorData.put(Integer.toString(addr), Integer.toString(humidity_data) + "%");
								}
								else if (addr == 1) {
									sensorData.put(Integer.toString(addr), Integer.toString(water_data) + "%");
								}
								else {
									sensorData.put(Integer.toString(addr), Integer.toString(sdata));
								}
								// new BigDecimal(water_data).setScale(2,1)

								System.out.println();
								SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
								System.out.println("数据校验正确");
								change = "时间：" + formatter.format(currentTime) + "\n";
								change += "数据校验正确\n";
								// jiankong+="数据校验正确\n";

								System.out.println("数据，解析中...");
								change += "数据，解析中...\n";

								System.out.println("设备ID：" + id);
								System.out.println("设备地址：" + Util.bytesToHexFun2(Arrays.copyOfRange(packet, 0, 1)));
								System.out.println("功能码：" + Util.bytesToHexFun2(Arrays.copyOfRange(packet, 1, 2)));
								System.out.println("数据个数：" + len / 2 + "  数据字长：" + len);
								change += "设备ID：" + id + "\n" + "设备地址："
										+ Util.bytesToHexFun2(Arrays.copyOfRange(packet, 0, 1)) + "\n" + "功能码："
										+ Util.bytesToHexFun2(Arrays.copyOfRange(packet, 1, 2)) + "\n" + "数据个数："
										+ len / 2 + "  数据字长：" + len + "\n";

								for (int i = 0; i < len; i += 2) {
									change += "传感器原始数据#" + (i + 1) / 2 + 1 + "："
											+ Util.bytesToHexFun2(Arrays.copyOfRange(data, i, i + 2)) + "  \n";

									System.out.print("传感器原始数据#" + (i + 1) / 2 + 1 + "：");
									System.out.print(Util.bytesToHexFun2(Arrays.copyOfRange(data, i, i + 2)) + "  ");
									System.out.println();
								}

								// 设置心跳
								int sensor_heart = data[0] * 256 + data[1];
								System.out.println("传感器心跳=" + sensor_heart);
								int HeartInterval = Server.heartbreak.get(Integer.toString(addr));
								System.out.println("hashmap心跳=" + HeartInterval);
								if (sensor_heart != HeartInterval) { // 前端更改心跳再发送更改指令
									byte[] changeHeart = new byte[2];
									changeHeart[0] = (byte) ((HeartInterval >> 8) & 0xff);
									changeHeart[1] = (byte) (HeartInterval & 0xff);
									socket.getOutputStream()
											.write(ModbusDirectHelper.writeSingleRegister(Server.addr, 1, changeHeart));
									// Server.addr,1,HeartInterval
								}
								
								System.out.println("设备" + addr + "当前调控阈值：" + dev_thresholds.get(Integer.toString(addr)));
								
							} else {
								System.out.println("[服务端读取]数据校验出错！");
								change += "数据校验错误\n";
								jiankong += "数据校验错误\n";
							}

							break;
						case ModbusDirectHelper.OP_DEVICE_REGISTER:
							if (Crc16.check(packet)) {
								Date currentTime = new Date();
								SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
								// 注册

								System.out.println("设备注册数据校验正确");
								change = "设备注册数据校验正确\n";
								jiankong += "\n设备注册数据校验正确\n";
								id = Util.bytes2Int(data);
								System.out.println("设备" + id + "已连接 " + formatter.format(currentTime));
								change += "设备" + id + "已连接" + "  " + formatter.format(currentTime) + "\n";

								jiankong += "设备" + id + "已连接" + "  " + formatter.format(currentTime) + "\n";

								// 记录设备心跳间隔数据 <add, time>
								int hb = Util.bytes2Int(Arrays.copyOfRange(packet, 3, 5));
								// System.out.println(hb);
								heartbreak.put(Integer.toString(addr), hb);
								// 设置外设状态
								ifswitch.put(Integer.toString(addr), false);
								// 预置阈值 
								dev_thresholds.put(Integer.toString(addr), 60);

								// 收到注册后马上向该客户端发送读数据的指令
								socket.getOutputStream().write(ModbusDirectHelper.readHoldingRegister(Server.addr,
										Server.offset, Server.count));
							} else {
								System.out.println("设备注册数据校验出错！");
								change += "设备注册数据校验错误\n";
								jiankong += "设备注册数据校验错误\n";
							}
							break;
						case ModbusDirectHelper.OP_DEVICE_HEARTBEAT:
							if (Crc16.check(Arrays.copyOfRange(packet, 0, 5 + len))) {
								System.out.println("上传数据校验正确");
								change = "上传数据校验正确\n";
								jiankong += "\n上传数据校验正确\n";

								Date currentTime = new Date();
								SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

								System.out.println("设备" + id + "开始上传数据 " + formatter.format(currentTime));
								change += "设备" + id + "开始上传数据" + "  " + formatter.format(currentTime) + "\n";
								jiankong += "设备" + id + "开始上传数据" + "  " + formatter.format(currentTime) + "\n";
								// 收到心跳后马上向该客户端发送读数据的指令
								socket.getOutputStream().write(ModbusDirectHelper.readHoldingRegister(Server.addr,
										Server.offset, Server.count));
							} else {
								System.out.println("上传数据校验出错！");
								change += "上传数据校验错误\n";
								jiankong += "上传数据校验错误\n";
							}

							break;

						default:
							break;
						}
					} else {

						System.out.println("当前设备：" + Integer.toString(addr) + " 已关闭");
						byte[] sw = Util.switchController(Integer.toString(addr), 100);
						socket.getOutputStream().write(ModbusDirectHelper.writeSingleRegister(Server.addr, 1, sw));

					}
				}
			} catch (Exception e) {
				// 客户端非正常断开（进程退出、网络故障等），会捕获到异常，释放资源
				System.out.println(e.getMessage());
			} finally {
				if (socket != null && socket.isConnected()) {
					try {
						socket.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}
}
