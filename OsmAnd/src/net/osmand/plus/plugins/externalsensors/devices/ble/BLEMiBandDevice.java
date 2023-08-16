package net.osmand.plus.plugins.externalsensors.devices.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperties;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEBatterySensorMiB;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEBikeSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEHeartRateSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEMiBandAuth;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLEMiBandDevice extends BLEAbstractDevice {

	public BLEMiBandDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(bluetoothAdapter, deviceId);
		auth = (new BLEMiBandAuth(this));
		sensors.add(new BLEHeartRateSensor(this));
		sensors.add(new BLEBatterySensorMiB(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.BLE_MI_BAND;
	}

	@NonNull
	public static UUID getServiceUUID() {
		return GattAttributes.UUID_SERVICE_MIBAND_AUTH;
	}

	@NonNull
	@Override
	public List<DeviceChangeableProperties> getChangeableProperties() {
		return Collections.singletonList(DeviceChangeableProperties.AUTH_KEY);
	}

	private void setAuthKey(String authKey) {
		auth.setAuthKey(authKey);
	}

	@Override
	public void setChangeableProperty(DeviceChangeableProperties property, String value) {
		if (property == DeviceChangeableProperties.AUTH_KEY && !Algorithms.isEmpty(value)) {
			setAuthKey(value);
		} else {
			super.setChangeableProperty(property, value);
		}
	}

}