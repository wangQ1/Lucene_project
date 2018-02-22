package cn.et.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import cn.et.entity.Cookbook;

public class LuceneUtil {
	// 指定索引库的位置
	private static String dir = "E:\\index";
	// 定义分词器 当参数为true时，分词器进行智能切分 默认细粒度切分算法
	private static IKAnalyzer ika = new IKAnalyzer();

	/**
	 * 将数据写入索引库
	 * 
	 * @param cb
	 *            一条数据
	 * @throws IOException
	 */
	public static void write(Cookbook cb) throws IOException {
		// 新建 document 对象
		Document doc = new Document();
		// 添加field 属性 属性名 属性值 [写入索引库(TYPE_NOT_STORED 不写入)] 属性值传入byte数组为默认写入
		doc.add(new Field("id", cb.getId() + "", TextField.TYPE_STORED));
		doc.add(new Field("dishes", cb.getDishes(), TextField.TYPE_STORED));
		doc.add(new Field("illustrate", cb.getIllustrate(), TextField.TYPE_STORED));
		// 指定索引库的存储目录
		Directory directory = FSDirectory.open(new File(dir));
		// 索引创建器配置 将分词器与lucene版本关联
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, ika);
		// 新建索引创建器 传入索引库的存储目录和分词器
		IndexWriter iwriter = new IndexWriter(directory, config);
		// 索引创建器中添加doucment
		iwriter.addDocument(doc);
		// 事务提交 索引创建
		iwriter.commit();
		iwriter.close();
	}

	/**
	 * 搜索 并高亮显示搜索内容
	 * @return
	 * @throws InvalidTokenOffsetsException
	 */
	public static List<Cookbook> search(String field, String value)
			throws IOException, ParseException, InvalidTokenOffsetsException {
		// 指定索引库的存储目录
		Directory directory = FSDirectory.open(new File(dir));
		// 读取索引库的存储目录
		DirectoryReader ireader = DirectoryReader.open(directory);
		// 搜索类
		IndexSearcher isearcher = new IndexSearcher(ireader);
		// lucene的查询解析器 用于指定查询的属性名和分词器
		QueryParser parser = new QueryParser(Version.LUCENE_47, field, ika);
		// 搜索
		Query query = parser.parse(value);
		// 设置高亮标签   默认为<B><B/>
		SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter("<font color=red>", "</font>");
		// 高亮分析器     参数1 添加前后缀的处理类     参数2 查询得分 可以传入分数
		Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
		// 获取搜索结果 可以指定返回的doucment个数 根据得分排序
		ScoreDoc[] hits = isearcher.search(query, null, 10).scoreDocs;
		List<Cookbook> list = new ArrayList<Cookbook>();
		for (int i = 0; i < hits.length; i++) {
			int id = hits[i].doc;// 获取document的id
			Document hitDoc = isearcher.doc(id);//根据document的id 获取具体的document对象
			Cookbook e = new Cookbook();
			e.setId(Integer.parseInt(hitDoc.get("id")));
			//获取document中的字段值
			String dishes = hitDoc.get("dishes");
			// 高亮处理
			String hhField = LuceneUtil.highlight(highlighter, isearcher, id, dishes, "dishes");
			// 将高亮处理后的内容放入实体类中
			e.setDishes(hhField);
			String illustrate = hitDoc.get("illustrate");
			hhField = LuceneUtil.highlight(highlighter, isearcher, id, illustrate, "illustrate");
			e.setIllustrate(hhField);
			list.add(e);
		}
		ireader.close();
		directory.close();
		return list;
	}

	/**
	 * 进行高亮处理
	 * @param query      搜索器
	 * @param isearcher  搜索类
	 * @param id		 document的id
	 * @param keyword	   关键字
	 * @param field		 document的属性名
	 * @return
	 * @throws IOException
	 * @throws InvalidTokenOffsetsException
	 */
	public static String highlight(Highlighter highlighter, IndexSearcher isearcher, int id, String keyword, String field)
			throws IOException, InvalidTokenOffsetsException {
		// 将查询的内容与搜索内容分词后匹配    匹配到后添加前后缀 高亮
		TokenStream tokenStream = TokenSources.getAnyTokenStream(isearcher.getIndexReader(), id, field, ika);
		// 里面封装了高亮的文本片段
		TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, keyword, true, 50);
		// 高亮处理后  内容存放的变量
		String hhField = "";
		if(frag.length < 1){
			return keyword;
		}else{
			for (int j = 0; j < frag.length; j++) {
				if ((frag[j] != null) && (frag[j].getScore() > 0)) {
					hhField = (frag[j].toString());
				}
			}
		}
		return hhField;
	}
}
