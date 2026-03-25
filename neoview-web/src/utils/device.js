export function getDeviceInfo() {
	return {
		name: navigator.userAgent,
		flag: 'web',
		version: navigator.appVersion,
	};
}
