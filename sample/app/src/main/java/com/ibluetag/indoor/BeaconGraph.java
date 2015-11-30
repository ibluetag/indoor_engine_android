package com.ibluetag.indoor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.text.TextPaint;
import android.text.StaticLayout;
import android.text.Layout.Alignment;
import com.ibluetag.indoor.sdk.MapProxy;

import android.util.Log;

import java.util.List;
import java.util.Map;
import com.ibluetag.loc.beacon.api.BeaconLocator;
import com.ibluetag.loc.beacon.model.MapInfo;
import com.ibluetag.sdk.model.SimpleBeacon;

public class BeaconGraph extends View {
    private static final String TAG = "BeaconLayout";
    private MapProxy mMapProxy;
    private BeaconLocator mBeaconLocator;
    private boolean mEnabled = false;

    public BeaconGraph(Context context) {
        super(context);
    }

    public BeaconGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BeaconGraph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBeaconLocator(BeaconLocator beaconLocator) {
        mBeaconLocator = beaconLocator;
    }

    public void setMapProxy(MapProxy mapProxy) {
        mMapProxy = mapProxy;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mEnabled == false) {
            return;
        }

        Map<String, PointF> beaconLayout = mBeaconLocator.getBeaconLayout();
        List<SimpleBeacon> detectedBeacons = mBeaconLocator.getDetectedBeacons();

        Paint paint = new Paint();
        paint.setColor(0x88888888);
        paint.setStrokeWidth(4.0f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLUE);
        paint.setTextSize(32.0f);


        Log.v(TAG,  "BeaconGraph onDraw..." + beaconLayout.size() + " entries on map");
        Log.v(TAG,  "BeaconGraph onDraw..." + detectedBeacons.size() + " beacons detected");

        //draw beacons on the map
        for (String id : beaconLayout.keySet()) {
            PointF pos = beaconLayout.get(id);
            PointF screenPos = mMapProxy.toScreenCoordinate(pos);
            if (screenPos != null) {
                canvas.drawPoint(screenPos.x, screenPos.y, paint);

                int minorPos = id.lastIndexOf('_') + 1;
                String minor = id.substring(minorPos);
                canvas.drawText("M"+minor, screenPos.x, screenPos.y + 10, paint);
            }
        }

        //draw detected beacons
        String beaconsStr = "detected beacons:\r\n";
        for (SimpleBeacon beacon: detectedBeacons) {
            beaconsStr += beacon.getMajor() + "_" + beacon.getMinor() + "_" + beacon.getDistanceF();
            beaconsStr += "m\r\n ";
        }
        TextPaint textPaint = new TextPaint();
        textPaint.setARGB(0xFF, 0, 0, 0);
        textPaint.setTextSize(20.0F);
        StaticLayout layout = new StaticLayout(beaconsStr, textPaint, 300,
                Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
        canvas.save();
        canvas.translate(20, 20);//从20，20开始画
        layout.draw(canvas);
        canvas.restore();//别忘了restore
    }

    public void showAll() {
        mEnabled = true;
        invalidate();
    }
}
