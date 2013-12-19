package com.taodian.route;

public class CPCChainRuleMatcher implements RuleMatcher {

	/**
	 * 用于CPC的路由规则检查，因为CPC的规则，大部分是和推广者有关的。 所以优先检查userId，能快速
	 * 的判断两个规则，是否匹配。
	 */
	@Override
	public boolean isMatch(Rule r, Rule r2, boolean isStrict) {
		//!(r.sourceUserId == r2.sourceUserId || (r.sourceUserId == 0 && !isStrict))
		if(r.sourceUserId != r2.sourceUserId && (r.sourceUserId != 0 || isStrict)) return false;
		if(r.sourceNumIid != r2.sourceNumIid && (r.sourceNumIid != 0 || isStrict)) return false;
		if(r.sourceShopId != r2.sourceShopId && (r.sourceShopId != 0 || isStrict)) return false;
		if(!r.sourceShortKey.equals(r2.sourceShortKey) && (!r.sourceShortKey.equals("") || isStrict)) return false;

		return true;
	}
}
