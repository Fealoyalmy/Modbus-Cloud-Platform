package edu.ahau.jsjds.sql;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class DataDaoImpl implements DataDao{
SqlSession session = null;
	
	public List<Data> selectAllData() {
		List<Data> list = new ArrayList<Data>();
		try {
			String resource = "mybatis-config.xml";
			Reader reader = Resources.getResourceAsReader(resource);
			SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
			SqlSessionFactory factory = builder.build(reader);	
			session = factory.openSession();	
			list = session.selectList("edu.ahau.jsjds.sql.DataDao.selectAllData");	
		}catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(list);
		return list;
	}
	
	public int insertData(Data data) {
		int rs = 0;
		try {
			String resource = "mybatis-config.xml";
			Reader reader = Resources.getResourceAsReader(resource);
			SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
			SqlSessionFactory factory = builder.build(reader);
			session = factory.openSession();
			rs = session.insert("edu.ahau.jsjds.sql.DataDao.insertData", data);
			session.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rs;
	}

}
