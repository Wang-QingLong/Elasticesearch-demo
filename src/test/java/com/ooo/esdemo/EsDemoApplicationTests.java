package com.ooo.esdemo;

import com.ooo.esdemo.pojo.Goods;
import com.ooo.esdemo.repository.GoodsRepository;
import com.ooo.esdemo.utilsMapper.HighlightResultMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class EsDemoApplicationTests {
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private GoodsRepository goodsRepository;

    /**
     * 创建索引库
     */
    @Test
    public void createIndex() {
        //创建索引库
        elasticsearchTemplate.createIndex(Goods.class);
        //创建映射关系
        elasticsearchTemplate.putMapping(Goods.class);


    }

    /**
     * 添加数据 添加和修改都是save
     */
    @Test
    public void esAdd() {
        goodsRepository.save(new Goods(1L, "小米手机9", " 手机",
                "小米", 3499.00, "http://image.leyou.com/13123.jpg"));
    }

    /**
     * 批量添加数据 添加和修改都是save
     */
    @Test
    public void esAddMuch() {
        ArrayList<Goods> goods = new ArrayList<>();
        goods.add(new Goods(2L, "小米手机1", " 手机",
                "小米", 3599.00, "http://image.leyou.com/13124.jpg"));
        goods.add(new Goods(3L, "小米手机2", " 手机",
                "小米", 3699.00, "http://image.leyou.com/13125.jpg"));
        goods.add(new Goods(4L, "小米手机3", " 手机",
                "小米", 3799.00, "http://image.leyou.com/13126.jpg"));
        goods.add(new Goods(5L, "小米手机4", " 手机",
                "小米", 3899.00, "http://image.leyou.com/13127.jpg"));
        goods.add(new Goods(6L, "小米手机5", " 手机",
                "小米", 3999.00, "http://image.leyou.com/13128.jpg"));

        goodsRepository.saveAll(goods);
    }


    /**
     * 更新数据，更新数据：有则改，无则添加
     */
    @Test
    public void esUpdate() {
        goodsRepository.save(new Goods(2L, "大米手机8", " 手机",
                "大米", 5999.00, "http://image.leyou.com/15123.jpg"));
    }

    /**
     * 查询所有数据，使用r表达式加上方法引用
     */
    @Test
    public void findAll() {
        Iterable<Goods> goods = goodsRepository.findAll();
        goods.forEach(System.out::println);
    }

    /**
     * Id删除
     */
    @Test
    public void deleteById() {
        goodsRepository.deleteById(1L);
    }

    /**
     * 黑科技：接口中定义的方法但未实现也可以使用
     * 标题和价位查询
     */
    @Test
    public void queryByTitleMatchesAndPrice() {
        List<Goods> goods = goodsRepository.queryByBrandMatchesAndAndPriceBetween("小米手机", 3000d, 4000d);
        goods.forEach(System.out::println);
    }

    /**
     * 原生多条件查询
     * 原生的查询建议使用esTemplate
     */
    @Test
    public void nativeQuery() {
        //原生查询构建工厂: querybuild 就是整个大的查询条件对象
        NativeSearchQueryBuilder querybulid = new NativeSearchQueryBuilder();
        //查询条件
        querybulid.withQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("title", "小米手机")).mustNot(QueryBuilders.matchQuery("price", "3999")));
        //排序
        querybulid.withSort(SortBuilders.fieldSort("price"));
        //分页
        querybulid.withPageable(PageRequest.of(0, 10));
        //高亮:spring不支持依赖，所以需要自己配置，在查询数据里面自定义
        querybulid.withHighlightFields(new HighlightBuilder.Field("title"));
        //聚合
//        querybulid.addAggregation(AggregationBuilders.);

        //利用原生的构建工厂生产出一个查询对象
        SearchQuery query = querybulid.build();
        //查询数据
        AggregatedPage<Goods> goods = elasticsearchTemplate.queryForPage(query, Goods.class,new HighlightResultMapper());
        //解析数据
        //总条数
        int totalPages = goods.getTotalPages();
        System.out.println("总页数 = " + totalPages);
        //总页数
        long totalElements = goods.getTotalElements();
        System.out.println("总条数 = " + totalElements);
        System.out.println("----------------------------------");
        goods.forEach(System.out::println);
    }


    /**
     * 聚合查询
     */
    @Test
    public void addAggregation() {
        //创建聚合原生查询对象
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.addAggregation(AggregationBuilders.terms("agg_price").field("category"));

        //生产查询条件对象
        NativeSearchQuery searchQuery = queryBuilder.build();
        //查询数据
        AggregatedPage<Goods> result = elasticsearchTemplate.queryForPage(searchQuery, Goods.class);
        //分析结果数据 :获取聚合结果对象
        Aggregations aggregations = result.getAggregations();
        //根据集合名称获取对象 :根据聚合的类型来确定使用什么接口接收，不然会报空指针
        Terms agg_price = aggregations.get("agg_price");
        //获取桶
        List<? extends Terms.Bucket> buckets = agg_price.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            //获取Key
            String key = bucket.getKeyAsString();
            System.out.println("key = " + key);
            long docCount = bucket.getDocCount();
            System.out.println("docCount = " + docCount);
        }


    }
}
