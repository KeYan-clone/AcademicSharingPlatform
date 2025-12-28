# Elasticsearch 学术成果搜索方案（简化版）

## 核心公式

$$\text{最终得分} = \text{BM25相关度} \times \left(1 + \frac{\log(1+citedByCount) \times 1.2 + \log(1+favouriteCount) \times 1.0 + \log(1+readCount) \times 0.8}{3}\right)$$

**说明**：
- **BM25 相关度**：字段匹配（title^3, abstractText^2）
    - abstractText 可能不存在或为null，需要判断
- **权重系数**：被引用次数(1.2) > 收藏次数(1.0) > 阅读次数(0.8)  
- **log1p 修饰符**：log(1+x) 函数，避免大数值过度影响
- **boost_mode**: multiply - 相关度与权重相乘，平衡两者
