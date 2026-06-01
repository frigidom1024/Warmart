package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.product.entity.*;
import com.mall.product.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuService {

    private final SpecGroupMapper specGroupMapper;
    private final SpecValueMapper specValueMapper;
    private final ProductSkuMapper productSkuMapper;

    /** 获取商品完整规格结构（分组 + SKU） */
    public List<SpecGroup> getGroups(Long productId) {
        List<SpecGroup> groups = specGroupMapper.selectList(
                new LambdaQueryWrapper<SpecGroup>()
                        .eq(SpecGroup::getProductId, productId)
                        .orderByAsc(SpecGroup::getSort));
        for (SpecGroup g : groups) {
            List<SpecValue> values = specValueMapper.selectList(
                    new LambdaQueryWrapper<SpecValue>()
                            .eq(SpecValue::getGroupId, g.getId())
                            .orderByAsc(SpecValue::getSort));
            g.setValues(values);
        }
        return groups;
    }

    public List<ProductSku> getSkus(Long productId) {
        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .orderByAsc(ProductSku::getSort));
        skus.forEach(ProductSku::parseSpecValueIds);
        return skus;
    }

    /** 全量保存规格定义（先删旧数据，再批量插入） */
    @Transactional
    public void saveGroupsAndSkus(Long productId,
                                   List<Map<String, Object>> groupsInput,
                                   List<Map<String, Object>> skusInput) {
        // 1. 删除旧数据
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId));
        List<SpecGroup> oldGroups = specGroupMapper.selectList(
                new LambdaQueryWrapper<SpecGroup>()
                        .eq(SpecGroup::getProductId, productId));
        for (SpecGroup g : oldGroups) {
            specValueMapper.delete(new LambdaQueryWrapper<SpecValue>()
                    .eq(SpecValue::getGroupId, g.getId()));
        }
        specGroupMapper.delete(new LambdaQueryWrapper<SpecGroup>()
                .eq(SpecGroup::getProductId, productId));

        // 2. 插入新 group + value
        Map<String, Long> valueIdMap = new HashMap<>(); // "groupName:value" -> new id
        for (int gi = 0; gi < groupsInput.size(); gi++) {
            Map<String, Object> gInput = groupsInput.get(gi);
            SpecGroup group = new SpecGroup();
            group.setProductId(productId);
            group.setName((String) gInput.get("name"));
            group.setSort(gi);
            specGroupMapper.insert(group);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> values = (List<Map<String, Object>>) gInput.get("values");
            for (int vi = 0; vi < values.size(); vi++) {
                SpecValue sv = new SpecValue();
                sv.setGroupId(group.getId());
                sv.setValue((String) values.get(vi).get("value"));
                sv.setSort(vi);
                specValueMapper.insert(sv);
                valueIdMap.put(group.getName() + ":" + sv.getValue(), sv.getId());
            }
        }

        // 3. 插入 SKU
        for (int si = 0; si < skusInput.size(); si++) {
            Map<String, Object> skuInput = skusInput.get(si);
            @SuppressWarnings("unchecked")
            List<String> specValueRefs = (List<String>) skuInput.get("specValueRefs");
            List<Long> ids = new ArrayList<>();
            for (String ref : specValueRefs) {
                Long id = valueIdMap.get(ref);
                if (id != null) ids.add(id);
            }
            if (ids.isEmpty()) continue;

            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setSpecValueIds(ids.stream().map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]")));
            sku.setPrice(skuInput.get("price") != null
                    ? new BigDecimal(skuInput.get("price").toString()) : null);
            sku.setStock(skuInput.get("stock") != null
                    ? ((Number) skuInput.get("stock")).intValue() : 0);
            sku.setImage((String) skuInput.get("image"));
            sku.setEnabled(skuInput.get("enabled") != null
                    ? (Boolean) skuInput.get("enabled") : true);
            sku.setSort(si);
            productSkuMapper.insert(sku);
        }
    }
}
