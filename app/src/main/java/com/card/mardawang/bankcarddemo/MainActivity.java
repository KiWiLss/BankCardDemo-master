package com.card.mardawang.bankcarddemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.unionpay.UPPayAssistEx;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

//根据银行卡号获取银行及银行卡类型
public class MainActivity extends AppCompatActivity implements Handler.Callback{
    public static final String TAG = "MMM";
    private String extra= "知识扩展"+"<br/>"+"<font color='#333333'><big>Luhn</big></font> <font color='#999999'><small>检验数字算法（Luhn Check Digit Algorithm），也叫做模数10公式，是一种简单的算法，用于" +
            "验证银行卡、信用卡号码的有效性的算法。对所有大型信用卡公司发行的信用卡都起作用，这些公司包括美国Express、护照、" +
            "万事达卡、Discover和用餐者俱乐部等。这种算法最初是在20世纪60年代由一组数学家制定，现在Luhn检验数字算法属于大众，" +
            "任何人都可以使用它。<br/>" + "<br/>" +
            "算法：将每个奇数加倍和使它变为单个的数字，如果必要的话通过减去9和在每个偶数上加上这些值。如果此卡要有效，那么，结果必须是10的倍数。</small></font>";

    private EditText et_cardnum;
    private TextView tv_bankname;
    private TextView tv_cardtype;
    private TextView tv_extra;
    private String cardnum;
    private BankInfoBean bankinfobean;
    private Button btn_get;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        et_cardnum = (EditText) findViewById(R.id.et_cardnum);
        tv_bankname = (TextView) findViewById(R.id.tv_bankname);
        tv_cardtype = (TextView) findViewById(R.id.tv_cardtype);
        tv_extra = (TextView) findViewById(R.id.tv_extra);
        btn_get = (Button) findViewById(R.id.tv_get);

        tv_extra.setText(Html.fromHtml(extra));

        btn_get.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardnum = et_cardnum.getText().toString().trim();
                if (cardnum!=null && checkBankCard(cardnum)) {
                    bankinfobean = new BankInfoBean(cardnum);
                    tv_bankname.setText(bankinfobean.getBankName());
                    tv_cardtype.setText(bankinfobean.getCardType());
                } else {
                    Toast.makeText(MainActivity.this, "卡号 "+cardnum+" 不合法,请重新输入", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    /**校验过程：
        1、从卡号最后一位数字开始，逆向将奇数位(1、3、5等等)相加。
        2、从卡号最后一位数字开始，逆向将偶数位数字，先乘以2（如果乘积为两位数，将个位十位数字相加，即将其减去9），再求和。
        3、将奇数位总和加上偶数位总和，结果应该可以被10整除。
     * 校验银行卡卡号
     */
    public static boolean checkBankCard(String bankCard) {
        if(bankCard.length() < 15 || bankCard.length() > 19) {
            return false;
        }
        char bit = getBankCardCheckCode(bankCard.substring(0, bankCard.length() - 1));
        if(bit == 'N'){
            return false;
        }
        return bankCard.charAt(bankCard.length() - 1) == bit;
    }

    /**
     * 从不含校验位的银行卡卡号采用 Luhn 校验算法获得校验位
     * @param nonCheckCodeBankCard
     * @return
     */
    public static char getBankCardCheckCode(String nonCheckCodeBankCard){
        if(nonCheckCodeBankCard == null || nonCheckCodeBankCard.trim().length() == 0
                || !nonCheckCodeBankCard.matches("\\d+")) {
            //如果传的不是数据返回N
            return 'N';
        }
        char[] chs = nonCheckCodeBankCard.trim().toCharArray();
        int luhmSum = 0;
        for(int i = chs.length - 1, j = 0; i >= 0; i--, j++) {
            int k = chs[i] - '0';
            if(j % 2 == 0) {
                k *= 2;
                k = k / 10 + k % 10;
            }
            luhmSum += k;
        }
        return (luhmSum % 10 == 0) ? '0' : (char)((10 - luhmSum % 10) + '0');
    }
    /*****************************************************************
     * mMode参数解释： "00" - 启动银联正式环境 "01" - 连接银联测试环境
     *****************************************************************/
    private final String mMode = "01";
    private static final String TN_URL_01 = "http://101.231.204.84:8091/sim/getacptn";
    public void pay(View view) {
        new Thread(mRunnable).start();

    }

    Runnable mRunnable=new Runnable() {
        @Override
        public void run() {

            String tn = null;
            InputStream is;
            try {

                String url = TN_URL_01;

                URL myURL = new URL(url);
                URLConnection ucon = myURL.openConnection();
                ucon.setConnectTimeout(120000);
                is = ucon.getInputStream();
                int i = -1;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while ((i = is.read()) != -1) {
                    baos.write(i);
                }

                tn = baos.toString();
                is.close();
                baos.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }

            Message msg = mHandler.obtainMessage();
            msg.obj = tn;
            mHandler.sendMessage(msg);
        }
    };

    Handler mHandler=new Handler(this);
    @Override
    public boolean handleMessage(Message msg) {
        Object obj = msg.obj;
        Log.e(TAG, "handleMessage: "+obj);
        if (msg.obj == null || ((String) msg.obj).length() == 0) {
            Log.e(TAG, "handleMessage: fail");
        }else {
            /*************************************************
             * 步骤2：通过银联工具类启动支付插件
             ************************************************/
            UPPayAssistEx.startPay(this, null, null, obj.toString(), mMode);
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /*************************************************
         * 步骤3：处理银联手机支付控件返回的支付结果
         ************************************************/
        if (data == null) {
            return;
        }

        String msg = "";
        /*
         * 支付控件返回字符串:success、fail、cancel 分别代表支付成功，支付失败，支付取消
         */
        String str = data.getExtras().getString("pay_result");
        if (str.equalsIgnoreCase("success")) {

            // 如果想对结果数据验签，可使用下面这段代码，但建议不验签，直接去商户后台查询交易结果
            // result_data结构见c）result_data参数说明
            if (data.hasExtra("result_data")) {
                String result = data.getExtras().getString("result_data");
              /*  try {
                    JSONObject resultJson = new JSONObject(result);
                    String sign = resultJson.getString("sign");
                    String dataOrg = resultJson.getString("data");
                    // 此处的verify建议送去商户后台做验签
                    // 如要放在手机端验，则代码必须支持更新证书
                    boolean ret = verify(dataOrg, sign, mMode);
                    if (ret) {
                        // 验签成功，显示支付结果
                        msg = "支付成功！";
                    } else {
                        // 验签失败
                        msg = "支付失败！";
                    }
                } catch (JSONException e) {
                }
            }*/
                // 结果result_data为成功时，去商户后台查询一下再展示成功
                msg = "支付成功！";
            } else if (str.equalsIgnoreCase("fail")) {
                msg = "支付失败！";
            } else if (str.equalsIgnoreCase("cancel")) {
                msg = "用户取消了支付";
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("支付结果通知");
            builder.setMessage(msg);
            builder.setInverseBackgroundForced(true);
            // builder.setCustomTitle();
            builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }

    }
}
