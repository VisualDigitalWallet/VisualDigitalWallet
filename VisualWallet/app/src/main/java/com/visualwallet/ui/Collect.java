package com.visualwallet.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONArray;
import com.google.zxing.Result;
import com.visualwallet.R;
import com.visualwallet.common.WaveBallProgress;
import com.visualwallet.cpplib.CppLib;
import com.visualwallet.data.DataUtil;
import com.visualwallet.data.ImageExporter;
import com.visualwallet.entity.Wallet;
import com.visualwallet.net.CollectRequest;
import com.visualwallet.net.DetectRequest;
import com.visualwallet.net.NetUtil;
import com.visualwallet.net.UpdateRequest;
import com.visualwallet.net.UploadRequest;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.milkyship.zxing.android.CaptureActivity;
import cn.milkyship.zxing.bean.ZxingConfig;
import cn.milkyship.zxing.common.Constant;
import cn.milkyship.zxing.decode.DecodeImgCallback;
import cn.milkyship.zxing.decode.DecodeImgThread;
import cn.milkyship.zxing.decode.ImageUtil;

public class Collect extends AppCompatActivity {

    private ImageButton local;
    private ImageButton scan;
    private ImageButton audioBtn;
    private ImageButton collectK;
    private LinearLayout collectLL;
    private WaveBallProgress waveProgress;
    private TextView progressText;

    private static final int TAKE_CAMERA = 101;
    private static final int PICK_PHOTO = 102;
    private static final int REQUEST_CODE_SCAN = 111;

    private Wallet wallet;
    private List<Integer> splitIndex;
    private List<String> splitInfo;
    private List<int[][]> splitMat;
    private String audioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect);
        Objects.requireNonNull(getSupportActionBar()).hide();//???????????????

        collectLL = findViewById(R.id.info_linear);
        local = findViewById(R.id.local);
        local.setOnClickListener(this::onPhotoClick);
        scan = findViewById(R.id.scan);
        scan.setOnClickListener(this::onScanClick);
        audioBtn = findViewById(R.id.audio);
        audioBtn.setOnClickListener(this::onAudioClick);
        if (com.visualwallet.common.Constant.appMode != 1) {
            audioBtn.setEnabled(false);
        }
        collectK = findViewById(R.id.collectK);
        collectK.setOnClickListener(this::onCollect);

        progressText = findViewById(R.id.progress_text);
        waveProgress = findViewById(R.id.wave_progress);

        Intent intent = getIntent();
        this.wallet = (Wallet) intent.getSerializableExtra(com.visualwallet.common.Constant.WALLET_ARG);

        splitIndex = new ArrayList<>();
        splitInfo = new ArrayList<>();
        splitMat = new ArrayList<>();
        audioPath = "";
    }

    public void onReturnClick(View view) {
        finish();
    }

    public void onPhotoClick(View v) {
        AndPermission.with(this)
                .permission(Permission.READ_EXTERNAL_STORAGE)
                .onGranted(permissions -> {
                    /*????????????*/
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, Constant.REQUEST_IMAGE);
                })
                .onDenied(permissions -> {
                    Uri packageURI = Uri.parse("package:" + getPackageName());
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(intent);

                    Toast.makeText(Collect.this, "???????????????????????????", Toast.LENGTH_LONG).show();
                }).start();
    }

    public void onScanClick(View v) {
        AndPermission.with(this)
                .permission(Permission.CAMERA, Permission.READ_EXTERNAL_STORAGE)
                .onGranted(permissions -> {
                    Intent intent = new Intent(Collect.this, CaptureActivity.class);
                    ZxingConfig config = new ZxingConfig();         //??????????????????
                    //config.setPlayBeep(false);                    //???????????????????????? ?????????true
                    //config.setShake(false);                       //????????????  ?????????true
                    config.setDecodeBarCode(false);                 //????????????????????? ?????????true
                    //config.setReactColor(R.color.colorAccent);    //????????????????????????????????? ???????????????
                    //config.setFrameLineColor(R.color.colorAccent);//??????????????????????????? ????????????
                    //config.setScanLineColor(R.color.colorAccent); //???????????????????????? ????????????
                    //config.setFullScreenScan(false);              //??????????????????  ?????????true ??????false??????????????????????????????
                    intent.putExtra(Constant.INTENT_ZXING_CONFIG, config);
                    startActivityForResult(intent, REQUEST_CODE_SCAN);
                })
                .onDenied(permissions -> Toast.makeText(Collect.this, "???????????????????????????", Toast.LENGTH_LONG).show()).start();
    }

    public void onAudioClick(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, com.visualwallet.common.Constant.FILE_SELECT_CODE);
    }

    public void onCollect(View vCollect) {
        Log.i("Collect submit", "id" + wallet.getId());
        Log.i("Collect submit", "splitIndex" + splitIndex.size());
        Log.i("Collect submit", "splitMat" + splitIndex.size());

        // ??????????????????????????????Collect
        int hasAudio = (audioPath.equals("") ? 0 : 1);
        new CollectRequest(wallet.getId(), splitIndex, splitMat, hasAudio).setNetCallback(res -> {
            if (res == null || !Objects.requireNonNull(res.get("code")).equals("200")) {
                String logInfo = "??????????????????";
                Log.e("Collect", "Net response illegal");
                if (res != null && res.get("code") != null) {
                    logInfo += " " + res.get("code");
                    Log.e("Collect", "http code " + res.get("code"));
                }
                Looper.prepare();
                Toast.makeText(Collect.this, logInfo, Toast.LENGTH_LONG).show();
                Looper.loop();
                return;
            }
            String flag = Objects.requireNonNull(res.get("flag")).toString();
            switch (flag) {
                case "1":
                    String secretKey = Objects.requireNonNull(res.get("secretKey")).toString();
                    secretKey = NetUtil.key2hex(secretKey);

                    Looper.prepare();
                    // ?????????????????????
                    picUpdate(wallet.getId(), Objects.requireNonNull(res.get("secretKey")).toString());

                    // ?????????????????????
                    AlertDialog.Builder adBuilder = new AlertDialog.Builder(Collect.this);
                    View adView = View.inflate(Collect.this, R.layout.alert_dialog, null);
                    adBuilder.setView(adView);
                    adBuilder.setCancelable(true);
                    TextView msg= adView.findViewById(R.id.msg);
                    ImageButton cfBtn = adView.findViewById(R.id.btn_comfirm);
                    cfBtn.setOnClickListener(v -> finish());
                    msg.setText(String.format("??????????????????\n0x%s", secretKey));
                    ImageButton copyBtn = adView.findViewById(R.id.btn_copy);
                    String finalSecretKey = secretKey;
                    copyBtn.setOnClickListener(v -> {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData mClipData = ClipData.newPlainText("??????", finalSecretKey);
                        cm.setPrimaryClip(mClipData);
                        Toast.makeText(Collect.this, "????????????", Toast.LENGTH_LONG).show();
                    });
                    adBuilder.create().show();
                    Looper.loop();

                    return;
                case "0":
                    Log.e("Collect", "Net flag 0, pics were modified");
                    Looper.prepare();
                    Toast.makeText(Collect.this, "?????????????????????????????????????????????", Toast.LENGTH_LONG).show();
                    Looper.loop();
                    break;
                case "-1":
                    Log.e("Collect", "Net flag -1, pics were not qrcode");
                    Looper.prepare();
                    Toast.makeText(Collect.this, "?????????????????????", Toast.LENGTH_LONG).show();
                    Looper.loop();
                    break;
                default:
                    Log.e("Collect", "Net flag illegal");
                    Looper.prepare();
                    Toast.makeText(Collect.this, "?????????????????????", Toast.LENGTH_LONG).show();
                    Looper.loop();
                    break;
            }

            // ????????????????????????????????????????????????
            Collect.this.runOnUiThread(() -> {
                findViewById(R.id.progress_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.progress_num_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.fail_alert).setVisibility(View.VISIBLE);
            });
        }).start();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i("collect activity result", String.valueOf(requestCode));

        switch (requestCode) {
            case Constant.REQUEST_IMAGE:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    String path = ImageUtil.getImageAbsolutePath(this, data.getData());
                    Log.i("img path", path);
                    new DecodeImgThread(path, new DecodeImgCallback() {
                        @Override
                        public void onImageDecodeSuccess(Result result) {
                            String decodeinfo = result.getText();
                            int[][] pointMatrix = result.getPointMatrix().getArray();
                            imgDetect(decodeinfo, pointMatrix);
                        }

                        @Override
                        public void onImageDecodeFailed() {
                            Looper.prepare();
                            Toast.makeText(Collect.this, cn.milkyship.zxing.R.string.scan_failed_tip, Toast.LENGTH_SHORT).show();
                            Looper.loop();
                        }
                    }).start();
                }
                break;
            case REQUEST_CODE_SCAN:
                if (resultCode == RESULT_OK && data != null) {
                    String content = data.getStringExtra(Constant.CODED_CONTENT);
                    int[][] pointMatrix = (int[][]) data.getSerializableExtra(Constant.CODED_POINT_MATRIX);

                    imgDetect(content, pointMatrix);
                }
                break;
            case com.visualwallet.common.Constant.FILE_SELECT_CODE:
                // ??????????????????
                if (resultCode != Activity.RESULT_OK) {
                    Log.e("AddNewTag", "onActivityResult() error, resultCode: " + resultCode);
                } else if (data != null) {
                    Uri uri = data.getData();
                    String audioPath = DataUtil.resolveAudioUri(Collect.this, uri);
                    Log.i("AddNewTag", "file path: " + audioPath);
                    if (audioPath != null) {
                        File audioFile = new File(audioPath);
                        audioDetect(audioFile);
                    }
                }
                break;
            default:
                break;
        }
//        Log.i("Collect", String.format("splitInfo size %d, K = %d", splitInfo.size(), wallet.getCoeK()));
//        if (splitInfo.size() == wallet.getCoeK()) {
//            collectK.setVisibility(View.VISIBLE);
//        }
    }

    @SuppressLint("SetTextI18n")
    private void addinfo(String dinfo) {
        Collect.this.runOnUiThread(() -> {
            TextView newText = new TextView(Collect.this);
            newText.setText(dinfo.substring(0, dinfo.length() - 9).replace('\n', ' '));
            newText.setTextColor(Collect.this.getResources().getColor(R.color.white));
            newText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            newText.setGravity(Gravity.CENTER_HORIZONTAL);
            newText.setBackgroundResource(R.drawable.collect_tag);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(40, 8, 40, 8);
            newText.setLayoutParams(lp);
            collectLL.addView(newText);
        });
    }

    private void imgDetect(String content, int[][] pointMatrix) {
        String indexStr = content.substring(content.length() - 2, content.length() - 1);

        Log.i("Collect scan result", content);
        Log.i("Collect indexStr", indexStr);

        int picIndex = Integer.parseInt(indexStr);

        Log.i("point matrix size", String.format("%dx%d", pointMatrix.length, pointMatrix[0].length));
        //Log.i("point matrix", " \n" + pointMatrix.replace("1", "#").replace("0", " "));

        // ???????????????????????????
        for (int i : splitIndex) {
            if (i == picIndex) {
                Looper.prepare();
                Toast.makeText(Collect.this, "???????????????", Toast.LENGTH_LONG).show();
                Looper.loop();
                return;
            }
        }

        if (com.visualwallet.common.Constant.appMode == 0) {
            // ????????????
            int[][] s0 = DataUtil.getS0(wallet.getCoeK(), wallet.getCoeN());
            int[][] s1 = DataUtil.getS1(wallet.getCoeK(), wallet.getCoeN());

            long beginTime = System.nanoTime();

            // c++??????????????????
            boolean suc = CppLib.detect(
                    Start.androidId.getBytes(),
                    wallet.getCoeN(),
                    picIndex,
                    s0,
                    s1,
                    s0.length,
                    s0[0].length,
                    pointMatrix,
                    pointMatrix.length
            );

            long endTime = System.nanoTime();
            long costTime = (endTime - beginTime);
            Log.i("Timer", String.format("Detect function cpp call, cost %d ns", costTime));

            if (suc) {
                addinfo(content);

                splitIndex.add(picIndex);
                splitInfo.add(content);
                splitMat.add(pointMatrix);

                Collect.this.runOnUiThread(() -> {
                    findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
                    findViewById(R.id.progress_num_layout).setVisibility(View.VISIBLE);
                    findViewById(R.id.fail_alert).setVisibility(View.INVISIBLE);
                    int progressNum = (int) (splitInfo.size() / (float) wallet.getCoeK() * 100);
                    String progressStr = progressNum + "%";
                    progressText.setText(progressStr);
                    waveProgress.startProgress(progressNum, 300, 0);
                });

                if (splitInfo.size() == wallet.getCoeK()) {
                    Collect.this.runOnUiThread(() -> collectK.setVisibility(View.VISIBLE));
                }
            } else {
                Looper.prepare();
                Toast.makeText(Collect.this, "???????????????", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        } else if (com.visualwallet.common.Constant.appMode == 1) {
            // ????????????
            new DetectRequest(wallet.getId(), picIndex, pointMatrix).setNetCallback(res -> {
                if (res == null) {
                    Looper.prepare();
                    Toast.makeText(Collect.this, "??????????????????????????????", Toast.LENGTH_LONG).show();
                    Looper.loop();
                    return;
                }
                Integer DetectRes = (Integer) res.get("flag");
                if (DetectRes == null) {
                    Looper.prepare();
                    Toast.makeText(Collect.this, "?????????????????????????????????", Toast.LENGTH_LONG).show();
                    Looper.loop();
                    return;
                }

                if (DetectRes == 1) {
                    addinfo(content);

                    splitIndex.add(picIndex);
                    splitInfo.add(content);
                    splitMat.add(pointMatrix);

                    Collect.this.runOnUiThread(() -> {
                        findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
                        findViewById(R.id.progress_num_layout).setVisibility(View.VISIBLE);
                        findViewById(R.id.fail_alert).setVisibility(View.INVISIBLE);
                        int progress = (int) (splitInfo.size() / (float) wallet.getCoeK() * 100);
                        String progressStr = progress + "%";
                        progressText.setText(progressStr);
                        waveProgress.startProgress(progress, 300, 0);
                    });

                    if (splitInfo.size() == wallet.getCoeK()) {
                        Collect.this.runOnUiThread(() -> collectK.setVisibility(View.VISIBLE));
                    }
                } else {
                    Collect.this.runOnUiThread(() -> {
                        findViewById(R.id.progress_layout).setVisibility(View.INVISIBLE);
                        findViewById(R.id.progress_num_layout).setVisibility(View.INVISIBLE);
                        findViewById(R.id.fail_alert).setVisibility(View.VISIBLE);
                    });
                    Looper.prepare();
                    Toast.makeText(Collect.this, "???????????????", Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }).start();
        }
    }

    private void picUpdate(int id, String secretKey) {
        new UpdateRequest(id, secretKey).setNetCallback(res -> {
            if (res == null) {
                Looper.prepare();
                Toast.makeText(Collect.this, "????????????????????????????????????", Toast.LENGTH_LONG).show();
                Looper.loop();
                return;
            }
            Integer updateRes = (Integer) res.get("flag");
            int[][][] updated = NetUtil.arrayJson2java((JSONArray) res.get("updated"));
            if (updateRes == null || updated == null) {
                Looper.prepare();
                Toast.makeText(Collect.this, "????????????????????????????????????", Toast.LENGTH_LONG).show();
                Looper.loop();
                return;
            }

            // ??????????????????
            ImageExporter.export(Collect.this, wallet.getWalName(), updated);

            // ????????????????????????????????????
            if (updated.length > 0) {
                String resText = "???????????????????????????" + updated.length + "??????????????????????????????????????????";
                Looper.prepare();
                Toast.makeText(Collect.this, resText, Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }).start();
    }

    private void audioDetect(File audioFile) {
        // ??????????????????????????????
        UploadRequest uploadRequest = new UploadRequest(wallet.getId(), 1, ".wav", audioFile);
        uploadRequest.setNetCallback(resUpload -> {
            String logInfo = "??????????????????";
            if (resUpload == null || !Objects.requireNonNull(resUpload.get("code")).equals("200")) {
                Log.e("AddNewTag", "Net response illegal");
                if (resUpload != null && resUpload.get("code") != null) {
                    logInfo += " " + resUpload.get("code");
                    Log.e("AddNewTag", "http code " + resUpload.get("code"));
                }
                Looper.prepare();
                Toast.makeText(Collect.this, logInfo, Toast.LENGTH_LONG).show();
                Looper.loop();
            }
            // ??????????????????????????????????????????
            new DetectRequest(wallet.getId(), 0).setNetCallback(resDetect -> {
                if (resDetect == null || resDetect.get("flag") == null) {
                    Looper.prepare();
                    Toast.makeText(Collect.this, "?????????????????????????????????", Toast.LENGTH_LONG).show();
                    Looper.loop();
                    return;
                }

                if (Integer.parseInt(resDetect.get("flag").toString()) == 1) {
                    String content = "?????????" + audioFile.getName() + "\t\tType: " + wallet.getCurType();
                    addinfo(content);
                    splitInfo.add(content);
                    audioPath = audioFile.getName();

                    Collect.this.runOnUiThread(() -> {
                        findViewById(R.id.progress_layout).setVisibility(View.VISIBLE);
                        findViewById(R.id.progress_num_layout).setVisibility(View.VISIBLE);
                        findViewById(R.id.fail_alert).setVisibility(View.INVISIBLE);
                        int prog = (int) (splitInfo.size() / (float) wallet.getCoeK() * 100);
                        progressText.setText(String.valueOf(prog + "%"));
                        waveProgress.startProgress(prog, 300, 0);
                    });

                    if (splitInfo.size() == wallet.getCoeK()) {
                        Collect.this.runOnUiThread(() -> collectK.setVisibility(View.VISIBLE));
                    }
                } else {
                    Collect.this.runOnUiThread(() -> {
                        findViewById(R.id.progress_layout).setVisibility(View.INVISIBLE);
                        findViewById(R.id.progress_num_layout).setVisibility(View.INVISIBLE);
                        findViewById(R.id.fail_alert).setVisibility(View.VISIBLE);
                    });
                    Looper.prepare();
                    Toast.makeText(Collect.this, "???????????????", Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }).start();
        }).start();
    }
}
