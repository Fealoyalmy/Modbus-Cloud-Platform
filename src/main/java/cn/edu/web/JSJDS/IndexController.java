package cn.edu.web.JSJDS;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Lettuce.Cluster.Refresh;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ahau.jsjds.iot.DummyClient;
import edu.ahau.jsjds.iot.Server;
import groovyjarjarantlr.collections.List;

@Controller

public class IndexController {
	// 唯一仿真客户端实例对象
	DummyClient dc = new DummyClient();

	//用来处理请求，接受请求，给出响应
	@RequestMapping()
	ModelAndView home()
	{
        ModelAndView mv = new ModelAndView();
		
		mv.setViewName("index");
		
		return mv;//主页
	}
	
	@RequestMapping("/home")
	ModelAndView refreshdata()
	{
		ModelAndView mv = new ModelAndView();
		
		mv.setViewName("home");
		mv.addObject("change", Server.change);
		mv.addObject("jiankong",Server.jiankong);
		return mv;
	}
	
	@RequestMapping("/refresh")
	@ResponseBody
	String refresh() {
		String res="";
		
		Map<String, ArrayList<Map<String, String>>> map = new HashMap<>();
		ArrayList<Map<String, String>> list= new ArrayList<Map<String, String>>();
		
		Map<String, String> qianduan = new HashMap<>();
		qianduan.put("change", Server.change);
		qianduan.put("jiankong", Server.jiankong);
		list.add(qianduan);
		
		for (String i : Server.sensorData.keySet()) {
			Map<String, String> data= new HashMap<>();

			data.put("id", i);
			data.put("data", Server.sensorData.get(i));
			data.put("status", String.valueOf(Server.ifswitch.get(i)));
			list.add(data);
		}
		
		map.put("data", list);
		ObjectMapper mapper = new ObjectMapper();
		
		try {
            res = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
		return res;
	}
	//TODO 获取后台已连接的设备信息
	@RequestMapping("/get_contral_inf")
	@ResponseBody
	String get_contral_inf() {
		String res="";
		
		Map<String, ArrayList<Map<String, String>>> map = new HashMap<>();
		ArrayList<Map<String, String>> list= new ArrayList<Map<String, String>>();
		for (String i : Server.heartbreak.keySet()) {		
			Map<String, String> data= new HashMap<>();
			
			data.put("id", i);
			data.put("time", Server.heartbreak.get(i).toString());
			data.put("status",String.valueOf(Server.isWork.get(i)));
			data.put("data", Server.sensorData.get(i));
			list.add(data);
        }
			
		map.put("data", list);
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
            res = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
		return res;
	}
	
	@RequestMapping("/change_time")
	@ResponseBody
	String change_time(@RequestBody Map<String,String> inf) {
		String res="";
		Map<String, String> map = new HashMap<>();
		
		String add = inf.get("add");
		int time = Integer.parseInt(inf.get("time"));
		System.out.println("web获取add:" + add);
		System.out.println("web获取time:" + time);
		System.out.println("原time=" + Server.heartbreak.get(add).toString());
		//TODO 在这个地方进行修改心跳时间
		Server.heartbreak.put(add, time);
		System.out.println("现time=" + Server.heartbreak.get(add).toString());
		
		map.put("status", "success");
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
            res = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
		return res;
	}
	
	@RequestMapping("/change_status")
	@ResponseBody
	String change_status(@RequestBody Map<String,String> inf) {
		String res="";
		String flag;
		Map<String, String> map = new HashMap<>();
		//System.out.println(inf.get("status"));
		flag=inf.get("status");
		
		if(flag.isEmpty()) {
			//System.out.println("关闭");
			Server.isWork.put(inf.get("add"), false);
			map.put("kaiguan", "guan");
			//System.out.println(Server.isWork);
		}	
		else {
			//System.out.println("打开");
			Server.isWork.put(inf.get("add"), true);
			map.put("kaiguan", "kai");
			//System.out.println(Server.isWork);
		}
		
		//System.out.println(Server.isWork.get(inf.get("add")));
		map.put("status", "success");
		
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
            res = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
		return res;
	}
	
	@RequestMapping("/page1")
	ModelAndView page1()
	{
		ModelAndView mv = new ModelAndView();
		
		mv.setViewName("page1");
		mv.addObject("jiange", DummyClient.getHeartBreak());
		mv.addObject("addr", Server.addr);
		mv.addObject("offset", Server.offset);
		mv.addObject("count", Server.count);
		return mv;//决定显示哪个界面
	}
	
	@PostMapping("/page1success")
	ModelAndView page1success(@RequestParam int addr,@RequestParam int offset,@RequestParam int count,@RequestParam float mySlider)
	{
		ModelAndView mv = new ModelAndView();
		Server.addr=addr;Server.offset=offset;Server.count=count;
		System.out.println("addr ="+addr);
		System.out.println("offset ="+offset);
		System.out.println("count ="+count);
		System.out.println("mySlider ="+mySlider);
		DummyClient.changeHeartBreak((int)mySlider);
		mv.setViewName("page1success");
		return mv;//决定显示哪个界面
	}
	
	@GetMapping("/page2")
	String page2()
	{
		return "page2";//决定显示哪个界面
	}
	
	// 启动仿真客户端
	@RequestMapping("/start_dummy")
	@ResponseBody
	String startDummy() {
		String res="";
		Map<String, String> map = new HashMap<>();
		if(!DummyClient.isConnected()) {
			dc.start();
			System.out.println("DummyClient 启动！");
			map.put("status", "on");
		}
		else {
			dc.closeThread();// = null;//new DummyClient();
			System.out.println("DummyClient 关闭！");
			map.put("status", "off");
		}
		ObjectMapper mapper = new ObjectMapper();	
		try {
            res = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
		return res;
	}
	
	// 获取网页url更改配置文件
	@RequestMapping("/getsqlurl")
	@ResponseBody
	String getSqlUrl(@RequestBody Map<String,String> inf) {
		String res="";		
		String url = inf.get("url");		
		Properties propertie = new Properties();
		FileInputStream inputFile = null;
		FileOutputStream outputFile = null;
		try {
			inputFile = new FileInputStream("src/main/resources/jdbc.properties");
			propertie.load(inputFile);
			outputFile = new FileOutputStream("src/main/resources/jdbc.properties");
			propertie.setProperty("jdbc.url", url + "?useSSL=false&characterEncoding=UTF8&serverTimezone=Asia/Shanghai");
			propertie.store(outputFile, "jdbc");
			inputFile.close();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
        System.out.println("修改jdbc-url：" + url);
		Map<String, String> map = new HashMap<>();
		map.put("status", "success");
		ObjectMapper mapper = new ObjectMapper();	
		try {
            res = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
		return res;
	}
	
	@RequestMapping("/change_threshold")
	@ResponseBody
	String change_threshold(@RequestBody Map<String,String> inf) {
		String res="";
		Map<String, String> map = new HashMap<>();
		
		String add = inf.get("add");
		String threshold = inf.get("threshold");
		System.out.println("web获取add:" + add);
		System.out.println("web获取threshold:" + threshold);
		Server.dev_thresholds.put(add, Integer.parseInt(threshold));

		map.put("status", "success");
		ObjectMapper mapper = new ObjectMapper();
		try {
            res = mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
		return res;
	}
	
	
	
	/*@RequestMapping("")
	String g(@RequestParam String name,@RequestParam String age)
	{
		
		return "";//决定显示哪个界面
	}*/

}
