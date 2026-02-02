package com.ecommerce4j.api;

import com.ecommerce4j.api.dto.AuthContext;
import com.ecommerce4j.api.dto.UnifiedShopInfo;
import com.ecommerce4j.api.enums.Platform;

/**
 * 统一授权服务接口
 * <p>
 * 负责处理所有平台的 OAuth 2.0 授权流程。
 */
public interface EcommAuthorizationService {

    /**
     * 生成引导用户授权的 URL。
     *
     * @param state    一个随机字符串，用于防止 CSRF 攻击
     * @return 平台授权页面的完整 URL
     */
    String getAuthorizationUrl(String state);

    /**
     * 使用授权码（code）换取访问令牌（access token）和刷新令牌（refresh token）。
     *
     * @param code     用户授权后从平台重定向URL中获取的授权码
     * @return 包含访问凭证的授权上下文对象
     */
    AuthContext exchangeCodeForTokens(String code);

    /**
     * 使用刷新令牌（refresh token）来获取新的访问令牌。
     *
     * @param authContext 当前的授权上下文，必须包含有效的 refresh token
     * @return 更新后的授权上下文，包含新的 access token
     */
    AuthContext refreshTokens(AuthContext authContext);

    /**
     * 获取店铺信息
     * @param authContext
     * @return
     */
    UnifiedShopInfo getShopInfo(AuthContext authContext);
}
