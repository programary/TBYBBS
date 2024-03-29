<div class="card">
  <div class="card-header">
    用户Token
    <a href="javascript:;" id="refreshToken" class="pull-right">刷新Token</a>
  </div>
  <div class="card-body">
    <p>Token：<code id="userToken">${_user.token}</code></p>
    <div id="qrcode" class="text-center"></div>
  </div>
</div>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery.qrcode/1.0/jquery.qrcode.min.js"></script>
<script>
  $("#qrcode").qrcode({
    width: 180,
    height: 180,
    text: '${_user.token}'
  });

  var token = '${_user.token}';
  $("#refreshToken").on("click", function () {
    $.ajax({
      url: '/api/settings/refreshToken',
      cache: false,
      async: false,
      type: 'get',
      dataType: 'json',
      contentType: 'application/json',
      headers: {
        'token': token
      },
      success: function (data) {
        if (data.code === 200) {
          suc("刷新token成功");
          $("#qrcode").html("");
          $("#qrcode").qrcode({
            width: 180,
            height: 180,
            text: data.detail
          });
          $("#userToken").text(data.detail);
          token = data.detail;
        } else {
          err("刷新token失败");
        }
      }
    })
  });
</script>
