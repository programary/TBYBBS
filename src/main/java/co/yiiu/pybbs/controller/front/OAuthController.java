package co.yiiu.pybbs.controller.front;

import co.yiiu.pybbs.model.OAuthUser;
import co.yiiu.pybbs.model.User;
import co.yiiu.pybbs.service.IOAuthUserService;
import co.yiiu.pybbs.service.ISystemConfigService;
import co.yiiu.pybbs.service.IUserService;
import co.yiiu.pybbs.util.CookieUtil;
import co.yiiu.pybbs.util.JsonUtil;
import co.yiiu.pybbs.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

/**
 * Created by tomoya.
 * Copyright (c) 2018, All Rights Reserved.
 * https://yiiu.co
 */
@Controller
@RequestMapping("/oauth")
public class OAuthController extends BaseController {

  @Autowired
  private ISystemConfigService systemConfigService;
  @Autowired
  private IUserService userService;
  @Autowired
  private IOAuthUserService oAuthUserService;
  @Autowired
  private CookieUtil cookieUtil;

  // 使用github联合登录
  @GetMapping("/github")
  public String github(HttpSession session) {
    String clientId = (String) systemConfigService.selectAllConfig().get("oauth_github_client_id");
    String clientSecret = (String) systemConfigService.selectAllConfig().get("oauth_github_client_secret");
    String callback = (String) systemConfigService.selectAllConfig().get("oauth_github_callback_url");

    String state = StringUtil.randomString(4).toUpperCase();
    session.setAttribute("github_state", state);

    Assert.isTrue(!StringUtils.isEmpty(clientId) && !StringUtils.isEmpty(clientSecret) && !StringUtils.isEmpty
        (callback), "Github登录还没有相关配置，联系站长吧！");

    return redirect("https://github.com/login/oauth/authorize?client_id=" + clientId + "&redirect_uri=" + callback +
        "&scope=user&state=" + state);
  }

  // github联合登录后的回调
  @GetMapping("/github/callback")
  public String callback(@RequestParam String code, @RequestParam String state, HttpSession session) {
    String github_state = (String) session.getAttribute("github_state");
    Assert.isTrue(state.equals(github_state), "非法请求");

    String clientId = (String) systemConfigService.selectAllConfig().get("oauth_github_client_id");
    String clientSecret = (String) systemConfigService.selectAllConfig().get("oauth_github_client_secret");
    String callback = (String) systemConfigService.selectAllConfig().get("oauth_github_callback_url");

    Assert.isTrue(!StringUtils.isEmpty(clientId) && !StringUtils.isEmpty(clientSecret) && !StringUtils.isEmpty
        (callback), "Github登录还没有相关配置，联系站长吧！");

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    //    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    // 封闭获取token请求的参数
    HttpEntity entity = new HttpEntity(headers);

    // 请求获取token接口，拿token
    ResponseEntity<String> exchange = restTemplate.postForEntity("https://github" + "" + "" + "" + "" + "" +
        ".com/login/oauth/access_token?client_id=" + clientId + "&client_secret=" + clientSecret + "&code=" + code +
        "&state=" + state + "&redirect_uri=" + callback, entity, String.class);
    Map map = JsonUtil.jsonToObject(exchange.getBody(), Map.class);

    String accessToken = (String) map.get("access_token");

    // 拿到token后再次请求用户个人信息接口拿用户信息
    ResponseEntity<Map> userEntity = restTemplate.getForEntity("https://api.github.com/user?access_token=" +
        accessToken, Map.class);
    Map userEntityBody = userEntity.getBody();

    // 拿用户信息
    String login = userEntityBody.get("login").toString();
    String githubId = userEntityBody.get("id").toString();
    String avatar_url = (String) userEntityBody.get("avatar_url");
    String bio = (String) userEntityBody.get("bio");
    String email = (String) userEntityBody.get("email");
    String blog = (String) userEntityBody.get("blog");

    OAuthUser oAuthUser = oAuthUserService.selectByTypeAndLogin("GITHUB", login);
    User user;
    if (oAuthUser == null) {
      String username = login;
      if (userService.selectByUsername(login) != null) username = login + githubId;
      // 先创建user，然后再创建oauthUser
      user = userService.addUser(username, null, avatar_url, email, bio, blog, StringUtils.isEmpty(email));
      oAuthUserService.addOAuthUser(Integer.parseInt(githubId), "GITHUB", login, accessToken, bio, email, user.getId
          (), null, null, null);
    } else {
      user = userService.selectById(oAuthUser.getUserId());
      oAuthUser.setEmail(email);
      oAuthUser.setBio(bio);
      oAuthUser.setAccessToken(accessToken);
      oAuthUserService.update(oAuthUser);
    }

    // 将用户信息写session
    session.setAttribute("_user", user);
    // 将用户token写cookie
    cookieUtil.setCookie(systemConfigService.selectAllConfig().get("cookie_name").toString(), user.getToken());

    return redirect("/");
  }

  // 使用wechat联合登录
  @GetMapping("/wechat")
  public String wechat(HttpSession session) {
    String appId = (String) systemConfigService.selectAllConfig().get("oauth_wechat_client_id");
    String appSecret = (String) systemConfigService.selectAllConfig().get("oauth_wechat_client_secret");
    String callback = (String) systemConfigService.selectAllConfig().get("oauth_wechat_callback_url");

    String state = StringUtil.randomString(4).toUpperCase();
    session.setAttribute("wechat_state", state);

    Assert.isTrue(!StringUtils.isEmpty(appId) && !StringUtils.isEmpty(appSecret) && !StringUtils.isEmpty(callback),
        "微信登录还没有相关配置，联系站长吧！");

    return redirect("https://open.weixin.qq.com/connect/qrconnect?appid=" + appId + "&redirect_uri=" + callback +
        "&response_type=code&scope=snsapi_login&state=" + state);
  }

  // wechat联合登录后的回调
  @GetMapping("/wechat/callback")
  public String wechat_callback(@RequestParam String code, @RequestParam String state, HttpSession session) throws
      UnsupportedEncodingException {
    String wechat_state = (String) session.getAttribute("wechat_state");
    Assert.isTrue(state.equals(wechat_state), "非法请求");

    String appId = (String) systemConfigService.selectAllConfig().get("oauth_wechat_client_id");
    String appSecret = (String) systemConfigService.selectAllConfig().get("oauth_wechat_client_secret");
    String callback = (String) systemConfigService.selectAllConfig().get("oauth_wechat_callback_url");

    Assert.isTrue(!StringUtils.isEmpty(appId) && !StringUtils.isEmpty(appSecret) && !StringUtils.isEmpty(callback),
        "微信登录还没有相关配置，联系站长吧！");

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();

    // 请求获取token接口，拿token
    ResponseEntity<String> exchange = restTemplate.getForEntity("https://api.weixin.qq" + "" + "" + "" + "" + "" +
        ".com/sns/oauth2/access_token?appid=" + appId + "&secret=" + appSecret + "&code=" + code +
        "&grant_type=authorization_code", String.class);

    Map accessTokenMap = JsonUtil.jsonToObject(exchange.getBody(), Map.class);
    String accessToken = (String) accessTokenMap.get("access_token");
    String refreshToken = (String) accessTokenMap.get("refresh_token");
    String unionId = (String) accessTokenMap.get("unionid");
    String openId = (String) accessTokenMap.get("openid");
    String expiresIn = accessTokenMap.get("expires_in").toString();

    // 拿到token后再次请求用户个人信息接口拿用户信息
    ResponseEntity<String> userEntity = restTemplate.getForEntity("https://api.weixin.qq" + "" + "" + "" + "" + "" +
        ".com/sns/userinfo?lang=zh_CN&access_token=" + accessToken + "&openid=" + openId, String.class);
    String entityBody = userEntity.getBody();
    // 转码，微信编码用的是 ISO-8859-1, 要转成UTF-8 中文才正常
    entityBody = new String(entityBody.getBytes("ISO-8859-1"), "UTF-8");
    Map userEntityBody = JsonUtil.jsonToObject(entityBody, Map.class);

    // 拿用户信息
    String nickname = userEntityBody.get("nickname").toString();
    String avatar = userEntityBody.get("headimgurl").toString();

    OAuthUser oAuthUser = oAuthUserService.selectByTypeAndLogin("WECHAT", nickname);
    User user;
    if (oAuthUser == null) {
      String username = nickname;
      if (userService.selectByUsername(nickname) != null) username = nickname + "_" + StringUtil.randomNumber(4);
      // 先创建user，然后再创建oauthUser
      user = userService.addUser(username, null, avatar, null, null, null, false);
      oAuthUserService.addOAuthUser(null, "WECHAT", nickname, accessToken, null, null, user.getId(), refreshToken,
          unionId, openId);
    } else {
      user = userService.selectById(oAuthUser.getUserId());
      oAuthUser.setAccessToken(accessToken);
      oAuthUserService.update(oAuthUser);
    }

    // 将用户信息写session
    session.setAttribute("_user", user);
    // 将用户token写cookie
    cookieUtil.setCookie(systemConfigService.selectAllConfig().get("cookie_name").toString(), user.getToken());

    return redirect("/");
  }
}
