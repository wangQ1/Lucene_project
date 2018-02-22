package cn.et.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import cn.et.entity.Cookbook;

@Mapper//集成到SpringBoot中需要添加该注解 ,表示这是一个映射接口
public interface CookbookMapper {
	/**
	 * 获取总行数
	 * @return
	 */
	@Select("select count(*) from cookbook")
	public int queryCount();
	/**
	 * 分页获取数据
	 * @param start 开始的位置
	 * @param length 要取的数量
	 * @return
	 */
	@Select("select * from cookbook limit #{0}, #{1}")
	public List<Cookbook> queryAll(int start, int length);
}
