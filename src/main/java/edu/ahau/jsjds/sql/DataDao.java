package edu.ahau.jsjds.sql;

import java.util.List;

public interface DataDao {
	public List<Data> selectAllData();
	public int insertData(Data data);
	//public int updateData(Data data);
}
