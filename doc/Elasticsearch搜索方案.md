# Elasticsearch 学术成果搜索方案（简化版）

## 核心公式

$$\text{最终得分} = \text{BM25相关度} \times \left(1 + \frac{\log(1+cited\_by\_count) \times 1.2 + \log(1+favourite\_count) \times 1.0 + \log(1+read\_count) \times 0.8}{3}\right)$$

**说明**：
- **BM25 相关度**：多字段 OR 匹配（title^3 + concepts^2.5 + abstract^1.5）
- **权重系数**：被引用次数(1.2) > 收藏次数(1.0) > 阅读次数(0.8)  
- **log1p 修饰符**：log(1+x) 函数，避免大数值过度影响
- **boost_mode**: multiply - 相关度与权重相乘，平衡两者

---

## 实现层次

### 1. Repository 层
- `searchByKeywordWithWeighting()` - 关键词加权搜索
- `searchByDateRangeAndKeywordWithWeighting()` - 日期范围+关键词加权搜索

### 2. Service 层  
- `searchByKeywordWithWeighting()` - 调用 Repository 返回 DTO
- `searchByDateRangeAndKeywordWithWeighting()` - 带时间范围的加权搜索

### 3. Controller 层
- `GET /achievements/search/weighted` - 关键词加权搜索 API
- `GET /achievements/search/weighted-by-date` - 日期范围+关键词加权搜索 API

---

## Elasticsearch DSL 查询

```json
{
  "query": {
    "function_score": {
      "query": {
        "multi_match": {
          "query": "人工智能",
          "fields": [
            "title^3",
            "concepts^2.5",
            "abstract^1.5"
          ],
          "operator": "or",
          "fuzziness": "AUTO"
        }
      },
      "functions": [
        {
          "field_value_factor": {
            "field": "cited_by_count",
            "factor": 1.2,
            "modifier": "log1p",
            "missing": 0
          }
        },
        {
          "field_value_factor": {
            "field": "favourite_count",
            "factor": 1.0,
            "modifier": "log1p",
            "missing": 0
          }
        },
        {
          "field_value_factor": {
            "field": "read_count",
            "factor": 0.8,
            "modifier": "log1p",
            "missing": 0
          }
        }
      ],
      "score_mode": "sum",
      "boost_mode": "multiply",
      "max_boost": 42
    }
  },
  "sort": [{"_score": {"order": "desc"}}, {"publication_date": {"order": "desc"}}],
  "from": 0,
  "size": 20
}
```

### 2. 日期范围 + 关键词加权搜索

```json
{
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "range": {
                "publication_date": {
                  "gte": "2020-01-01",
                  "lte": "2024-12-31"
                }
              }
            }
          ],
          "should": [
            {
              "multi_match": {
                "query": "深度学习",
                "fields": ["title^3", "concepts^2.5", "abstract^1.5"],
                "operator": "or",
                "fuzziness": "AUTO"
              }
            }
          ],
          "minimum_should_match": 1
        }
      },
      "functions": [
        {
          "field_value_factor": {
            "field": "cited_by_count",
            "factor": 1.2,
            "modifier": "log1p",
            "missing": 0
          }
        },
        {
          "field_value_factor": {
            "field": "favourite_count",
            "factor": 1.0,
            "modifier": "log1p",
            "missing": 0
          }
        },
        {
          "field_value_factor": {
            "field": "read_count",
            "factor": 0.8,
            "modifier": "log1p",
            "missing": 0
          }
        }
      ],
      "score_mode": "sum",
      "boost_mode": "multiply"
    }
  },
  "sort": [{"_score": {"order": "desc"}}, {"publication_date": {"order": "desc"}}],
  "from": 0,
  "size": 20
}
```

---

## Java 代码实现

### Repository 注解
