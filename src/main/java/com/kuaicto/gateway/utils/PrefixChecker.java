package com.kuaicto.gateway.utils;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class PrefixChecker {
	private final Set<String> prefixSet = new HashSet<>();
	private final Set<String> ignoredPrefixSet = new HashSet<>();

	/**
	 * 配置格式（半角冒号分割， 具有高优先级!表示不匹配）: /path/to/a:!/path/to/a/b:/path/to/b:... 匹配所有指定路径，忽略以!开始的路径
	 * @param prefixConfig
	 */
	public PrefixChecker(String prefixConfig) {

		if (StringUtils.isNotBlank(prefixConfig)) {
			for (String prefix : StringUtils.split(prefixConfig, ':')) {
				if (StringUtils.isNotBlank(prefix)) {
					if (prefix.startsWith("!")) {
						ignoredPrefixSet.add(prefix.substring(1));
					} else {
						prefixSet.add(prefix);	
					}
				}
			}
		}
		if (prefixSet.isEmpty()) {
			prefixSet.add("/"); // 默认拦截所有
		}
	}
	
	/**
	 * 是否匹配
	 * @param path
	 * @return
	 */
	public boolean match(String path) {
		
		// 不匹配具有更高优先级
		for (String prefix : this.ignoredPrefixSet) {
			if (path.startsWith(prefix)) {
				return false;
			}
		}

		// 匹配检查
		boolean skip = true; // 标记是否跳过授权
		for (String prefix : prefixSet) {
			if (path.startsWith(prefix)) {
				skip = false;
				break;
			}
		}

		return !skip;
	}
}
