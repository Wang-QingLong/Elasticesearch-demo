package com.ooo.esdemo.repository;

import com.ooo.esdemo.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * @version V1.0
 * @author: WangQingLong
 * @date: 2019/12/10 22:05
 * @description: Goods实体类的接口 第一个泛型填写实体类，第二个泛型是指Id类型
 */
public interface GoodsRepository  extends ElasticsearchRepository<Goods,Long> {

    List<Goods> queryByBrandMatchesAndAndPriceBetween(String s,double gte,double  lte);
}
