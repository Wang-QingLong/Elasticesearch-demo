package com.ooo.esdemo.utilsMapper;

import com.google.gson.Gson;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HighlightResultMapper implements SearchResultMapper {
    Gson gson = new Gson();

    @Override
    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        String scrollId = response.getScrollId();
        long total = response.getHits().getTotalHits();
        float maxScore = response.getHits().getMaxScore();

        List<T> list = new ArrayList<>();
        for (SearchHit hit : response.getHits()) {
            String source = hit.getSourceAsString();
            T t = gson.fromJson(source, clazz);
            // 处理高亮
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {
                for (Map.Entry<String, HighlightField> entry : highlightFields.entrySet()) {
                    String fieldName = entry.getKey();
                    String value = StringUtils.join(entry.getValue().getFragments());
                    try {
                        BeanUtils.setProperty(t, fieldName, value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            list.add(t);
        }
        return new AggregatedPageImpl<>(list, pageable, total, response.getAggregations(), scrollId, maxScore);
    }
}