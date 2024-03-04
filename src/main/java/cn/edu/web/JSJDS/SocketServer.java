package cn.edu.web.JSJDS;

import java.io.IOException;
import edu.ahau.jsjds.iot.*;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

@Component
public class SocketServer {
	
	@PostConstruct
	void start()
	{
		new Thread() { // 开启新的服务端线程
			@Override
			public void run() {
				//new DummyClient().start();
				try {
					new Server().start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
		System.out.println("jsjsjda-web开始运行");
	}

}
