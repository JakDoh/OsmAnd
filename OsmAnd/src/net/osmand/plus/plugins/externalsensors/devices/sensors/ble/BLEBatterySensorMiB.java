package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice;

import java.util.UUID;

public class BLEBatterySensorMiB extends BLEBatterySensor {


	public BLEBatterySensorMiB(@NonNull BLEAbstractDevice device) {
		super(device);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return UUID.fromString("00000006-0000-3512-2118-0009af100700");
	}

	@Override
	public void decodeBatteryCharacteristic(@NonNull BluetoothGatt gatt,
											@NonNull BluetoothGattCharacteristic characteristic) {
		int batteryLevel = characteristic.getValue()[1];
		BatteryData data = new BatteryData(System.currentTimeMillis(), batteryLevel);
		this.lastBatteryData = data;
		getBLEDevice().fireSensorDataEvent(this, data);
	}


}