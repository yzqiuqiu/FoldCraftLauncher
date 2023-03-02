package com.tungsten.fcl.activity;

import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mio.mclauncher.customcontrol.MioCrossingKeyboard;
import com.mio.mclauncher.customcontrol.MioCustomManager;
import com.tungsten.fcl.R;
import com.tungsten.fcl.control.MenuCallback;
import com.tungsten.fcl.control.MenuType;
import com.tungsten.fcl.control.GameMenu;
import com.tungsten.fcl.control.JavaGuiMenu;
import com.tungsten.fcl.miopatch.AndroidKeyMap;
import com.tungsten.fcl.setting.GameOption;
import com.tungsten.fclauncher.FCLKeycodes;
import com.tungsten.fclauncher.bridge.FCLBridge;
import com.tungsten.fclcore.util.Logging;
import com.tungsten.fcllibrary.component.FCLActivity;

import java.util.logging.Level;

public class JVMActivity extends FCLActivity implements TextureView.SurfaceTextureListener {

    private TextureView textureView;

    private MenuCallback menu;
    private static MenuType menuType;
    private static FCLBridge fclBridge;
    private AndroidKeyMap androidKeyMap=new AndroidKeyMap();

    private int baseX;
    private int baseY;

    private int baseXC;
    private int baseYC;

    public static void setFClBridge(FCLBridge fclBridge, MenuType menuType) {
        JVMActivity.fclBridge = fclBridge;
        JVMActivity.menuType = menuType;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jvm);

        if (menuType == null || fclBridge == null) {
            Logging.LOG.log(Level.WARNING, "Failed to get ControllerType or FCLBridge, task canceled.");
            return;
        }

        menu = menuType == MenuType.GAME ? new GameMenu() : new JavaGuiMenu();
        menu.setup(this, fclBridge);
        textureView = findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);
        textureView.setFocusable(true);
        addContentView(menu.getLayout(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
        Logging.LOG.log(Level.INFO, "surface ready, start jvm now!");
        GameOption gameOption=new GameOption(menu.getBridge().getGameDir());
        gameOption.set("fullscreen","false");
        gameOption.set("overrideWidth",""+i);
        gameOption.set("overrideHeight",""+i1);
        gameOption.save();
       
        surfaceTexture.setDefaultBufferSize((int) (i * fclBridge.getScaleFactor()), (int) (i1 * fclBridge.getScaleFactor()));
        fclBridge.execute(new Surface(surfaceTexture), menu.getCallbackBridge());
        fclBridge.pushEventWindow((int) (i * fclBridge.getScaleFactor()), (int) (i1 * fclBridge.getScaleFactor()));
        textureView.postDelayed(new Runnable() {
            @Override
            public void run() {
                useMouse();
                textureView.requestFocus();
                textureView.requestPointerCapture();
            }
        },3000);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
        surfaceTexture.setDefaultBufferSize((int) (i * fclBridge.getScaleFactor()), (int) (i1 * fclBridge.getScaleFactor()));
        fclBridge.pushEventWindow((int) (i * fclBridge.getScaleFactor()), (int) (i1 * fclBridge.getScaleFactor()));
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        return false;
    }

    private int output = 0;

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
        if (textureView != null && textureView.getSurfaceTexture() != null) {
            textureView.post(() -> onSurfaceTextureSizeChanged(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight()));
        }
        if (output == 1) {
            menu.onGraphicOutput();
            output++;
        }
        if (output < 1) {
            output++;
        }
    }

    @Override
    protected void onPause() {
        menu.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        menu.onResume();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        menu.onBackPressed();
        textureView.requestFocus();
        textureView.requestPointerCapture();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (textureView != null && textureView.getSurfaceTexture() != null) {
            textureView.post(() -> onSurfaceTextureSizeChanged(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight()));
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (textureView != null && textureView.getSurfaceTexture() != null) {
            textureView.post(() -> onSurfaceTextureSizeChanged(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight()));
        }
    }

    public void useMouse() {
        textureView.setOnCapturedPointerListener(new View.OnCapturedPointerListener() {
            @Override
            public boolean onCapturedPointer(View view, MotionEvent event) {
                if (menu.getCursorMode()==FCLBridge.CursorEnabled){
                    baseXC+=(int) event.getX()*1.2;
                    baseYC+=(int) event.getY()*1.2;
                    menu.getInput().setPointer(baseXC,baseYC);
                } else {
                    baseX+=(int) event.getX()*1.2;
                    baseY+=(int) event.getY()*1.2;
                    menu.getInput().setPointer(baseX,baseY);
                }

                if (event.getAction() == MotionEvent.ACTION_BUTTON_PRESS) {
                    if (event.getActionButton() == MotionEvent.BUTTON_PRIMARY) {
                        fclBridge.pushEventMouseButton(FCLBridge.Button1, true);
                    }
                    if (event.getActionButton() == MotionEvent.BUTTON_SECONDARY || event.getActionButton() == MotionEvent.BUTTON_BACK) {
                        fclBridge.pushEventMouseButton(FCLBridge.Button3, true);
                    }
                    if (event.getActionButton() == MotionEvent.BUTTON_TERTIARY) {
                        fclBridge.pushEventMouseButton(FCLBridge.Button2, true);
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_BUTTON_RELEASE) {
                    if (event.getActionButton() == MotionEvent.BUTTON_PRIMARY) {
                        fclBridge.pushEventMouseButton(FCLBridge.Button1, false);
                    }
                    if (event.getActionButton() == MotionEvent.BUTTON_SECONDARY || event.getActionButton() == MotionEvent.BUTTON_BACK) {
                        fclBridge.pushEventMouseButton(FCLBridge.Button3, false);
                    }
                    if (event.getActionButton() == MotionEvent.BUTTON_TERTIARY) {
                        fclBridge.pushEventMouseButton(FCLBridge.Button2, false);
                    }
                }
                if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        fclBridge.pushEventMouseButton(FCLBridge.Button5, true);
                        fclBridge.pushEventMouseButton(FCLBridge.Button5, false);
                    } else {
                        fclBridge.pushEventMouseButton(FCLBridge.Button4, true);
                        fclBridge.pushEventMouseButton(FCLBridge.Button4, false);
                    }
                }
                return false;
            }
        });

        textureView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent event) {
                if (event.getAction()==KeyEvent.ACTION_DOWN){
                    fclBridge.pushEventKey(androidKeyMap.translate(event.getKeyCode()),0,true);
                } else if (event.getAction()==KeyEvent.ACTION_UP){
                    fclBridge.pushEventKey(androidKeyMap.translate(event.getKeyCode()),0,false);
                }
                return true;
            }
        });
    }

}
