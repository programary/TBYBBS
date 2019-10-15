<div class="card d-none" id="email_forget_password_div">
  <div class="card-header">找回密码</div>
  <div class="card-body">
    <form action="" onsubmit="return;">
      <div class="form-group">
        <label for="email">邮箱</label>
        <input type="email" id="email" name="email" class="form-control" placeholder="邮箱"/>
      </div>
      <div class="form-group">
        <label for="captcha">验证码</label>
        <div class="input-group">
          <input type="text" class="form-control" id="forget_password_captcha" name="captcha" placeholder="验证码"/>
          <span class="input-group-append">
                <img style="border: 1px solid #ccc;" src="" class="captcha" id="emailCaptcha"/>
              </span>
        </div>
      </div>
      <div class="form-group">
        <button type="button" id="email_forget_password" onclick="email_forget_password()" class="btn btn-info">找回密码
        </button>
        <i class="fa fa-question-circle" data-toggle="tooltip" data-placement="right" title=""
           data-original-title="手机号登录系统会判断手机号是否注册过，如果没有注册过，会创建帐号"></i>
      </div>
    </form>
      <#if !model.isEmpty(site.oauth_github_client_id!) || !model.isEmpty(site.sms_access_key_id!)>
        <hr>
      </#if>
      <#if !model.isEmpty(site.oauth_github_client_id!)>
        <a href="/oauth/github" class="btn btn-success btn-block"><i class="fa fa-github"></i>&nbsp;&nbsp;通过Github登录/注册</a>
      </#if>
      <#if !model.isEmpty(site.sms_access_key_id!)>
        <button class="btn btn-primary btn-block" id="mobile_login_btn"><i class="fa fa-mobile"></i>&nbsp;&nbsp;通过手机号登录/注册
        </button>
      </#if>
  </div>
</div>
<script>
  function email_forget_password() {
    var email = $("#email").val();
    var captcha = $("#email_captcha").val();
    if (!email) {
      err("请输入邮箱");
      return;
    }
    if (!captcha) {
      err("请输入验证码");
      return;
    }
    $.ajax({
      url: '/api/forget_password',
      type: 'post',
      cache: false,
      async: false,
      contentType: 'application/json',
      data: JSON.stringify({
        email: email,
        captcha: captcha,
      }),
      success: function (data) {
        if (data.code === 200) {
          suc("邮件发送成功");
          setTimeout(function () {
            window.location.href = "/";
          }, 700);
        } else {
          err(data.description);
        }
      }
    });
  }

  function local_login_btn() {
    $("#email_forget_password_div").addClass("hidden");
    $("#mobile_login_div").addClass("hidden");
    $("#local_login_div").removeClass("hidden");
  }
</script>
