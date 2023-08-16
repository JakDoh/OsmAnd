package net.osmand.plus.plugins.externalsensors

import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperties

class AuthKeyDeviceSettings : DeviceSettings {
	companion object {
		const val DEFAULT_AUTH_KEY = "00000000000000000000000000000000"
	}

	constructor(
		deviceId: String, deviceType: DeviceType,
		deviceName: String, deviceEnabled: Boolean) : super(
		deviceId, deviceType,
		deviceName, deviceEnabled) {
		additionalParams[DeviceChangeableProperties.AUTH_KEY] =	DEFAULT_AUTH_KEY
	}

	constructor(settings: DeviceSettings) : super(settings) {
		if (!additionalParams.containsKey(DeviceChangeableProperties.AUTH_KEY)) {
			additionalParams[DeviceChangeableProperties.AUTH_KEY] =	DEFAULT_AUTH_KEY
		}
	}

}