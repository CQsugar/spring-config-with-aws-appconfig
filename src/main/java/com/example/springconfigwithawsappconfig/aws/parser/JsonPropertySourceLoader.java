package com.example.springconfigwithawsappconfig.aws.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class JsonPropertySourceLoader implements PropertySourceLoader {

    /**
     * constant.
     */
    private static final String VALUE = "value";

    /**
     * symbol: dot.
     */
    private static final String DOT = ".";

    /**
     * symbol: left bracket.
     */
    private static final String LEFT_BRACKET = "[";

    /**
     * symbol: right bracket.
     */
    private static final String RIGHT_BRACKET = "]";

    @Override
    public String[] getFileExtensions() {
        return new String[]{"json"};
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>(32);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> appConfigDataMap = mapper.readValue(resource.getInputStream(),
                LinkedHashMap.class);
        flattenedMap(result, appConfigDataMap, null);
        return Collections.singletonList(
                new OriginTrackedMapPropertySource(name, this.reloadMap(result), true));
    }


    protected void flattenedMap(Map<String, Object> result, Map<String, Object> dataMap,
                                String parentKey) {
        if (CollectionUtils.isEmpty(dataMap)) {
            return;
        }
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            String fullKey = this.getFullKey(parentKey, key);

            boolean isNotAdded = true;
            if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) value;
                flattenedMap(result, map, fullKey);
                isNotAdded = false;
            } else if (value instanceof Collection) {
                int count = 0;
                Collection<?> collection = (Collection<?>) value;
                for (Object object : collection) {
                    flattenedMap(result,
                            Collections.singletonMap(LEFT_BRACKET + (count++) + RIGHT_BRACKET, object),
                            fullKey);
                }
                isNotAdded = false;
            }

            if (isNotAdded) {
                result.put(fullKey, value);
            }
        }
    }

    private String getFullKey(String parentKey, String key) {
        String fullKey;
        if (StringUtils.hasLength(parentKey)) {
            if (key.startsWith(LEFT_BRACKET)) {
                fullKey = parentKey.concat(key);
            } else {
                fullKey = parentKey.concat(DOT).concat(key);
            }
        } else {
            fullKey = key;
        }
        return fullKey;
    }


    /**
     * Reload the key ending in `value` if need.
     */
    protected Map<String, Object> reloadMap(Map<String, Object> map) {
        if (CollectionUtils.isEmpty(map)) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>(map);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.contains(DOT)) {
                int idx = key.lastIndexOf(DOT);
                String suffix = key.substring(idx + 1);
                if (VALUE.equalsIgnoreCase(suffix)) {
                    result.put(key.substring(0, idx), entry.getValue());
                }
            }
        }
        return result;
    }


}
