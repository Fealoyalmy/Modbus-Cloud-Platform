package edu.ahau.jsjds.iot;

import java.io.IOException;
import java.util.Properties;

/**
 * 系统配置。与config.properties属性文件配合使用。
 *
 */
public class Config {

	private static Config INSTANCE = null;
	private Properties properties = new Properties();

	private Config() {
		try {
			properties.load(Config.class.getResourceAsStream("config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取系统配置对象实例
	 * 
	 * @return
	 */
	public static Config instance() {
		if (INSTANCE == null) {
			synchronized (Config.class) {
				if (INSTANCE == null) {
					INSTANCE = new Config();
				}
			}
		}
		return INSTANCE;
	}

	/**
	 * 获取服务器端口
	 * 
	 * @return
	 */
	public int getServerPort() {
		try {
			return Integer.parseInt(properties.getProperty("port"));
		} catch (Exception e) {
			return 10086;
		}
	}

	/**
	 * 服务器端数据处理的线程池大小
	 * 
	 * @return
	 */
	public int getServerPoolSize() {
		try {
			return Integer.parseInt(properties.getProperty("pool-size"));
		} catch (Exception e) {
			return 10;
		}
	}

	/**
	 * 是否启动仿真客户端
	 * 
	 * @return
	 */
	public boolean enableDummyClient() {
		try {
			return Boolean.parseBoolean(properties.getProperty("enable-dummy"));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 获取系统配置项的内容
	 * 
	 * @param key
	 *            系统配置项的键，如port，表示socket服务监听的端口
	 * @return 系统配置项的值
	 */
	public String get(String key) {
		return properties.getProperty(key);
	}
}
