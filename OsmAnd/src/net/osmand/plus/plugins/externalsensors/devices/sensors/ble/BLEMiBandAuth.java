package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import static net.osmand.gpx.GPXUtilities.DECIMAL_FORMAT;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class BLEMiBandAuth extends BLEAbstractSensor {

	private static final Log LOG = PlatformUtil.getLog(BLEMiBandAuth.class);
	private String authKey;

	public enum AuthStatus {
		READY4AUTH(R.string.shared_string_other),
		REQUEST_RN_OK(R.string.shared_string_chest),
		RN_RECEIVED(R.string.shared_string_wrist),
		AUTH_OK(R.string.shared_string_finger),
		KEY_SENDING_FAILED(R.string.shared_string_hand),
		REQUEST_RN_ERROR(R.string.shared_string_ear_lobe),
		ENCRYPTION_KEY_FAILED(R.string.shared_string_foot);

		@StringRes
		private final int stringRes;

		AuthStatus(int stringRes) {
			this.stringRes = stringRes;
		}

		public int getStringRes() {
			return stringRes;
		}

		public String getString(@NonNull Context context) {
			return context.getString(stringRes);
		}
	}

	public BLEMiBandAuth(@NonNull BLEAbstractDevice device) {
		super(device, device.getDeviceId() + "_auth");

	}

	public BLEMiBandAuth(@NonNull BLEAbstractDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return GattAttributes.UUID_CHARACTERISTIC_MIBAND_AUTH;
	}

	@NonNull
	@Override
	public String getName() {
		return "Authentication layer";
	}


	@SuppressLint("MissingPermission")
	@Override
	public void onCharacteristicRead(@NonNull BluetoothGatt gatt,
									 @NonNull BluetoothGattCharacteristic characteristic,
									 int status) {
		if (status == BluetoothGatt.GATT_SUCCESS) {
			if (getRequestedCharacteristicUUID().equals(characteristic.getUuid())) {
				decodeAuthCharacteristic(characteristic);
			}
		}
	}

	@Override
	public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
										@NonNull BluetoothGattCharacteristic characteristic) {
		if (getRequestedCharacteristicUUID().equals(characteristic.getUuid())) {
			decodeAuthCharacteristic(characteristic);
		}
	}

	@SuppressLint("MissingPermission")
	private void decodeAuthCharacteristic(@NonNull BluetoothGattCharacteristic characteristic) {

		final byte[] FIRST_NOTIF = {1, 0, 0};
		final byte[] REQUEST_RN = {16, 1, 1};
		final byte[] REQ_RN_RECEIVED = {16, 2, 1};
		final byte[] AUTH_OK = {16, 3, 1};
		final byte[] KEY_SENDING_FAILED = {16, 1, 4};
		final byte[] REQUEST_RN_ERROR = {16, 2, 4};
		final byte[] ENCRYPTION_KEY_FAILED = {16, 3, 4};

		try {

			byte[] valueChara = characteristic.getValue();
			byte[] cmd = Arrays.copyOfRange(valueChara, 0, 3);

			if (Arrays.equals(cmd, FIRST_NOTIF)) {
				byte[] rnr_cmd = new byte[]{0x02, 0x00};
				characteristic.setValue(rnr_cmd);
				getBLEDevice().writeCharacteristic(characteristic);
			} else if (Arrays.equals(cmd, REQUEST_RN)) {
				LOG.debug("Request for random number was received");
			} else if (Arrays.equals(cmd, REQ_RN_RECEIVED)) {
				LOG.debug("Random number for authentication received");
				byte[] challenge = Arrays.copyOfRange(valueChara, 3, 19);
				Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
				//ExternalSensorsPlugin plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
				String authKey = this.authKey != null ? this.authKey : "null" ;
				// double check
				byte[] AuthKey_byteArr = Hex.decodeHex(
						(Pattern.matches("[0-9A-Fa-f]{32}$", authKey) ? authKey : "00000000000000000000000000000000").toCharArray());
				SecretKeySpec key = new SecretKeySpec(AuthKey_byteArr, "AES");
				cipher.init(Cipher.ENCRYPT_MODE, key);
				byte[] response = cipher.doFinal(challenge);
				byte[] keysend_cmd = new byte[]{0x03, 0x00};
				byte[] resp = new byte[keysend_cmd.length + response.length];
				System.arraycopy(keysend_cmd, 0, resp, 0, keysend_cmd.length);
				System.arraycopy(response, 0, resp, keysend_cmd.length, response.length);

				characteristic.setValue(resp);
				getBLEDevice().writeCharacteristic(characteristic);

				LOG.debug("Sending authentication response");
			} else if (Arrays.equals(cmd, AUTH_OK)) {
				LOG.debug("Authentication successfully complete");
				getBLEDevice().setAuthDone();
				getBLEDevice().fireSensors();
			} else if (Arrays.equals(cmd, KEY_SENDING_FAILED)) {
				LOG.debug("Authentication key sending failed");
			} else if (Arrays.equals(cmd, REQUEST_RN_ERROR)) {
				LOG.debug("Requested random number failed");
			} else if (Arrays.equals(cmd, ENCRYPTION_KEY_FAILED)) {
				LOG.debug("Encrypted response failed");
			}

			//getBLEDevice().fireSensorDataEvent(this, data);
		//TODO
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (NoSuchPaddingException e) {
			throw new RuntimeException(e);
		} catch (DecoderException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		} catch (BadPaddingException e) {
			throw new RuntimeException(e);
		} catch (IllegalBlockSizeException e) {
			throw new RuntimeException(e);
		}
	}

	public void setAuthKey(String value) {
		this.authKey = value;
	}

	// fuck the system
	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {

		return Collections.emptyList();
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {

	}


}



